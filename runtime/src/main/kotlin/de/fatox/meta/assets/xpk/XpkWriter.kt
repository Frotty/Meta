package de.fatox.meta.assets.xpk

import de.fatox.meta.assets.assetPathKey
import de.fatox.meta.assets.xpk.XpkFormat.BLOCK_ROW_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.BLOCK_ALIGNMENT
import de.fatox.meta.assets.xpk.XpkFormat.BLOCK_SIZE
import de.fatox.meta.assets.xpk.XpkFormat.CODEC_DEFLATE
import de.fatox.meta.assets.xpk.XpkFormat.CODEC_STORE
import de.fatox.meta.assets.xpk.XpkFormat.FOOTER_FIELDS_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.FOOTER_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.NONCE_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.PAGE_SIZE
import de.fatox.meta.assets.xpk.XpkFormat.SALT_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.TOC_ROW_LENGTH
import java.io.ByteArrayOutputStream
import java.security.PrivateKey
import java.security.Signature
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/**
 * Builds an XPK v2 archive.
 *
 * Meta owns the writer as well as the reader so the format has one authoritative definition. Before this, the reader
 * was the only specification and nothing pinned down what it would actually meet in the field.
 *
 * **Output is deterministic.** The same inputs produce byte-identical output: entries are sorted by name hash,
 * identical payloads share one block, block nonces come from block content rather than randomness, and the archive
 * salt is derived from the content set. Every measure in `docs/xpk-format-audit.md` §9 depends on this - a packer
 * that reshuffles or re-salts between builds makes Steam re-download the archive whatever else is done.
 */
class XpkWriter(private val profile: XpkProfile) {
	private val sources = LinkedHashMap<String, ByteArray>()

	/** Adds one entry. [path] is normalised and hashed; the plaintext name is never written to the archive. */
	fun add(path: String, bytes: ByteArray): XpkWriter {
		val normalised = assetPathKey(path)
		require(normalised.isNotEmpty()) { "Entry path is empty after normalisation: '$path'" }
		// An entry at least a block long becomes a block of its own, and the reader refuses a block declaring more
		// than this. Catching it here means the pack fails rather than producing an archive its own reader cannot
		// open - both sides read the limit from XpkFormat so they cannot drift apart.
		require(XpkFormat.isPackableEntrySize(bytes.size)) {
			"Entry $normalised is ${bytes.size} bytes, over the ${XpkFormat.MAX_BLOCK_RAW_SIZE}-byte block limit"
		}
		require(sources.put(normalised, bytes) == null) { "Duplicate entry path: $normalised" }
		return this
	}

	/**
	 * Produces the archive.
	 *
	 * [signingKey] is the Ed25519 private key for the table of contents. It belongs in CI, never in the game: with
	 * it withheld, nobody can author an archive an unmodified game accepts, even knowing the format and the symmetric
	 * key. Omitting it produces an unsigned archive, which only a profile with no signing key will load.
	 */
	fun build(signingKey: PrivateKey? = null): ByteArray {
		val entries = planEntries()
		val blocks = planBlocks(entries)

		val salt = XpkFormat.deriveSalt(
			profile,
			LongArray(entries.size) { entries[it].nameHash },
			LongArray(entries.size) { entries[it].contentKey },
		)

		val payload = layOutPages(blocks)
		val blockTable = encodeBlockTable(blocks, salt)
		val toc = encodeToc(entries, salt)

        val tocOffset = SALT_LENGTH.toLong() + payload.size + blockTable.size
		val body = ByteArrayOutputStream(SALT_LENGTH + payload.size + blockTable.size + toc.size + FOOTER_LENGTH)
		body.write(salt)
		body.write(payload)
		body.write(blockTable)
		body.write(toc)

		val fileLength = body.size().toLong() + FOOTER_LENGTH
		body.write(
			buildFooter(
				tocOffset = tocOffset,
				tocLength = toc.size,
				blockCount = blocks.size,
				entryCount = entries.size,
				blockTable = blockTable,
				toc = toc,
				fileLength = fileLength,
				signingKey = signingKey,
			),
		)
		return body.toByteArray()
	}

	// ---- planning -------------------------------------------------------------------------------------------

	private class PlannedEntry(
		val path: String,
		val nameHash: Long,
		val contentKey: Long,
		val bytes: ByteArray,
	) {
		var blockIndex: Int = -1
		var offsetInBlock: Int = 0
	}

	private class PlannedBlock(val raw: ByteArray) {
		var stored: ByteArray = raw
		var codec: Byte = CODEC_STORE
		var nonce: ByteArray = ByteArray(NONCE_LENGTH)
		var checksum: Int = 0
		var fileOffset: Long = 0
	}

	/** Sorted by name hash so the layout depends on content alone, never on insertion order. */
	private fun planEntries(): List<PlannedEntry> {
		val planned = sources.map { (path, bytes) ->
			PlannedEntry(path, XpkFormat.nameHash(profile, path), XpkFormat.contentKey(bytes, 0, bytes.size), bytes)
		}.sortedBy { it.nameHash }

		for (index in 1 until planned.size) {
			// 64 bits over a few thousand entries makes this vanishingly unlikely, but a silent collision would
			// serve one asset's bytes for another's name, so the pack fails rather than the game.
			check(planned[index].nameHash != planned[index - 1].nameHash) {
                "Name hash collision between '${planned[index - 1].path}' and '${planned[index].path}'"
			}
		}
		return planned
	}

	/**
	 * Packs entries into blocks, sharing one block between identical payloads.
	 *
	 * Content addressing is free dedup - reskins and level variants that ship the same bytes are stored once - and it
	 * is what lets an unchanged block keep its nonce, and therefore its ciphertext, across builds.
	 */
	private fun planBlocks(entries: List<PlannedEntry>): List<PlannedBlock> {
		val blocks = ArrayList<PlannedBlock>()
		val byContent = HashMap<Long, Pair<Int, Int>>()
		var open: ByteArrayOutputStream? = null
		var openIndex = -1

		// The open block is promised index `openIndex`, so nothing may be appended to `blocks` while it is still
		// open - the entries already pointing at it would end up naming whatever was appended instead.
		fun seal() {
			val pending = open ?: return
			blocks.add(PlannedBlock(pending.toByteArray()))
			open = null
			openIndex = -1
		}

		for (entryIndex in entries.indices) {
			val entry = entries[entryIndex]
			val shared = byContent[entry.contentKey]
			if (shared != null) {
				// Identical payload already placed: point at it rather than storing the bytes twice.
				entry.blockIndex = shared.first
				entry.offsetInBlock = shared.second
				continue
			}

			// An entry at least a block long gets a block of its own, so a block never holds a partial entry and a
			// read never has to stitch two together.
			if (entry.bytes.size >= BLOCK_SIZE) {
				seal()
				blocks.add(PlannedBlock(entry.bytes))
				entry.blockIndex = blocks.size - 1
				entry.offsetInBlock = 0
				byContent[entry.contentKey] = entry.blockIndex to 0
				continue
			}

			var pending = open
			if (pending == null || pending.size() + entry.bytes.size > BLOCK_SIZE) {
				seal()
				pending = ByteArrayOutputStream(BLOCK_SIZE)
				open = pending
				openIndex = blocks.size
			}
			entry.blockIndex = openIndex
			entry.offsetInBlock = pending.size()
			byContent[entry.contentKey] = entry.blockIndex to entry.offsetInBlock
			pending.write(entry.bytes)
		}
		seal()

		for (index in blocks.indices) compressAndSeal(blocks[index])
		return blocks
	}

	private fun compressAndSeal(block: PlannedBlock) {
		val deflated = deflate(block.raw)
		// Compression that saves almost nothing costs decode time for no benefit, and for already-compressed
		// payloads - png, ogg, ktx2 - it reliably saves nothing at all.
		val worthIt = deflated.size < block.raw.size * XpkFormat.STORE_RAW_RATIO
		block.stored = if (worthIt) deflated else block.raw.copyOf()
		block.codec = if (worthIt) CODEC_DEFLATE else CODEC_STORE
		block.checksum = XpkFormat.blockChecksum(block.raw, 0, block.raw.size)
		// Nonce from the *plaintext*, so an unchanged block is byte-identical after encryption in the next build.
		block.nonce = XpkFormat.blockNonce(profile, block.raw, 0, block.raw.size)
		XpkFormat.crypt(profile, block.nonce, block.stored, 0, block.stored.size)
	}

	private fun deflate(raw: ByteArray): ByteArray {
		val out = ByteArrayOutputStream(raw.size / 2 + 64)
		val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
		try {
			DeflaterOutputStream(out, deflater, 1 shl 16).use { it.write(raw) }
		} finally {
			deflater.end()
		}
		return out.toByteArray()
	}

	/**
	 * Lays blocks out with 4 KB-aligned starts inside 1 MB pages that blocks never straddle.
	 *
	 * Two levels, for two reasons. Block starts are quantised to [BLOCK_ALIGNMENT] so a re-compression that lands in
	 * the same bucket moves nothing at all; pages are [PAGE_SIZE] because that is SteamPipe's chunking unit, so a
	 * shift that does happen is confined to the page it occurs in rather than running to the end of the file.
	 *
	 * What this does *not* promise is that a changed block never moves its neighbours - exact offset stability needs
	 * fixed-size slots, which for 64 KB blocks compressing 2:1 would waste about half the archive. What it does
	 * guarantee is that an unchanged block's bytes are unchanged, which is what lets SteamPipe's chunk search find
	 * them again. See `docs/xpk-format-audit.md` §9.
	 */
	private fun layOutPages(blocks: List<PlannedBlock>): ByteArray {
		val out = ByteArrayOutputStream()
		var pageUsed = 0
		for (index in blocks.indices) {
			val block = blocks[index]
			val size = block.stored.size
			val padded = alignUp(size, BLOCK_ALIGNMENT)

			// Start a fresh page when this block would not fit in what is left of the current one.
			if (pageUsed > 0 && pageUsed + padded > PAGE_SIZE) {
				out.write(ByteArray(PAGE_SIZE - pageUsed))
				pageUsed = 0
			}

			block.fileOffset = SALT_LENGTH.toLong() + out.size()
			out.write(block.stored)
			out.write(ByteArray(padded - size))

			if (padded > PAGE_SIZE) {
				// An entry that is incompressible and larger than a page - a big texture or an audio stream - cannot
				// be made to fit one, and splitting it would only spread the same bytes over the same chunks. What
				// must not happen is that it leaves everything after it off-boundary, so the run is padded out to a
				// whole number of pages and the next block starts aligned again.
				out.write(ByteArray(alignUp(padded, PAGE_SIZE) - padded))
				pageUsed = 0
			} else {
				pageUsed += padded
				if (pageUsed == PAGE_SIZE) pageUsed = 0
			}
		}
		return out.toByteArray()
	}

	private fun alignUp(value: Int, alignment: Int): Int = (value + alignment - 1) / alignment * alignment

	// ---- encoding -------------------------------------------------------------------------------------------

	private fun encodeBlockTable(blocks: List<PlannedBlock>, salt: ByteArray): ByteArray {
		val buffer = XpkFormat.littleEndian(blocks.size * BLOCK_ROW_LENGTH)
		for (index in blocks.indices) {
			val block = blocks[index]
			buffer.putLong(block.fileOffset)
			buffer.putInt(block.stored.size)
			buffer.putInt(block.raw.size)
			buffer.put(block.codec)
			buffer.put(ByteArray(3))
			buffer.putInt(block.checksum)
			buffer.put(block.nonce)
		}
		val bytes = buffer.array()
		XpkFormat.crypt(profile, XpkFormat.metadataNonce(profile, salt, XpkFormat.PURPOSE_BLOCK_TABLE), bytes, 0, bytes.size)
		return bytes
	}

	private fun encodeToc(entries: List<PlannedEntry>, salt: ByteArray): ByteArray {
		val buffer = XpkFormat.littleEndian(entries.size * TOC_ROW_LENGTH)
		for (index in entries.indices) {
			val entry = entries[index]
			buffer.putLong(entry.nameHash)
			buffer.putLong(entry.contentKey)
			buffer.putInt(entry.blockIndex)
			buffer.putInt(entry.offsetInBlock)
			buffer.putInt(entry.bytes.size)
			buffer.putInt(0)
		}
		val bytes = buffer.array()
		XpkFormat.crypt(profile, XpkFormat.metadataNonce(profile, salt, XpkFormat.PURPOSE_TOC), bytes, 0, bytes.size)
		return bytes
	}

	private fun buildFooter(
		tocOffset: Long,
		tocLength: Int,
		blockCount: Int,
		entryCount: Int,
		blockTable: ByteArray,
		toc: ByteArray,
		fileLength: Long,
		signingKey: PrivateKey?,
	): ByteArray {
		// Both tables, not just the table of contents. The block table carries each block's offset and its nonce, so
		// signing the TOC alone leaves the mapping from entry to bytes unauthenticated: somebody holding the
		// game-embedded symmetric key but not the CI signing key could re-encrypt a block, install a matching nonce,
		// and keep the original signature. Everything that decides which bytes an entry resolves to is signed.
		val fields = XpkFormat.littleEndian(FOOTER_FIELDS_LENGTH)
		fields.putLong(tocOffset)
		fields.putInt(tocLength)
		fields.putInt(blockCount)
		fields.putInt(entryCount)
		fields.putShort(XpkFormat.VERSION.toShort())
		fields.putShort(profile.profileId.toShort())
		fields.putLong(XpkFormat.metadataChecksum(blockTable, toc))

		val signature = ByteArray(XpkFormat.SIGNATURE_LENGTH)
		if (signingKey != null) {
			val signer = Signature.getInstance("Ed25519")
			signer.initSign(signingKey)
			XpkFormat.updateWithMetadata(signer, blockTable, toc)
			val produced = signer.sign()
			check(produced.size == XpkFormat.SIGNATURE_LENGTH) {
				"Ed25519 signature was ${produced.size} bytes, expected ${XpkFormat.SIGNATURE_LENGTH}"
			}
			produced.copyInto(signature)
		}

		val footer = ByteArray(FOOTER_LENGTH)
		fields.array().copyInto(footer)
		signature.copyInto(footer, FOOTER_FIELDS_LENGTH)
		XpkFormat.maskFooter(profile, footer, fileLength)
		return footer
	}
}

