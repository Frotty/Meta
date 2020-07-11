package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.MultiReadOnlySeekableByteChannel
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.util.*


object XPKLoader {
    const val EXTENSION = "xpk"
    private const val HASH_LENGTH = 40

    fun getList(fileHandle: FileHandle): Array<XPKFileHandle> {
        val array = Array<XPKFileHandle>()
		Files.readAllBytes(fileHandle.file().toPath()).let {
			val dataChannel = Channels.newChannel(ByteArrayInputStream(it, 0, it.size - HASH_LENGTH))
			val hashChannel = Channels.newChannel(ByteArrayInputStream(it, it.size - HASH_LENGTH, HASH_LENGTH))
			val buffer = ByteBuffer.allocate(40)
			hashChannel.read(buffer)
			val hashBytes : ByteArray = buffer.array()

            val dataHashBytes = HashUtils.computeSha1(dataChannel)
			dataChannel.close()
			hashChannel.close()
            if (!hashBytes.contentEquals(dataHashBytes)) {
                throw RuntimeException("game files invalid")
            }

			val data2 = Channels.newChannel(ByteArrayInputStream(it, 0, it.size - HASH_LENGTH))
			val sevenZFile = SevenZFile(XPKByteChannel(it))
            var archive = sevenZFile.nextEntry
            do {
                val xpkFileHandle = XPKFileHandle(array, 0, fileHandle, archive, archive.name.replace("/", "\\"))
                array.add(xpkFileHandle)
                archive = sevenZFile.nextEntry
            } while (archive != null)
            sevenZFile.close()
            return array
        }

    }

    fun loadEntry(file: FileHandle, entry: SevenZArchiveEntry): ByteArray? {
        val s7f = SevenZFile(file.file())
        s7f.use {
            var itr = it.nextEntry
            do {
                if (itr.name == entry.name) {
                    val size = itr.size.toInt()
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
                itr = it.nextEntry
            } while (itr != null)
        }
        return null
    }


}
