package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.crypto.HASH_LENGTH
import de.fatox.meta.api.crypto.StreamingXXH64
import de.fatox.meta.api.crypto.checkHash
import org.apache.commons.compress.archivers.sevenz.SevenZFile

object XPKLoader {
	const val EXTENSION: String = "xpk"

	/** Lists archive paths without exposing Apache Commons Compress types to the caller. */
	fun listEntryNames(fileHandle: FileHandle): Array<String> {
		val handles = getList(fileHandle)
		val names = Array<String>(handles.size)
		for (index in 0 until handles.size) names.add(handles[index].path())
		return names
	}

	/** Retained for consumers that need libGDX file handles for lazy entry reads. */
	fun getList(fileHandle: FileHandle): Array<XPKFileHandle> {
		val fileBytes = readAndVerify(fileHandle)

		fileBytes[0] = '7'.code.toByte()
		fileBytes[1] = 'z'.code.toByte()
		fileBytes[2] = 0xBC.toByte()
		fileBytes[3] = 0xAF.toByte()
		fileBytes[4] = 0x27.toByte()
		fileBytes[5] = 0x1C.toByte()

		val array = Array<XPKFileHandle>()
		val byteChannel = XPKByteChannel(fileBytes)
		SevenZFile.Builder().setSeekableByteChannel(byteChannel).get().let {
			var archive = it.nextEntry
			var entriesSinceYield = 0
			while (archive != null) {
				val xpkFileHandle = XPKFileHandle(
					array,
					archive.size.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
					byteChannel,
					archive.name,
					archive.size,
					archive.name.replace("/", "\\"),
				)
				array.add(xpkFileHandle)
				archive = it.nextEntry
				if (++entriesSinceYield >= ENTRIES_PER_YIELD) {
					entriesSinceYield = 0
					Thread.yield()
				}
			}
		}
		return array
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
				Thread.yield()
			}
			check(offset == bytes.size) { "Unexpected end of XPK file ${fileHandle.path()} at $offset/${bytes.size}" }
		}
		checkHash(bytes, streamingHash.digest())
		return bytes
	}

	internal fun loadEntry(file: XPKByteChannel, entryName: String, entrySize: Long): ByteArray? = synchronized(file) {
		file.position(0)
		val s7f = SevenZFile.Builder().setSeekableByteChannel(file).get()
		s7f.let {
			var itr = s7f.nextEntry
			var entriesSinceYield = 0
			while (itr != null) {
				if (itr.name == entryName) {
					require(entrySize in 0..Int.MAX_VALUE.toLong()) {
						"Invalid XPK entry size: $entryName ($entrySize bytes)"
					}
					val size = entrySize.toInt()
					val content = ByteArray(size)
					var offset = 0
					while (offset < size) {
						val result = s7f.read(content, offset, minOf(IO_CHUNK_SIZE, size - offset))
						if (result == -1) {
							break
						}
						offset += result
						Thread.yield()
					}
					check(offset == size) { "Unexpected end of XPK entry $entryName at $offset/$size" }
					return content
				}
				itr = s7f.nextEntry
				if (++entriesSinceYield >= ENTRIES_PER_YIELD) {
					entriesSinceYield = 0
					Thread.yield()
				}
			}
		}
		return null
	}

	private const val IO_CHUNK_SIZE = 64 * 1024
	private const val ENTRIES_PER_YIELD = 16
}
