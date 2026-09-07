package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.api.crypto.HASH_LENGTH
import de.fatox.meta.api.crypto.hash
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class XPKLoaderTest {
	@Test
	fun `cooperative loader preserves existing XPK format`() {
		withArchive(mapOf("textures/test.bin" to ByteArray(150_000) { (it * 31).toByte() })) { file, contents ->
			val expected = contents.getValue("textures/test.bin")
			val archive = XPKLoader.open(file)
			try {
				val entries = archive.entries
				assertEquals(1, entries.size)
				assertEquals("textures/test.bin", entries[0].path())
				assertEquals("test.bin", entries[0].name())
				assertEquals(expected.size.toLong(), entries[0].length())
				assertContentEquals(expected, entries[0].readBytes())
				assertSame(entries[0], entries[0].parent().child("TEST.BIN"))
			} finally {
				archive.dispose()
			}
			assertEquals(listOf("textures/test.bin"), XPKLoader.listEntryNames(file).toList())
		}
	}

	/**
	 * A missing child used to come back carrying the *calling* entry's name and size, so it reported
	 * `exists() == true` and read that other entry's bytes. Atlas page resolution goes through this path.
	 */
	@Test
	fun `missing child does not alias another entry`() {
		withArchive(
			mapOf(
				"ui/atlas.txt" to "atlas text".toByteArray(),
				"ui/page.png" to ByteArray(64) { 7 },
			),
		) { file, _ ->
			val archive = XPKLoader.open(file)
			try {
				val atlas = archive.entries.first { it.path() == "ui/atlas.txt" }
				val directory = atlas.parent()
				assertTrue(directory.exists(), "an indexed directory prefix should exist")
				assertTrue(directory.isDirectory)

				val missing = directory.child("nope.png")
				assertFalse(missing.exists(), "a child that is not in the archive must not exist")
				assertEquals(0L, missing.length())
				assertEquals("ui/nope.png", missing.path())
				runCatching { missing.readBytes() }
					.onSuccess { fail("reading a missing entry returned ${it.size} bytes") }

				// The real sibling still resolves, case-insensitively, to the indexed handle.
				assertSame(
					archive.entries.first { it.path() == "ui/page.png" },
					atlas.sibling("PAGE.PNG"),
				)
			} finally {
				archive.dispose()
			}
		}
	}

	/** Reads happen on AssetManager's worker and on the GL thread; the memoised array must be safely published. */
	@Test
	fun `concurrent reads of the same entry return identical content`() {
		val contents = (0 until 24).associate { index ->
			"data/entry$index.bin" to ByteArray(8_000 + index * 97) { (index * 13 + it).toByte() }
		}
		withArchive(contents) { file, expected ->
			val archive = XPKLoader.open(file)
			val pool = Executors.newFixedThreadPool(8)
			try {
				val entries = archive.entries
				val tasks = ArrayList<Callable<Unit>>()
				// Reverse order on purpose: it is the worst case for a forward-sweeping reader.
				for (index in entries.size - 1 downTo 0) {
					val handle = entries[index]
					repeat(3) {
						tasks.add(
							Callable {
								assertContentEquals(expected.getValue(handle.path()), handle.readBytes())
							},
						)
					}
				}
				pool.invokeAll(tasks).forEach { it.get() }
			} finally {
				pool.shutdownNow()
				archive.dispose()
			}
		}
	}

	@Test
	fun `entry paths use forward slashes and survive an archive with directory entries`() {
		withArchive(
			mapOf("a/b/c.bin" to ByteArray(32) { 3 }),
			directories = listOf("a", "a/b"),
		) { file, _ ->
			val archive = XPKLoader.open(file)
			try {
				assertEquals(1, archive.entries.size, "directory entries must not be indexed as files")
				val entry = archive.entries[0]
				assertEquals("a/b/c.bin", entry.path())
				assertTrue(entry.parent().isDirectory)
				assertTrue(entry.parent().parent().exists())

				// libGDX's default list() consults a File that does not exist, so it returned nothing here.
				assertEquals(listOf("a/b/c.bin"), entry.parent().list().map { it.path() })
				assertEquals(listOf("a/b"), entry.parent().parent().list().map { it.path() })
				assertEquals(emptyList(), entry.list().map { it.path() }, "a file entry has no children")
			} finally {
				archive.dispose()
			}
		}
	}

	/** Builds a real XPK: a 7z archive with the signature scrambled and an XXH64 trailer appended. */
	private fun withArchive(
		contents: Map<String, ByteArray>,
		directories: List<String> = emptyList(),
		block: (FileHandle, Map<String, ByteArray>) -> Unit,
	) {
		val temporaryFile: File = Files.createTempFile("meta-xpk-loader", ".xpk").toFile()
		try {
			SevenZOutputFile(temporaryFile).use { output ->
				for (path in directories) {
					output.putArchiveEntry(SevenZArchiveEntry().apply { name = path; isDirectory = true })
					output.closeArchiveEntry()
				}
				for ((path, bytes) in contents) {
					output.putArchiveEntry(
						SevenZArchiveEntry().apply { name = path; size = bytes.size.toLong() },
					)
					output.write(bytes)
					output.closeArchiveEntry()
				}
			}

			val archiveBytes = temporaryFile.readBytes()
			for (index in 0 until 6) archiveBytes[index] = (index * 19 + 7).toByte()
			val xpk = archiveBytes.copyOf(archiveBytes.size + HASH_LENGTH)
			ByteBuffer.wrap(xpk)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putLong(archiveBytes.size, archiveBytes.hash().value.toLong())
			temporaryFile.writeBytes(xpk)

			block(FileHandle(temporaryFile), contents)
		} finally {
			temporaryFile.delete()
		}
	}
}
