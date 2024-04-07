package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.crypto.checkHash
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile

object XPKLoader {
	const val EXTENSION: String = "xpk"

	fun getList(fileHandle: FileHandle): Array<XPKFileHandle> {
		val fileBytes = fileHandle.file().readBytes()

		checkHash(fileBytes)

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
			do {
				val xpkFileHandle = XPKFileHandle(array, 0, byteChannel, archive, archive.name.replace("/", "\\"))
				array.add(xpkFileHandle)
				archive = it.nextEntry
			} while (archive != null)
		}
		return array
	}

	fun loadEntry(file: XPKByteChannel, entry: SevenZArchiveEntry): ByteArray? {
		file.position(0)
		val s7f = SevenZFile.Builder().setSeekableByteChannel(file).get()
		s7f.let {
			var itr = s7f.nextEntry
			do {
				if (itr.name == entry.name) {
					val size = entry.size.toInt()
					val content = ByteArray(size)
					var offset = s7f.read(content)
					while (offset != -1) {
						val result = s7f.read(content, offset, size)
						if (result == -1) {
							break
						}
						offset += result
					}
					return content
				}
				itr = s7f.nextEntry
			} while (itr != null)
		}
		return null
	}
}
