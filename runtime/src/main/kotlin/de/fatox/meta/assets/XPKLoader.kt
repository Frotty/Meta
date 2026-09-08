package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.crypto.HASH_LENGTH
import de.fatox.meta.api.crypto.StreamingXXH64
import de.fatox.meta.api.crypto.checkHash
import org.apache.commons.compress.archivers.sevenz.SevenZFile

object XPKLoader {
	const val EXTENSION: String = "xpk"

	/**
	 * Opens an archive and indexes its entries. The caller owns the returned [XpkArchive] and must dispose it;
	 * [MetaAssetProvider] does so from its own `dispose`.
	 */
	fun open(fileHandle: FileHandle): XpkArchive = open(fileHandle, PASSTHROUGH_CACHE_BUDGET)

	/**
	 * Overrides for callers that own the archive's lifetime.
	 *
	 * The budget exists so tests can exercise exhaustion without building a 64 MB archive; [retainAfterRead] lets
	 * [MetaAssetProvider] keep buffers only while a load phase is actually running - see [XpkArchive].
	 */
	internal fun open(
		fileHandle: FileHandle,
		passthroughCacheBudget: Long = PASSTHROUGH_CACHE_BUDGET,
		retainAfterRead: () -> Boolean = { true },
	): XpkArchive {
		val fileBytes = readAndVerify(fileHandle)
		restoreSignature(fileBytes)

		val names = ArrayList<String>()
		val sizes = ArrayList<Long>()
		val directories = ArrayList<Boolean>()
		// One header parse, then the reader is closed: the enumeration pass needs no entry payloads. Closing it also
		// closes its channel view, which is why each reader gets a fresh one - see XpkArchive.restart.
		SevenZFile.Builder().setSeekableByteChannel(XpkReadOnlyChannel(fileBytes)).get().use { file ->
			var entry = file.nextEntry
			while (entry != null) {
				names.add(entry.name)
				sizes.add(if (entry.isDirectory) 0L else entry.size)
				directories.add(entry.isDirectory)
				entry = file.nextEntry
			}
		}

		return XpkArchive(
			fileBytes,
			fileHandle.path(),
			names.toTypedArray(),
			LongArray(sizes.size) { sizes[it] },
			BooleanArray(directories.size) { directories[it] },
			passthroughCacheBudget,
			retainAfterRead,
		)
	}

	/** Lists archive paths without exposing Apache Commons Compress types to the caller. */
	fun listEntryNames(fileHandle: FileHandle): Array<String> {
		val archive = open(fileHandle)
		try {
			val handles = archive.allEntries
			val names = Array<String>(handles.size)
			for (index in 0 until handles.size) names.add(handles[index].path())
			return names
		} finally {
			archive.dispose()
		}
	}

	/**
	 * Retained for consumers that need libGDX file handles for lazy entry reads.
	 *
	 * Prefer [open]: this returns the entries without the archive that owns them, so the caller has nothing obvious
	 * to dispose. The handles do carry it - [XPKFileHandle.archive] - but the ownership is easy to miss, and an
	 * undisposed archive pins its 7z reader and cached entry buffers for as long as any handle lives.
	 */
	@Deprecated(
		"Use open(fileHandle), which returns the disposable archive that owns the entries.",
		ReplaceWith("open(fileHandle).entries"),
	)
	fun getList(fileHandle: FileHandle): Array<XPKFileHandle> = open(fileHandle).allEntries

	/**
	 * The first six bytes of the 7z signature are overwritten at pack time so a magic-byte scan does not find the
	 * archive; put them back before handing the bytes to the reader.
	 */
	private fun restoreSignature(fileBytes: ByteArray) {
		fileBytes[0] = '7'.code.toByte()
		fileBytes[1] = 'z'.code.toByte()
		fileBytes[2] = 0xBC.toByte()
		fileBytes[3] = 0xAF.toByte()
		fileBytes[4] = 0x27.toByte()
		fileBytes[5] = 0x1C.toByte()
	}

	private fun readAndVerify(fileHandle: FileHandle): ByteArray {
		val fileLength = fileHandle.length()
		require(fileLength in HASH_LENGTH.toLong()..Int.MAX_VALUE.toLong()) {
			"Invalid XPK length $fileLength for ${fileHandle.path()}"
		}
		val bytes = ByteArray(fileLength.toInt())
		val contentLength = bytes.size - HASH_LENGTH
		val streamingHash = StreamingXXH64()
		fileHandle.read().use { input ->
			var offset = 0
			while (offset < bytes.size) {
				val read = input.read(bytes, offset, minOf(IO_CHUNK_SIZE, bytes.size - offset))
				if (read < 0) break
				if (read == 0) continue
				if (offset < contentLength) {
					val hashLength = minOf(read, contentLength - offset)
					if (hashLength > 0) streamingHash.update(bytes, offset, hashLength)
				}
				offset += read
			}
			check(offset == bytes.size) { "Unexpected end of XPK file ${fileHandle.path()} at $offset/${bytes.size}" }
		}
		checkHash(bytes, streamingHash.digest())
		return bytes
	}

	private const val IO_CHUNK_SIZE = 64 * 1024
}
