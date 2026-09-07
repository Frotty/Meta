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

				// The archive root is a directory even though no entry path names it.
				val root = entry.parent().parent().parent()
				assertEquals("", root.path())
				assertTrue(root.exists(), "the archive root always exists")
				assertTrue(root.isDirectory)
				assertEquals(listOf("a"), root.list().map { it.path() })
			} finally {
				archive.dispose()
			}
		}
	}

	/**
	 * Entry bytes used to be retained for the process lifetime, so a loaded game held a second full copy of its
	 * asset data in heap. Releasing must free them and leave the archive usable.
	 */
	@Test
	fun `releasing cached entries frees buffers and the archive still reads`() {
		val contents = (0 until 6).associate { index ->
			"data/entry$index.bin" to ByteArray(4_000 + index) { (index * 17 + it).toByte() }
		}
		withArchive(contents) { file, expected ->
			val archive = XPKLoader.open(file)
			try {
				assertTrue(archive.isFullyReleased, "a freshly opened archive retains nothing")

				val entries = archive.entries
				for (index in 0 until entries.size) entries[index].readBytes()
				assertFalse(archive.isFullyReleased, "reads must retain something to release")

				archive.releaseCachedEntries()
				assertTrue(archive.isFullyReleased, "release must drop every buffer and close the reader")

				// A later read re-opens and sweeps again rather than failing.
				for (index in 0 until entries.size) {
					val handle = entries[index]
					assertContentEquals(expected.getValue(handle.path()), handle.readBytes())
				}

				archive.releaseCachedEntries()
				archive.releaseCachedEntries()
				assertTrue(archive.isFullyReleased, "release is idempotent")
			} finally {
				archive.dispose()
			}
		}
	}

	/**
	 * The budget check counted only bytes already cached, so an entry of any size was admitted whenever the budget
	 * was not yet spent - one large video or model could overshoot the bound by its whole payload.
	 */
	@Test
	fun `a pass-through entry larger than the budget is not retained`() {
		val contents = (0 until 4).associate { index ->
			"data/entry$index.bin" to ByteArray(4_000) { (index * 11 + it).toByte() }
		}
		withArchive(contents) { file, expected ->
			// Budget smaller than any single entry: nothing may be retained on the way past.
			val archive = XPKLoader.open(file, passthroughCacheBudget = 1_000L)
			try {
				val last = archive.entries[archive.entries.size - 1]
				assertContentEquals(expected.getValue(last.path()), last.readBytes())
				assertEquals(
					last.length(),
					archive.retainedEntryBytes,
					"only the requested entry may be retained when every pass-through entry exceeds the budget",
				)
			} finally {
				archive.dispose()
			}
		}
	}

	/**
	 * With the budget spent, uncached entries sat behind the cursor, so a descending read rewound the solid stream
	 * for every one of them - reinstating the quadratic behaviour this class exists to remove.
	 */
	@Test
	fun `descending reads stay linear once the pass-through budget is spent`() {
		val contents = (0 until 32).associate { index ->
			"data/entry$index.bin" to ByteArray(2_000) { (index * 7 + it).toByte() }
		}
		withArchive(contents) { file, expected ->
			val archive = XPKLoader.open(file, passthroughCacheBudget = 1_000L)
			try {
				val entries = archive.entries
				for (index in entries.size - 1 downTo 0) {
					val handle = entries[index]
					assertContentEquals(expected.getValue(handle.path()), handle.readBytes())
				}
				// One sweep to serve the first (highest) request, one more once that rewind lifts the budget.
				assertTrue(
					archive.sweepCount <= 2,
					"descending reads rewound ${archive.sweepCount} times over ${entries.size} entries",
				)
			} finally {
				archive.dispose()
			}
		}
	}

	/**
	 * `getList` returns entries without the archive that owns them, so a consumer using it had no way to reach the
	 * deterministic cleanup: the 7z reader and every cached buffer stayed pinned for as long as a handle lived.
	 */
	@Suppress("DEPRECATION")
	@Test
	fun `handles from getList still expose the archive that owns them`() {
		val contents = mapOf("data/only.bin" to ByteArray(2_048) { it.toByte() })
		withArchive(contents) { file, expected ->
			val entries = XPKLoader.getList(file)
			val handle = entries[0]
			assertContentEquals(expected.getValue("data/only.bin"), handle.readBytes())

			val owner = handle.archive
			assertFalse(owner.isFullyReleased, "the read must have retained something")
			owner.releaseCachedEntries()
			assertTrue(owner.isFullyReleased, "a getList consumer can reach the cleanup")

			// Still usable after release, then disposable.
			assertContentEquals(expected.getValue("data/only.bin"), handle.readBytes())
			owner.dispose()
		}
	}

	/**
	 * Locks the public members this rewrite would otherwise have dropped.
	 *
	 * A javap diff of the runtime jar against master found four public symbols removed without deprecation:
	 * `XPKFileHandle.getName()`, `XPKByteChannel.array()`, the parameterless `XPKByteChannel()`, and the
	 * `throws IOException` clauses on the channel's `read`/`write`/`position`. Downstream games resolve this module
	 * from JitPack, so each was a break. Referencing them here means deleting one fails the build rather than a
	 * consumer's.
	 */
	@Suppress("DEPRECATION")
	@Test
	fun `deprecated public members remain callable for downstream compatibility`() {
		val contents = mapOf("ui/skin.json" to ByteArray(512) { it.toByte() })
		withArchive(contents) { file, _ ->
			val archive = XPKLoader.open(file)
			try {
				val handle = archive.entries[0]
				// getName() used to carry the whole path, and still must - name() is the file name now.
				assertEquals("ui/skin.json", handle.name)
				assertEquals("ui/skin.json", handle.path())
				assertEquals("skin.json", handle.name())
			} finally {
				archive.dispose()
			}
		}

		// XPKByteChannel keeps the growable write behaviour it always documented. The loader no longer uses it -
		// it reads through the internal XpkReadOnlyChannel - so restoring the symbols without the behaviour would
		// have turned a link error into a runtime one for anyone using it as the buffer it claimed to be.
		val writable = XPKByteChannel()
		writable.write(java.nio.ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)))
		assertEquals(4L, writable.size())
		assertEquals(listOf<Byte>(1, 2, 3, 4), writable.array().copyOf(4).toList())
		writable.truncate(2L)
		assertEquals(2L, writable.size())

		val backing = ByteArray(HASH_LENGTH + 4) { it.toByte() }
		val channel = XPKByteChannel(backing)
		assertSame(backing, channel.array())
		assertEquals(4L, channel.size(), "size() still excludes the hash trailer")
	}

	/**
	 * An explicitly stored empty directory was recorded for `exists()` but never appeared in its parent's listing,
	 * because listings only walked file entries.
	 */
	@Test
	fun `explicitly stored empty directories appear in listings`() {
		withArchive(
			mapOf("assets/used.bin" to ByteArray(16) { 1 }),
			directories = listOf("assets", "empty", "empty/deeper"),
		) { file, _ ->
			val archive = XPKLoader.open(file)
			try {
				val root = archive.entries[0].parent().parent()
				assertEquals("", root.path())
				assertEquals(listOf("assets", "empty"), root.list().map { it.path() }.sorted())

				val empty = root.child("empty")
				assertTrue(empty.exists())
				assertTrue(empty.isDirectory)
				assertEquals(listOf("empty/deeper"), empty.list().map { it.path() })
				assertEquals(emptyList(), empty.child("deeper").list().map { it.path() })
			} finally {
				archive.dispose()
			}
		}
	}

	/**
	 * 7z flags a directory by attribute, not by spelling, and Commons Compress round-trips a `dir/` name verbatim -
	 * verified against a real archive. A raw trailing slash in the key made `child("empty")` miss and hid the entry
	 * from its parent's listing. A stored `also/nested/` must also register `also`, which the archive never names.
	 */
	@Test
	fun `directory entries spelled with a trailing slash index the same as without`() {
		withArchive(
			mapOf("assets/used.bin" to ByteArray(16) { 2 }),
			directories = listOf("empty/", "also/nested/", "/leading/", "dots/./inner/", "terminal/."),
		) { file, _ ->
			val archive = XPKLoader.open(file)
			try {
				val root = archive.entries[0].parent().parent()
				assertEquals(
					listOf("also", "assets", "dots", "empty", "leading", "terminal"),
					root.list().map { it.path() }.sorted(),
				)
				for (name in listOf("empty", "also", "leading", "dots", "terminal")) {
					val directory = root.child(name)
					assertTrue(directory.exists(), "$name should exist")
					assertTrue(directory.isDirectory, "$name should be a directory")
				}
				assertEquals(listOf("also/nested"), root.child("also").list().map { it.path() })
				assertEquals(listOf("dots/inner"), root.child("dots").list().map { it.path() })
				// `terminal/.` must be the directory `terminal`, not a child of it.
				assertEquals(emptyList(), root.child("terminal").list().map { it.path() })
				// A trailing slash on the lookup side resolves to the same entry.
				assertSame(archive.entries[0], root.child("assets/").child("used.bin"))
			} finally {
				archive.dispose()
			}
		}
	}

	/**
	 * The property that owns path normalisation, rather than a list of spellings someone remembered.
	 *
	 * The fast-path guard in `normalisedPath` was twice wrong by omission - it enumerated substrings to look for and
	 * missed first a trailing separator and then a terminal `/.`, returning a non-canonical string unchanged. Asserting
	 * that every output *is* canonical catches any spelling that slips past the guard, including ones not listed here.
	 */
	@Test
	fun `normalisedPath always returns a canonical path`() {
		val spellings = listOf(
			"", ".", "/", "//", "./", "/.", "a", "a/b", "a/b/c.bin",
			"a/", "/a", "/a/", "a//b", "a/./b", "./a", "a/.", "a/./", "a//./b//",
			"///", "./.", "a/././b", "dir\\file.bin", "\\a\\b\\", "a/../b",
		)
		for (spelling in spellings) {
			val result = normalisedPath(spelling)
			assertFalse(result.startsWith("/"), "'$spelling' -> '$result' keeps a leading separator")
			assertFalse(result.endsWith("/"), "'$spelling' -> '$result' keeps a trailing separator")
			assertFalse(result.contains("//"), "'$spelling' -> '$result' keeps an empty segment")
			assertFalse(result.contains("\\"), "'$spelling' -> '$result' keeps a backslash")
			for (segment in result.split('/')) {
				assertTrue(segment != ".", "'$spelling' -> '$result' keeps a dot segment")
				assertTrue(segment.isNotEmpty() || result.isEmpty(), "'$spelling' -> '$result' keeps an empty segment")
			}
			assertEquals(result, normalisedPath(result), "'$spelling' -> '$result' is not a fixed point")
		}
		// `..` is left intact on purpose: entries resolve through a map, never the filesystem.
		assertEquals("a/../b", normalisedPath("a/../b"))
	}

	/**
	 * A read outside a load phase must clean up after itself.
	 *
	 * Nothing pumps `MetaAssetProvider.update` once the splash finishes - `SplashScreen.kt:600` is its only call site
	 * - so a lazily constructed asset, such as `MetaSoundSource.sound`, would otherwise pin its payload and the open
	 * 7z decoder for the rest of the session. Tying cleanup to that pump meant enumerating every path that reaches a
	 * read; the archive now decides per read instead.
	 */
	@Test
	fun `reads outside a load phase release immediately, reads inside it retain`() {
		val contents = (0 until 4).associate { index ->
			"sfx/hit$index.ogg" to ByteArray(1_024) { (index * 5 + it).toByte() }
		}
		withArchive(contents) { file, expected ->
			var loading = true
			val archive = XPKLoader.open(file, retainAfterRead = { loading })
			try {
				val entries = archive.entries
				entries[0].readBytes()
				assertFalse(archive.isFullyReleased, "a read during a load phase should retain")

				archive.releaseCachedEntries()
				loading = false

				// The gameplay-time read still returns the right bytes, and leaves nothing behind.
				val handle = entries[entries.size - 1]
				assertContentEquals(expected.getValue(handle.path()), handle.readBytes())
				assertTrue(
					archive.isFullyReleased,
					"a read outside a load phase must not retain buffers or the 7z reader",
				)
				assertEquals(0L, archive.retainedEntryBytes)

				// Repeatable: each subsequent read is self-contained.
				assertContentEquals(expected.getValue(entries[1].path()), entries[1].readBytes())
				assertTrue(archive.isFullyReleased)
			} finally {
				archive.dispose()
			}
		}
	}

	/**
	 * The legacy enumeration created a handle for every archive record, directories included, and `listEntryNames`
	 * projected all of them. `XpkArchive.entries` is deliberately file-only, so sourcing the deprecated APIs from it
	 * would silently drop directory records for archive tooling.
	 */
	@Suppress("DEPRECATION")
	@Test
	fun `legacy enumeration still reports directory records`() {
		withArchive(
			mapOf("pack/a.bin" to ByteArray(8) { 1 }),
			directories = listOf("pack", "spare"),
		) { file, _ ->
			assertEquals(
				listOf("pack", "pack/a.bin", "spare"),
				XPKLoader.listEntryNames(file).toList().sorted(),
			)

			val legacy = XPKLoader.getList(file)
			try {
				assertEquals(3, legacy.size, "getList enumerated every record before this rewrite")
				val directory = (0 until legacy.size).map { legacy[it] }.first { it.path() == "spare" }
				assertTrue(directory.isDirectory)
				assertEquals(0L, directory.length())
				// Reading a directory record produced an empty array, not a failure.
				assertEquals(0, directory.readBytes().size)

				// The file-only surface is unchanged.
				assertEquals(listOf("pack/a.bin"), legacy[0].archive.entries.let { e -> (0 until e.size).map { e[it].path() } })
			} finally {
				legacy[0].archive.dispose()
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
