package de.fatox.meta.assets.xpk

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.CRC32C
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The XPK v2 container layout, and the primitives the writer and reader must agree on.
 *
 * Both sides derive everything from this file so neither can drift from the other. See `docs/xpk-format-audit.md`
 * §7 for the layout and §8-§9 for why each choice is what it is.
 *
 * ```
 * offset 0        salt[16]                 the only plaintext; deterministic, not random per build
 * offset 16       payload pages            1 MB, aligned; 64 KB blocks, never straddling a page
 *                 block table              per block: file offset, stored size, raw size, codec, nonce
 *                 table of contents        per entry: nameHash, contentKey, block, offset, sizes
 * offset len-96   footer                   obfuscated, then Ed25519 signature over the TOC
 * ```
 */
internal object XpkFormat {
	/** Bumped only for a layout change a previous reader could not parse. */
	const val VERSION: Int = 2

	const val SALT_LENGTH: Int = 16
	const val NONCE_LENGTH: Int = 16
	const val SIGNATURE_LENGTH: Int = 64

	/**
	 * Compression block, 64 KB.
	 *
	 * Measured on this repository's assets: 64 KB keeps 98.6% of the ratio a single solid stream reaches, at 0.221 ms
	 * to decode one block. 4 KB - MPQ's sector size - gives up 10% of the ratio, and past 256 KB the ratio is
	 * exhausted while decode latency keeps climbing. 64 KB rather than 128 KB because of [PAGE_SIZE].
	 */
	const val BLOCK_SIZE: Int = 64 * 1024

	/**
	 * Page, 1 MB, matching SteamPipe's chunking unit.
	 *
	 * Blocks are packed into pages and never straddle one, so a block whose compressed size changes cannot shift
	 * anything outside its own page. Without that, one changed asset cascades into every later 1 MB chunk and Steam
	 * re-downloads most of the archive. Tail padding costs about half a block per page - ~3% at 64 KB blocks, which
	 * is why the block is 64 KB and not 128 KB.
	 */
	const val PAGE_SIZE: Int = 1024 * 1024

	/**
	 * Block starts are aligned to this, so a re-compression landing in the same bucket shifts nothing at all.
	 *
	 * 4 KB costs about 2 KB per 64 KB block on average - roughly 3% - and is a page size on every target platform.
	 */
	const val BLOCK_ALIGNMENT: Int = 4 * 1024

	/** Compression is skipped when it saves less than this, because decode time then buys nothing. */
	const val STORE_RAW_RATIO: Double = 0.97

	const val CODEC_STORE: Byte = 0
	const val CODEC_DEFLATE: Byte = 1

	/** footer: tocOffset u64, tocLength u32, blockCount u32, entryCount u32, version u16, profileId u16, checksum u64 */
	const val FOOTER_FIELDS_LENGTH: Int = 8 + 4 + 4 + 4 + 2 + 2 + 8
	const val FOOTER_LENGTH: Int = FOOTER_FIELDS_LENGTH + SIGNATURE_LENGTH

	/** blockTable row: fileOffset u64, storedSize u32, rawSize u32, codec u8, pad u8[3], crc u32, nonce[16] */
	const val BLOCK_ROW_LENGTH: Int = 8 + 4 + 4 + 1 + 3 + 4 + NONCE_LENGTH

	/** toc row: nameHash u64, contentKey u64, blockIndex u32, offsetInBlock u32, rawSize u32, flags u32 */
	const val TOC_ROW_LENGTH: Int = 8 + 8 + 4 + 4 + 4 + 4

	fun littleEndian(capacity: Int): ByteBuffer = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN)

	fun littleEndian(bytes: ByteArray): ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

	/**
	 * Keyed 64-bit hash of a normalised entry path.
	 *
	 * Keyed, not plain: names are never stored, and an unkeyed hash of a small guessable namespace is recoverable by
	 * brute force - which is how Blizzard's MPQ listfiles were reconstructed. HMAC-SHA256 rather than a hand-written
	 * SipHash so no primitive is invented here; truncation to 64 bits gives a collision probability around 3e-12 at
	 * 10 000 entries, and the writer fails the pack on an actual collision rather than trusting that.
	 */
	fun nameHash(profile: XpkProfile, normalisedPath: String): Long {
		val mac = hmac(profile.nameHashMacKey)
		val digest = mac.doFinal(normalisedPath.lowercase().toByteArray(Charsets.UTF_8))
		return littleEndian(digest).getLong(0)
	}

    /** Content identity, so identical payloads share one block and unchanged blocks re-encrypt identically. */
	fun contentKey(bytes: ByteArray, offset: Int, length: Int): Long {
		val digest = MessageDigest.getInstance("SHA-256")
		digest.update(bytes, offset, length)
		return littleEndian(digest.digest()).getLong(0)
	}

	/**
	 * Per-block keystream nonce, derived from the block's plaintext.
	 *
	 * This is what keeps Steam deltas small: an unchanged block encrypts to identical ciphertext across builds, so
	 * only changed bytes are downloaded. A random per-build nonce would change every byte of the archive and force a
	 * full re-download. The trade is that equal plaintext blocks are visibly equal, which for game assets is not a
	 * concern. The reader cannot recompute this - it has no plaintext yet - so the writer stores it in the block
	 * table, which is itself encrypted.
	 */
	fun blockNonce(profile: XpkProfile, plaintext: ByteArray, offset: Int, length: Int): ByteArray {
		val mac = hmac(profile.macKey)
		mac.update(plaintext, offset, length)
		return mac.doFinal().copyOf(NONCE_LENGTH)
	}

	/**
	 * Per-block corruption check, verified on every read.
	 *
	 * CRC32C because it is a JDK intrinsic: measured at ~50 GB/s against HMAC-SHA256's 1.5 GB/s on the project
	 * toolchain, and eight times faster than the AES-CTR pass the same bytes have to make anyway. That is what lets
	 * this be unconditional - a 5 MB asset pays 0.1 ms rather than 3.4 ms, which is below the noise of loading it.
	 *
	 * It detects damage, not forgery, and the distinction is worth stating exactly. A CRC is trivial to recompute,
	 * and four chosen bytes can hold one constant while the rest of a block changes, so somebody who has recovered
	 * the client-side symmetric key can rewrite payload undetected. [metadataChecksum] does not close that: it
	 * authenticates the metadata tables - which entries exist, which blocks they point at - not the block contents.
	 *
	 * That is a deliberate trade, not an oversight. Recovering payload authentication means hashing every entry with
	 * something an attacker cannot forge, and the measurement is stark: HMAC-SHA256 runs at 1551 MB/s here against
	 * CRC32C's 50227, so a 5 MB asset would pay 3.4 ms instead of 0.1 ms on every read. This format exists to make
	 * extraction non-trivial, not to withstand somebody who already holds the key, so the read path buys the check
	 * that catches what actually happens - a truncated download, a bad sector, a half-written patch.
	 */
	fun blockChecksum(bytes: ByteArray, offset: Int, length: Int): Int {
		val crc = CRC32C()
		crc.update(bytes, offset, length)
		return crc.value.toInt()
	}

	/** Nonce for the block table and TOC, derived from the archive salt so it is stable for unchanged content. */
	fun metadataNonce(profile: XpkProfile, salt: ByteArray, purpose: Byte): ByteArray {
		val mac = hmac(profile.macKey)
		mac.update(salt)
		mac.update(purpose)
		return mac.doFinal().copyOf(NONCE_LENGTH)
	}

	const val PURPOSE_BLOCK_TABLE: Byte = 1
	const val PURPOSE_TOC: Byte = 2

	/**
	 * Checksum over both metadata tables, in file order, without concatenating them.
	 *
	 * Signing the table of contents alone would authenticate *what* the entries are while leaving *where they point*
	 * open, since the block table holds each block's offset and nonce. Whoever holds the game-embedded symmetric key
	 * could then re-point an entry and keep the original signature. Ciphertext is signed rather than plaintext so
	 * verification happens before anything is decrypted.
	 *
	 * The limit, stated so it is not mistaken for more: this authenticates the *metadata*, so nobody can author an
	 * archive or re-point an entry without the CI key. It does not authenticate block *contents* - see
	 * [blockChecksum] for why that check is a CRC and what it therefore does not cover.
	 */
	fun metadataDigest(blockTable: ByteArray, toc: ByteArray): ByteArray {
		val digest = MessageDigest.getInstance("SHA-256")
		digest.update(blockTable)
		digest.update(toc)
		return digest.digest()
	}

	/** The footer's 64-bit summary of [metadataDigest], so the cheap check does not need the full digest. */
	fun metadataChecksum(digest: ByteArray): Long = littleEndian(digest).getLong(0)

	/**
	 * What the Ed25519 signature is actually computed over: the 32-byte [metadataDigest], not the tables.
	 *
	 * Pure Ed25519 needs two passes over its message, so the JDK provider buffers everything handed to `update` and
	 * copies it again in `verify` - feeding it the tables directly would hold three copies of attacker-declared data
	 * before the signature could reject it. Signing a collision-resistant digest is the standard construction for
	 * this (it is what Ed25519ph exists for) and makes verification constant-memory.
	 */
	fun signedDigest(signature: java.security.Signature, digest: ByteArray) {
		signature.update(digest)
	}

	/**
	 * Ceiling on the two metadata tables together, checked before either is read.
	 *
	 * This is the one number that bounds opening a hostile archive, so the whole path is enumerated here rather than
	 * left to be re-derived each time a field turns out to be unbounded - which it did, four rounds running:
	 *
	 * | Allocation | Bound |
	 * | --- | --- |
	 * | footer, salt | constant |
	 * | table of contents + block table | this budget, checked before either is read |
	 * | arrays `XpkV2Archive.decode` derives | at most 92.5% of the tables - see below |
	 * | metadata digest, signature input | 32 bytes each, whatever the tables weigh |
	 * | one decoded block | [MAX_BLOCK_RAW_SIZE], and only after the metadata verified |
	 *
	 * The derived-array figure differs by table and the worse one governs: a 32-byte TOC row yields 20 bytes of
	 * primitive arrays (62.5%), but a 40-byte block row yields 37 - offset, two sizes, codec, checksum and a 16-byte
	 * nonce - which is 92.5%. An archive that is almost all block table is therefore the expensive case, giving a
	 * peak near `budget * 1.925`, about 31 MB here.
	 *
	 * 16 MB was not chosen from that total; it is roughly 250 000 entries, far past any real game archive, and the
	 * peak simply follows from it.
	 */
	const val MAX_METADATA_BYTES: Long = 16L * 1024 * 1024

	/**
	 * Whether a block-table row's declared sizes are plausible before anything is allocated from them.
	 *
	 * `rawSize` reaches `ByteArray(rawSize)`, so an unvalidated value is a negative-size exception or an
	 * out-of-memory kill rather than a rejected archive. Signing the block table stops a *tampered* one being
	 * accepted at all, but a development profile carries no signing key and a truncated file is not an attack, so the
	 * range check stands on its own regardless.
	 */
	fun isPlausibleBlock(codec: Byte, storedSize: Int, rawSize: Int): Boolean {
		// Both sizes, not just the decompressed one: `blockOf` allocates `ByteArray(storedSize)` to read the block in
		// before it allocates anything for the decoded form. Capping only `rawSize` left a deflate row free to
		// declare a small decoded size and an enormous stored one, bounded by nothing but the file's own length.
		//
		// With this, every allocation the reader makes from a file-declared value is bounded: the footer counts by
		// MAX_ENTRIES and MAX_BLOCKS, the two tables by those counts, and both block sizes here.
		if (storedSize < 0 || storedSize > MAX_BLOCK_RAW_SIZE) return false
		if (rawSize < 0 || rawSize > MAX_BLOCK_RAW_SIZE) return false
		return when (codec) {
			CODEC_STORE -> rawSize == storedSize
			// Deflate's maximum expansion is a little over 1032:1; anything beyond that is a decompression bomb.
			CODEC_DEFLATE -> rawSize.toLong() <= storedSize.toLong() * MAX_DEFLATE_EXPANSION + 64
			else -> false
		}
	}

	/**
	 * Whether an entry can be packed at all.
	 *
	 * An entry at least a block long becomes a block of its own, and [isPlausibleBlock] refuses a block declaring
	 * more than [MAX_BLOCK_RAW_SIZE]. Both sides consulting one predicate is what stops the writer emitting an
	 * archive its own reader will not open - a disagreement that would surface at load time in a shipped game rather
	 * than in the pack step.
	 */
	fun isPackableEntrySize(size: Int): Boolean = size in 0..MAX_BLOCK_RAW_SIZE

	/** Generous ceiling on a single decoded block: Valve caps pack files at 1-2 GB, so one block cannot near it. */
	const val MAX_BLOCK_RAW_SIZE: Int = 512 * 1024 * 1024

	private const val MAX_DEFLATE_EXPANSION = 1032L

	/**
	 * AES-256 in CTR mode, applied in place.
	 *
	 * CTR because the keystream at any offset follows from the counter, so random access survives encryption - a
	 * block is decryptable without touching the ones before it. Measured at ~6 GB/s in 64 KB chunks on the project
	 * toolchain, roughly twenty times the decompressor behind it, so the cipher is never the bottleneck and a custom
	 * one would be slower as well as weaker.
	 */
	fun crypt(profile: XpkProfile, nonce: ByteArray, data: ByteArray, offset: Int, length: Int) {
		val cipher = ciphers.get()
		cipher.init(Cipher.ENCRYPT_MODE, profile.aesKey, IvParameterSpec(nonce))
		// CTR is its own inverse, and doFinal into the same array is an in-place XOR with the keystream.
		cipher.doFinal(data, offset, length, data, offset)
	}

	/** Deterministic archive salt: the same content set always produces the same file. */
	fun deriveSalt(profile: XpkProfile, sortedNameHashes: LongArray, sortedContentKeys: LongArray): ByteArray {
		val mac = hmac(profile.macKey)
		val scratch = littleEndian(16)
		for (index in sortedNameHashes.indices) {
			scratch.clear()
			scratch.putLong(sortedNameHashes[index])
			scratch.putLong(sortedContentKeys[index])
			mac.update(scratch.array(), 0, 16)
		}
		return mac.doFinal().copyOf(SALT_LENGTH)
	}

	/** Obfuscates the footer in place; its own inverse. Keeps the tail from reading as a structured header. */
	fun maskFooter(profile: XpkProfile, footer: ByteArray, fileLength: Long) {
		var state = profile.footerMask xor fileLength xor (VERSION.toLong() shl 48)
		for (index in footer.indices) {
			// xorshift64*, enough to remove structure from bytes that are already ciphertext-adjacent.
			state = state xor (state shl 13)
			state = state xor (state ushr 7)
			state = state xor (state shl 17)
			footer[index] = (footer[index].toInt() xor (state and 0xFF).toInt()).toByte()
		}
	}

	/**
	 * A Mac per thread rather than per call.
	 *
	 * Name hashing runs on every asset lookup and block nonces on every block read, so allocating a provider object
	 * each time would put a steady allocation on the loading path for nothing. `Mac` is not thread-safe, hence the
	 * thread-local; `init` resets it, so a reused instance carries nothing between calls.
	 */
	private val macs = ThreadLocal.withInitial { Mac.getInstance("HmacSHA256") }

	private val ciphers = ThreadLocal.withInitial { Cipher.getInstance("AES/CTR/NoPadding") }

	private fun hmac(key: SecretKeySpec): Mac = macs.get().apply { init(key) }
}
