package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.api.crypto.HASH_LENGTH
import de.fatox.meta.api.crypto.hash
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class XPKLoaderTest {
	@Test
	fun `cooperative loader preserves existing XPK format`() {
		val temporaryFile = Files.createTempFile("meta-xpk-loader", ".xpk").toFile()
		try {
			val expected = ByteArray(150_000) { index -> (index * 31).toByte() }
			SevenZOutputFile(temporaryFile).use { output ->
				val entry = SevenZArchiveEntry().apply {
					name = "textures/test.bin"
					size = expected.size.toLong()
				}
				output.putArchiveEntry(entry)
				output.write(expected)
				output.closeArchiveEntry()
			}

			val archive = temporaryFile.readBytes()
			for (index in 0 until 6) archive[index] = (index * 19 + 7).toByte()
			val xpk = archive.copyOf(archive.size + HASH_LENGTH)
			ByteBuffer.wrap(xpk).order(ByteOrder.LITTLE_ENDIAN).putLong(archive.size, archive.hash().value.toLong())
			temporaryFile.writeBytes(xpk)

			val entries = XPKLoader.getList(FileHandle(temporaryFile))
			assertEquals(1, entries.size)
			assertEquals("textures\\test.bin", entries[0].name())
			assertContentEquals(expected, entries[0].readBytes())
		} finally {
			temporaryFile.delete()
		}
	}
}
