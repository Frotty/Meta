package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.crypto.HashUtils
import de.fatox.meta.api.crypto.HashUtils.hex
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.file.Files


object XPKLoader {
    const val EXTENSION = "xpk"
    const val HASH_LENGTH = 20

    fun getList(fileHandle: FileHandle): Array<XPKFileHandle> {
        val array = Array<XPKFileHandle>()
		Files.readAllBytes(fileHandle.file().toPath()).let {
			val input1 = ByteArrayInputStream(it, 0, it.size - HASH_LENGTH)
			val dataChannel = Channels.newChannel(input1)
			val input2 = ByteArrayInputStream(it, it.size - HASH_LENGTH, HASH_LENGTH)
			val hashChannel = Channels.newChannel(input2)
			val buffer = ByteBuffer.allocate(HASH_LENGTH)
			hashChannel.read(buffer)
			val hashBytes : ByteArray = buffer.array()

            val dataHashBytes = HashUtils.computeSha1(dataChannel)

			dataChannel.close()
			input1.close()

			hashChannel.close()
			input2.close()

            if (!hashBytes.contentEquals(dataHashBytes)) {
                throw RuntimeException("game files invalid. expected: " + hex(hashBytes) + " actual: " + hex(dataHashBytes))
            }
			it[0] = '7'.toByte()
			it[1] = 'z'.toByte()
			it[2] = 0xBC.toByte()
			it[3] = 0xAF.toByte()
			it[4] = 0x27.toByte()
			it[5] = 0x1C.toByte()

			val byteChannel = XPKByteChannel(it)
			val sevenZFile = SevenZFile(byteChannel)
			sevenZFile.let {
				var archive = sevenZFile.nextEntry
				do {
					val xpkFileHandle = XPKFileHandle(array, 0, byteChannel, archive, archive.name.replace("/", "\\"))
					array.add(xpkFileHandle)
					archive = sevenZFile.nextEntry
				} while (archive != null)
			}
            return array
        }

    }

    fun loadEntry(file: XPKByteChannel, entry: SevenZArchiveEntry): ByteArray? {
		file.position(0)
		val s7f = SevenZFile(file)
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
