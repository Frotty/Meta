package de.fatox.meta.assets.xpk

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import de.fatox.meta.assets.normalisedPath
import de.fatox.meta.assets.xpk.XpkFormat.BLOCK_ROW_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.CODEC_DEFLATE
import de.fatox.meta.assets.xpk.XpkFormat.CODEC_STORE
import de.fatox.meta.assets.xpk.XpkFormat.FOOTER_FIELDS_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.FOOTER_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.NONCE_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.SALT_LENGTH
import de.fatox.meta.assets.xpk.XpkFormat.TOC_ROW_LENGTH
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.Signature
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Reads an XPK v2 archive.
 *
 * Blocks are independently decodable, so reading an entry touches only the block holding it. That removes the reason
 * v1 needed a shared cursor, a pass-through cache and a retention policy at all - and with them the whole class of
 * defects those produced, where cleanup had to guess when a caller was finished (see `docs/xpk-format-audit.md` M2).
 * A read here allocates its own result and leaves at most one bounded block behind.
 *
 * Entries carry keyed name hashes rather than paths, so the archive cannot list what it holds. That is deliberate -
 * an extractor gets nameless blobs - and it means callers resolve by name through [find] rather than enumerating.
 */
class XpkV2Archive internal constructor(
	private val profile: XpkProfile,
	private val channel: FileChannel,
	private val archivePath: String,
	/** Entry name hashes, ascending, so a lookup is a binary search with no allocation. */
	private val nameHashes: LongArray,
	private val entryBlock: IntArray,
	private val entryOffset: IntArray,
	private val entrySize: IntArray,
	private val blockFileOffset: LongArray,
	private val blockStoredSize: IntArray,
	private val blockRawSize: IntArray,
	private val blockCodec: ByteArray,
	private val blockNonces: ByteArray,
) : Disposable {
	private val lock = Any()
	private var disposed = false

	/**
	 * One decoded block, so several entries sharing a block decode it once.
	 *
	 * Deliberately a single slot rather than an LRU: entries that travel together are packed together, so a load
	 * burst hits the same block repeatedly, and one slot is bounded by construction - there is no policy to get
	 * wrong and nothing to release on a schedule.
	 */
	private var cachedBlockIndex = -1
	private var cachedBlock: ByteArray? = null

	val entryCount: Int get() = nameHashes.size

	/** Block start offsets, for tests that check the on-disk alignment the layout promises. */
	internal val blockOffsets: LongArray get() = blockFileOffset.copyOf()

	/** Resolves a path to a handle, or `null` when the archive does not hold it. */
	fun find(path: String): FileHandle? {
		val normalised = normalisedPath(path)
		if (normalised.isEmpty()) return null
		val index = indexOf(XpkFormat.nameHash(profile, normalised))
		return if (index < 0) null else XpkV2FileHandle(this, index, normalised)
	}

	internal fun sizeOf(entryIndex: Int): Long = entrySize[entryIndex].toLong()

	private fun indexOf(nameHash: Long): Int {
		var low = 0
		var high = nameHashes.size - 1
		while (low <= high) {
			val mid = (low + high) ushr 1
			val candidate = nameHashes[mid]
			when {
				candidate < nameHash -> low = mid + 1
				candidate > nameHash -> high = mid - 1
				else -> return mid
			}
		}
		return -1
	}

	/** Decodes one entry. Touches only the block holding it - no prefix, no sweep. */
	internal fun bytesOf(entryIndex: Int): ByteArray = synchronized(lock) {
		check(!disposed) { "XPK archive $archivePath was disposed" }
		val block = blockOf(entryBlock[entryIndex])
		val offset = entryOffset[entryIndex]
		val size = entrySize[entryIndex]
		// Long maths: two ints read from the file could overflow to a negative sum and slip past the bound.
		if (offset < 0 || size < 0 || offset.toLong() + size > block.size.toLong()) {
			throw GdxRuntimeException("XPK entry $entryIndex is out of bounds in $archivePath")
		}
		return block.copyOfRange(offset, offset + size)
	}

	private fun blockOf(blockIndex: Int): ByteArray {
		cachedBlock?.let { if (cachedBlockIndex == blockIndex) return it }

		val stored = ByteArray(blockStoredSize[blockIndex])
		readFully(stored, blockFileOffset[blockIndex])

		val nonce = ByteArray(NONCE_LENGTH)
		blockNonces.copyInto(nonce, 0, blockIndex * NONCE_LENGTH, (blockIndex + 1) * NONCE_LENGTH)
		XpkFormat.crypt(profile, nonce, stored, 0, stored.size)

		val raw = when (val codec = blockCodec[blockIndex]) {
			CODEC_STORE -> stored
			CODEC_DEFLATE -> inflate(stored, blockRawSize[blockIndex])
			else -> throw GdxRuntimeException("Unknown XPK codec $codec in $archivePath")
		}
		// A block whose plaintext does not hash to the nonce it was stored with has been altered or corrupted.
		val expected = XpkFormat.blockNonce(profile, raw, 0, raw.size)
		if (!expected.contentEquals(nonce)) {
			throw GdxRuntimeException("XPK block $blockIndex failed its integrity check in $archivePath")
		}

		cachedBlockIndex = blockIndex
		cachedBlock = raw
		return raw
	}

	private fun inflate(stored: ByteArray, rawSize: Int): ByteArray {
		val out = ByteArray(rawSize)
		val inflater = Inflater(true)
		try {
			InflaterInputStream(ByteArrayInputStream(stored), inflater, 1 shl 16).use { input ->
				var read = 0
				while (read < rawSize) {
					val count = input.read(out, read, rawSize - read)
					if (count < 0) break
					read += count
				}
				if (read != rawSize) {
					throw GdxRuntimeException("XPK block decompressed to $read bytes, expected $rawSize")
				}
			}
		} finally {
			inflater.end()
		}
		return out
	}

	private fun readFully(destination: ByteArray, fileOffset: Long) {
		val buffer = ByteBuffer.wrap(destination)
		var position = fileOffset
		while (buffer.hasRemaining()) {
			// Positional reads: thread-safe, no channel position to share, and no mapping to hold the file open
			// against a patcher - which on Windows would block updating the archive while the game runs.
			val count = channel.read(buffer, position)
			if (count < 0) throw GdxRuntimeException("Unexpected end of XPK file $archivePath at $position")
			position += count
		}
	}

	override fun dispose() {
		synchronized(lock) {
			if (disposed) return
			disposed = true
			cachedBlock = null
			cachedBlockIndex = -1
			runCatching { channel.close() }
		}
	}

	companion object {
		/**
		 * Opens [fileHandle] as a v2 archive, or returns `null` when it is not one under [profile].
		 *
		 * There is no magic to test: the file starts with a salt indistinguishable from noise, so an archive is
		 * identified by successfully unmasking a footer whose recorded version and profile match and whose table of
		 * contents hashes to the checksum it carries. A wrong profile therefore reads as "not mine" rather than
		 * producing garbage - which is also how a v1 archive falls through to the v1 reader.
		 *
		 * Also `null` for anything that is not a real file on disk: reading blocks on demand needs positional reads,
		 * not a classpath stream.
		 */
		fun openOrNull(profile: XpkProfile, fileHandle: FileHandle): XpkV2Archive? {
			val file = runCatching { fileHandle.file() }.getOrNull() ?: return null
			if (!file.isFile) return null
			return openOrNull(profile, file.toPath(), fileHandle.path())
		}

		internal fun openOrNull(profile: XpkProfile, path: Path, displayPath: String): XpkV2Archive? {
			val channel = FileChannel.open(path, StandardOpenOption.READ)
			var keepOpen = false
			try {
				val fileLength = channel.size()
				if (fileLength < SALT_LENGTH + FOOTER_LENGTH) return null

				val footer = ByteArray(FOOTER_LENGTH)
				if (!tryRead(channel, footer, fileLength - FOOTER_LENGTH)) return null
				XpkFormat.maskFooter(profile, footer, fileLength)

				val fields = XpkFormat.littleEndian(footer)
				val tocOffset = fields.getLong()
				val tocLength = fields.getInt()
				val blockCount = fields.getInt()
				val entryCount = fields.getInt()
				val version = fields.getShort().toInt() and 0xFFFF
				val profileId = fields.getShort().toInt() and 0xFFFF
				val tocChecksum = fields.getLong()

				if (version != XpkFormat.VERSION || profileId != profile.profileId) return null

				// Everything below comes from the file, so it is arithmetic on untrusted values. Long maths and an
				// explicit ceiling keep a hostile or truncated footer from overflowing into a length that passes the
				// checks and then reads off the end of a buffer.
				if (entryCount < 0 || blockCount < 0) return null
				if (entryCount > MAX_ENTRIES || blockCount > MAX_BLOCKS) return null
				if (tocLength.toLong() != entryCount.toLong() * TOC_ROW_LENGTH) return null
				val blockTableLength = blockCount.toLong() * BLOCK_ROW_LENGTH
				val bodyEnd = fileLength - FOOTER_LENGTH
				if (tocOffset < SALT_LENGTH || tocOffset > bodyEnd) return null
				if (tocOffset + tocLength.toLong() > bodyEnd) return null
				if (tocOffset - blockTableLength < SALT_LENGTH) return null

				val salt = ByteArray(SALT_LENGTH)
				if (!tryRead(channel, salt, 0)) return null

				val toc = ByteArray(tocLength)
				if (!tryRead(channel, toc, tocOffset)) return null

				val blockTable = ByteArray(blockTableLength.toInt())
				if (!tryRead(channel, blockTable, tocOffset - blockTableLength)) return null

				// Checksum and signature cover both tables as stored, so nothing that decides which bytes an entry
				// resolves to is left unauthenticated. Verified before either is decrypted.
				val signedMaterial = XpkFormat.signedMaterial(blockTable, toc)
				if (XpkFormat.contentKey(signedMaterial, 0, signedMaterial.size) != tocChecksum) return null
				verifySignature(profile, footer, signedMaterial, displayPath)

				XpkFormat.crypt(profile, XpkFormat.metadataNonce(profile, salt, XpkFormat.PURPOSE_TOC), toc, 0, toc.size)
				XpkFormat.crypt(
					profile,
					XpkFormat.metadataNonce(profile, salt, XpkFormat.PURPOSE_BLOCK_TABLE),
					blockTable,
					0,
					blockTable.size,
				)

				val archive = decode(profile, channel, displayPath, toc, blockTable, entryCount, blockCount, fileLength)
				keepOpen = true
				return archive
			} catch (_: IOException) {
				return null
			} finally {
				if (!keepOpen) runCatching { channel.close() }
			}
		}

		private fun verifySignature(profile: XpkProfile, footer: ByteArray, signed: ByteArray, displayPath: String) {
			val signingKey = profile.tocSigningKey ?: return
			val signature = footer.copyOfRange(FOOTER_FIELDS_LENGTH, FOOTER_LENGTH)
			val verifier = Signature.getInstance("Ed25519")
			verifier.initVerify(signingKey)
			verifier.update(signed)
			// Not a "not mine" result: the archive identified itself as this profile's and then failed to prove it,
			// which is the case the signature exists to catch.
			if (!verifier.verify(signature)) {
				throw GdxRuntimeException("XPK archive $displayPath has an invalid table-of-contents signature")
			}
		}

		private fun decode(
			profile: XpkProfile,
			channel: FileChannel,
			displayPath: String,
			toc: ByteArray,
			blockTable: ByteArray,
			entryCount: Int,
			blockCount: Int,
			fileLength: Long,
		): XpkV2Archive {
			val nameHashes = LongArray(entryCount)
			val entryBlock = IntArray(entryCount)
			val entryOffset = IntArray(entryCount)
			val entrySize = IntArray(entryCount)
			val tocBuffer = XpkFormat.littleEndian(toc)
			for (index in 0 until entryCount) {
				nameHashes[index] = tocBuffer.getLong()
				tocBuffer.getLong() // contentKey: dedup bookkeeping, not needed to read
				entryBlock[index] = tocBuffer.getInt()
				entryOffset[index] = tocBuffer.getInt()
				entrySize[index] = tocBuffer.getInt()
				tocBuffer.getInt() // flags, reserved
				if (index > 0 && nameHashes[index] <= nameHashes[index - 1]) {
					throw GdxRuntimeException("XPK table of contents in $displayPath is not sorted")
				}
				if (entryBlock[index] !in 0 until blockCount) {
					throw GdxRuntimeException("XPK entry $index references block ${entryBlock[index]} in $displayPath")
				}
			}

			val blockFileOffset = LongArray(blockCount)
			val blockStoredSize = IntArray(blockCount)
			val blockRawSize = IntArray(blockCount)
			val blockCodec = ByteArray(blockCount)
			val blockNonces = ByteArray(blockCount * NONCE_LENGTH)
			val blockBuffer = XpkFormat.littleEndian(blockTable)
			for (index in 0 until blockCount) {
				blockFileOffset[index] = blockBuffer.getLong()
				blockStoredSize[index] = blockBuffer.getInt()
				blockRawSize[index] = blockBuffer.getInt()
				blockCodec[index] = blockBuffer.get()
				blockBuffer.position(blockBuffer.position() + 3)
				blockBuffer.get(blockNonces, index * NONCE_LENGTH, NONCE_LENGTH)
				val offset = blockFileOffset[index]
				val stored = blockStoredSize[index]
				// rawSize reaches ByteArray(rawSize) when the block is read, so an unchecked value is a negative-size
				// throw or an out-of-memory kill instead of a rejected archive. Signing the block table stops a
				// tampered one being accepted at all, but a development profile carries no signing key and a
				// truncated file is not an attack, so the range check has to stand on its own too.
				if (!XpkFormat.isPlausibleBlock(blockCodec[index], stored, blockRawSize[index])) {
					throw GdxRuntimeException("XPK block $index declares implausible sizes in $displayPath")
				}
				if (offset < SALT_LENGTH || offset + stored.toLong() > fileLength - FOOTER_LENGTH) {
					throw GdxRuntimeException("XPK block $index lies outside $displayPath")
				}
			}

			return XpkV2Archive(
				profile, channel, displayPath,
				nameHashes, entryBlock, entryOffset, entrySize,
				blockFileOffset, blockStoredSize, blockRawSize, blockCodec, blockNonces,
			)
		}

		/** Ceilings for footer-declared counts, well past any real archive, so overflow cannot be reached. */
		private const val MAX_ENTRIES = 1 shl 24
		private const val MAX_BLOCKS = 1 shl 24

		private fun tryRead(channel: FileChannel, destination: ByteArray, fileOffset: Long): Boolean {
			val buffer = ByteBuffer.wrap(destination)
			var position = fileOffset
			while (buffer.hasRemaining()) {
				val count = channel.read(buffer, position)
				if (count < 0) return false
				position += count
			}
			return true
		}
	}
}
