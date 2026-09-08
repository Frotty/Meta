package de.fatox.meta.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.audioVideoDataKey
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.input.MetaUiInputBindings
import de.fatox.meta.input.loadProfile
import de.fatox.meta.test.GdxTestEnvironment
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** A settings-shaped value: mutable, no-arg constructible, exactly what libGDX Json round-trips. */
class TestSettings {
	var masterVolume: Float = 0.5f
	var difficulty: String = "normal"
	var invertY: Boolean = false
}

class MetaDataTest {
	/**
	 * The failure a player actually hits: the game is killed while settings are being written, so the file on disk is
	 * a partial one, and the next launch has to decide what to do with it.
	 *
	 * Overwriting it with defaults - which is what a "recover by writing a fresh instance" path does - destroys the
	 * only copy of their settings and leaves nothing to diagnose. Whatever is returned to the caller, the bytes must
	 * survive somewhere.
	 */
	@Test
	fun `a corrupt settings file is preserved, not overwritten with defaults`() {
		withMetaData { metaData, root ->
			val key = MetaDataKey<TestSettings>("settings.json")
			metaData.save(key, TestSettings().apply { masterVolume = 0.9f; difficulty = "brutal" })

			// Truncated mid-write, the way a kill during save leaves it.
			val file = metaData.getCachedHandle(key)
			val original = file.readString()
			file.writeString(original.substring(0, original.length / 2), false)
			val damaged = file.readString()

			// A fresh MetaData, as a relaunch would have: no warm cache.
			val reopened = newMetaData(root)
            val recovered = reopened.get(key, TestSettings::class)
			assertEquals(0.5f, recovered.masterVolume, "a damaged file should read as defaults")

			val survivors = root.file().walkTopDown()
				.filter { it.isFile && it.readText() == damaged }
				.toList()
			assertTrue(
				survivors.isNotEmpty(),
				"the damaged bytes were destroyed; nothing under ${root.path()} still holds them",
			)
		}
	}

	@Test
	fun `values round-trip`() {
		withMetaData { metaData, _ ->
			val key = MetaDataKey<TestSettings>("round-trip.json")
			metaData.save(key, TestSettings().apply { masterVolume = 0.25f; difficulty = "easy"; invertY = true })

			val loaded = newMetaData(metaData.dataRoot).get(key, TestSettings::class)
			assertEquals(0.25f, loaded.masterVolume)
			assertEquals("easy", loaded.difficulty)
			assertEquals(true, loaded.invertY)
		}
	}

	/** Reading is not a mutation. A game that only ever asks for settings should not litter the save directory. */
	@Test
	fun `reading a key that was never saved does not create a file`() {
		withMetaData { metaData, root ->
			val key = MetaDataKey<TestSettings>("never-saved.json")
			val value = metaData.get(key, TestSettings::class)
			assertEquals(0.5f, value.masterVolume)

			val created = root.file().walkTopDown().filter { it.isFile }.toList()
			assertTrue(created.isEmpty(), "reading created ${created.map { it.name }}")
		}
	}

	/** A half-written temp file left behind is the next launch's corrupt settings. */
	@Test
	fun `saving leaves no scratch files behind`() {
		withMetaData { metaData, root ->
			val key = MetaDataKey<TestSettings>("clean.json")
			repeat(3) { index -> metaData.save(key, TestSettings().apply { masterVolume = index / 10f }) }

			val stray = root.file().walkTopDown()
				.filter { it.isFile && it.name != "clean.json" }
				.toList()
			assertTrue(stray.isEmpty(), "left behind ${stray.map { it.name }}")
		}
	}

	/**
	 * `AGENTS.md` requires persisted keys to stay stable, but nothing enforced it - a renamed field deserialises to
	 * its default and the old value is dropped on the next save, silently. Pinning the wire names makes a rename a
	 * failing build rather than a player's lost settings, and would also catch an obfuscator renaming them.
	 */
	@Test
	fun `persisted field names are pinned`() {
		withMetaData { metaData, _ ->
			val key = MetaDataKey<TestSettings>("names.json")
			// Values must differ from the defaults, or nothing is written at all - see the elision test below.
			metaData.save(key, TestSettings().apply { masterVolume = 0.31f; difficulty = "hard"; invertY = true })
			val json = metaData.getCachedHandle(key).readString()
			for (name in listOf("masterVolume", "difficulty", "invertY")) {
				assertTrue(json.contains("\"$name\""), "'$name' is missing from $json")
			}
			assertTrue(json.startsWith("{\""), "output should be standard JSON with quoted names, was $json")
		}
	}

	/**
	 * libGDX `Json` writes prototype-relative: a field equal to the value a default-constructed instance would have
	 * is omitted entirely, so saving an all-defaults object produces `{}`.
	 *
	 * That keeps files small and lets a new field pick up its default on an old save, but it has a consequence worth
	 * pinning: **changing a default silently changes what existing saves mean**, because the old value was never
	 * written down. A player who deliberately chose what is now the old default will find their setting moved.
	 */
	@Test
	fun `values equal to the default are not written`() {
		withMetaData { metaData, _ ->
			val allDefaults = MetaDataKey<TestSettings>("defaults.json")
			metaData.save(allDefaults, TestSettings())
			assertEquals("{}", metaData.getCachedHandle(allDefaults).readString())

			val oneChanged = MetaDataKey<TestSettings>("one-changed.json")
			metaData.save(oneChanged, TestSettings().apply { difficulty = "hard" })
			val json = metaData.getCachedHandle(oneChanged).readString()
			assertTrue(json.contains("difficulty"), json)
			assertTrue(!json.contains("masterVolume"), "an unchanged field should be omitted, was $json")
		}
	}
	/**
	 * The cache mirrors the file; it does not outlive it. `load` consulted the cache first, and a deleted file reports
	 * a modification time of zero, so any cache entry looked newer and won - `MetaProjectManager.get` went on handing
	 * out a removed project's metadata instead of reporting it missing.
	 */
	@Test
	fun `a deleted file is not served from the cache`() {
		withMetaData { metaData, _ ->
			val key = MetaDataKey<TestSettings>("deleted.json")
			metaData.save(key, TestSettings().apply { difficulty = "brutal" })
			assertEquals("brutal", metaData.stored(key)?.difficulty, "should read back before delete")

			assertTrue(metaData.getCachedHandle(key).delete(), "test could not delete the file")

			assertEquals(
				MetaData.StoredValue.Absent,
				metaData.read(key, TestSettings::class, cached = false),
				"a deleted value must not come back from the cache",
			)
			assertEquals(false, metaData.has(key), "has must agree with load")
		}
	}

	/** `has` answers "is a value stored", not "has this key been looked up before". */
	@Test
	fun `a key that was only read is not reported as present`() {
		withMetaData { metaData, _ ->
			val key = MetaDataKey<TestSettings>("looked-up.json")
			metaData.get(key, TestSettings::class)
			assertEquals(false, metaData.has(key), "reading a missing key must not make it exist")

			metaData.save(key, TestSettings().apply { difficulty = "hard" })
			assertTrue(metaData.has(key), "a saved key must be reported as present")
		}
	}

	/**
	 * `File.createTempFile` rejects a prefix shorter than three characters, so routing saves through a scratch file
	 * turned short keys - which `MetaProjectManager.save` forwards verbatim - into an `IllegalArgumentException`.
	 */
	@Test
	fun `a short key saves`() {
		withMetaData { metaData, _ ->
			for (name in listOf("a", "ab", "abc")) {
				val key = MetaDataKey<TestSettings>(name)
				metaData.save(key, TestSettings().apply { difficulty = name })
				assertEquals(name, newMetaData(metaData.dataRoot).get(key, TestSettings::class).difficulty)
			}
		}
	}

	/**
	 * A value byte-identical to the stored one skips the write, which is worth doing - but the cache still has to
	 * adopt the instance that was handed over, or the next read returns the older object the caller has stopped using.
	 */
	@Test
	fun `saving an unchanged value still adopts the caller's instance`() {
		withMetaData { metaData, _ ->
			val key = MetaDataKey<TestSettings>("adopted.json")
			metaData.save(key, TestSettings().apply { difficulty = "hard" })

			val resaved = TestSettings().apply { difficulty = "hard" }
			metaData.save(key, resaved)

			assertSame(resaved, metaData.get(key, TestSettings::class), "the cache kept the superseded instance")
		}
	}

	/**
	 * A real file dated to the epoch - restored from an archive that carried no timestamps, say - must not read as
	 * missing. `File.lastModified` already returns zero for a file that is not there, so sharing that value as an
	 * "absent" sentinel answers with defaults over a perfectly good save, and the next write then overwrites it.
	 */
	@Test
	fun `a file dated to the epoch is still read`() {
		withMetaData { metaData, root ->
			val key = MetaDataKey<TestSettings>("epoch.json")
			metaData.save(key, TestSettings().apply { difficulty = "brutal" })

			val file = metaData.getCachedHandle(key).file()
			assumeTrue(file.setLastModified(0L) && file.lastModified() == 0L, "filesystem keeps no epoch timestamps")

			val reopened = newMetaData(root)
			assertEquals("brutal", reopened.stored(key)?.difficulty, "a valid save read as missing")
			assertTrue(reopened.has(key), "has must agree with load")
		}
	}

	/**
	 * Publishing by rename means the file that lands is a new one, so a mode the old file had is lost unless it is
	 * carried across - `Files.createTempFile` creates owner-only. Project metadata a collaborator could read would
	 * stop being readable to them on the next save.
	 *
	 * POSIX-only, so this runs on CI (Linux) and is skipped on a Windows workstation.
	 */
	@Test
	fun `replacing a save keeps the permissions of the file it replaces`() {
		withMetaData { metaData, _ ->
			val key = MetaDataKey<TestSettings>("permissions.json")
			metaData.save(key, TestSettings().apply { difficulty = "hard" })
			val path = metaData.getCachedHandle(key).file().toPath()

			val readableByAll = setOf(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.OTHERS_READ,
			)
			try {
				Files.setPosixFilePermissions(path, readableByAll)
			} catch (_: UnsupportedOperationException) {
				assumeTrue(false, "not a POSIX filesystem")
			}

			metaData.save(key, TestSettings().apply { difficulty = "brutal" })

			assertEquals(readableByAll, Files.getPosixFilePermissions(path), "the replacement narrowed the mode")

			// A mode without owner-write is the case that proves the ordering is safe: permissions are applied to the
			// scratch file before it is written, so the write only succeeds through the handle that created it.
			val readOnly = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ)
			Files.setPosixFilePermissions(path, readOnly)
			metaData.save(key, TestSettings().apply { difficulty = "brutal"; invertY = true })
			assertEquals(readOnly, Files.getPosixFilePermissions(path), "a read-only mode was not carried")
			assertEquals(true, newMetaData(metaData.dataRoot).get(key, TestSettings::class).invertY)
			assertEquals("brutal", newMetaData(metaData.dataRoot).get(key, TestSettings::class).difficulty)
		}
	}

	/**
	 * A file this creates should look like any other file written into that directory. Requesting the scratch file
	 * from `Files.createTempFile` made it owner-only on POSIX, so the first save of a project's metadata landed
	 * private regardless of the directory's umask or group policy - `MetaProjectManager.save` writes into a project
	 * root, not the player's private data root.
	 *
	 * POSIX-only, so this runs on CI (Linux) and is skipped on a Windows workstation.
	 */
	@Test
	fun `a newly created save is not narrower than a plain write`() {
		withMetaData { metaData, root ->
			val reference = root.file().toPath().resolve("reference.probe")
			val expected = try {
				Files.newOutputStream(reference).use { it.write(1) }
				Files.getPosixFilePermissions(reference)
			} catch (_: UnsupportedOperationException) {
				assumeTrue(false, "not a POSIX filesystem")
				return@withMetaData
			}

			val key = MetaDataKey<TestSettings>("created.json")
			metaData.save(key, TestSettings().apply { difficulty = "hard" })

			assertEquals(
				expected,
				Files.getPosixFilePermissions(metaData.getCachedHandle(key).file().toPath()),
				"a created save should match what an ordinary write into the same directory produces",
			)
		}
	}

	/**
	 * `load` reparses; only `get` caches. That split predates this change and has to survive it: project metadata
	 * lives in a directory a person also edits, and a replacement whose modification time does not advance - a
	 * restored backup, or two writes inside one filesystem tick - would otherwise pin the stale object forever.
	 */
	@Test
	fun `load sees a replacement whose timestamp did not advance`() {
		withMetaData { metaData, _ ->
			val key = MetaDataKey<TestSettings>("external.json")
			metaData.save(key, TestSettings().apply { difficulty = "hard" })
			assertEquals("hard", metaData.stored(key)?.difficulty)

			// Replaced behind this instance's back, keeping the timestamp it already had.
			val file = metaData.getCachedHandle(key)
			val stamp = file.lastModified()
			file.writeString("""{"difficulty":"brutal"}""", false)
			assumeTrue(file.file().setLastModified(stamp), "filesystem will not restore a timestamp")

			assertEquals(
				"brutal",
				metaData.stored(key)?.difficulty,
				"load must reparse rather than answer from the cache",
			)
		}
	}

	/**
	 * The staging file must be one this call made. A predictable `<target>.tmp` opened with `TRUNCATE_EXISTING`
	 * destroys an unrelated sibling of that name - reachable through `MetaProjectManager.save`, which writes into a
	 * project directory rather than a private data root.
	 */
	@Test
	fun `saving does not destroy an unrelated sibling named like the scratch file`() {
		withMetaData { metaData, root ->
			val key = MetaDataKey<TestSettings>("notes.json")
			val bystander = root.child("notes.json.tmp")
			bystander.writeString("someone else's file", false)

			metaData.save(key, TestSettings().apply { difficulty = "hard" })

			assertEquals("someone else's file", bystander.readString(), "the save overwrote an unrelated file")
		}
	}

	/**
	 * A save target that is a symlink - settings redirected into a synchronised folder - must have its destination
	 * updated. Renaming over the path replaces the link itself, silently dismantling the redirection; the plain write
	 * this replaced followed it.
	 *
	 * Symlink creation needs privileges Windows does not grant by default, so this runs on CI (Linux).
	 */
	@Test
	fun `saving through a symlink updates its destination`() {
		withMetaData { metaData, root ->
			val destination = root.child("elsewhere").also { it.parent().mkdirs() }
			destination.writeString("{}", false)
			val link = root.file().toPath().resolve("linked.json")
			try {
				Files.createSymbolicLink(link, destination.file().toPath())
			} catch (_: Exception) {
				assumeTrue(false, "cannot create symbolic links here")
				return@withMetaData
			}

			metaData.save(MetaDataKey<TestSettings>("linked.json"), TestSettings().apply { difficulty = "brutal" })

			assertTrue(Files.isSymbolicLink(link), "the link was replaced by a regular file")
			assertTrue(destination.readString().contains("brutal"), "the destination was not updated")
		}
	}

	/**
	 * Quarantine is for bytes that are not a value of this type. A file that simply could not be read says nothing
	 * about its contents, and moving it aside there renames a perfectly good save out of the way.
	 *
	 * It must also not be reported as absent. Absent means "writing here loses nothing", and this file is intact - it
	 * is the reading that failed - so a caller told `Absent` would overwrite data that was never in any trouble.
	 */
	@Test
	fun `a file that cannot be read is neither quarantined nor reported absent`() {
		withMetaData { metaData, root ->
			val key = MetaDataKey<TestSettings>("unreadable.json")
			metaData.save(key, TestSettings().apply { difficulty = "brutal" })
			val file = metaData.getCachedHandle(key).file()

			// A directory in the file's place: present and stat-able, but every read of it fails.
			assertTrue(file.delete())
			assertTrue(file.mkdir())

			val stored = newMetaData(root).read(key, TestSettings::class, cached = false)
			assertTrue(
				stored is MetaData.StoredValue.Unreadable,
				"an unreadable file must not be reported as absent, was $stored",
			)
			val quarantined = root.file().walkTopDown().filter { it.name.contains(".corrupt") }.toList()
			assertTrue(quarantined.isEmpty(), "moved aside ${quarantined.map { it.name }} without reading it")
		}
	}

	/**
	 * A key long enough to be near a filesystem's 255-byte component limit saves on its own, so it must keep saving.
	 * Deriving the scratch name from the target added up to 21 characters to it, and the sibling was then too long to
	 * create - a key the plain write accepted started failing.
	 */
	@Test
	fun `a key near the filesystem name limit saves`() {
		withMetaData { metaData, _ ->
			val key = MetaDataKey<TestSettings>("k".repeat(240) + ".json")
			metaData.save(key, TestSettings().apply { difficulty = "hard" })
			assertEquals("hard", newMetaData(metaData.dataRoot).get(key, TestSettings::class).difficulty)
		}
	}

	/**
	 * A damaged save with a near-limit name still has to be preserved. Appending `.corrupt` to a 250-character name
	 * overruns the 255-byte component limit, `renameTo` returns false, and the only copy of the damaged bytes is left
	 * to be overwritten by the next save - the one outcome quarantine exists to prevent.
	 */
	@Test
	fun `a damaged file with a near-limit name is still preserved`() {
		withMetaData { metaData, root ->
			val key = MetaDataKey<TestSettings>("q".repeat(245) + ".json")
			metaData.save(key, TestSettings().apply { difficulty = "brutal" })

			val file = metaData.getCachedHandle(key)
			file.writeString("{ truncated", false)
			val damaged = file.readString()

			val reopened = newMetaData(root)
			assertEquals(
				MetaData.StoredValue.Absent,
				reopened.read(key, TestSettings::class, cached = false),
				"damaged bytes that were set aside leave the path free",
			)

			// The loss is not immediate: a quarantine that could not place the file leaves it where it is, and the
			// next save is what overwrites the only copy.
			reopened.save(key, TestSettings().apply { difficulty = "hard" })

			val survivors = root.file().walkTopDown().filter { it.isFile && it.readText() == damaged }.toList()
			assertTrue(survivors.isNotEmpty(), "the damaged bytes were lost; nothing under ${root.path()} holds them")
		}
	}

	/** Window layout is stored under `<screen>/<name>`, so a nested key is ordinary and its path is created on save. */
	@Test
	fun `a nested key saves and reads back`() {
		withMetaData { metaData, root ->
			val key = MetaDataKey<TestSettings>("screens/main/layout.json")
			metaData.save(key, TestSettings().apply { difficulty = "hard" })
			assertEquals("hard", newMetaData(root).get(key, TestSettings::class).difficulty)
		}
	}

	/**
	 * Quarantine names must not run out. A bounded counter leaves the damaged file at its live path once the numbers
	 * are used up, and the caller's usual `load(...) ?: defaults().also { save(it) }` - `MetaUiInputProfilePersistence`
	 * does exactly that - then overwrites the bytes quarantine exists to keep.
	 */
	@Test
	fun `a damaged file is preserved even when many quarantine names are taken`() {
		withMetaData { metaData, root ->
			val key = MetaDataKey<TestSettings>("crowded.json")
			metaData.save(key, TestSettings().apply { difficulty = "brutal" })

			root.child("crowded.json${MetaData.CORRUPT_SUFFIX}").writeString("older", false)
			for (index in 0..40) root.child("crowded.json${MetaData.CORRUPT_SUFFIX}.$index").writeString("older", false)

			val file = metaData.getCachedHandle(key)
			file.writeString("{ truncated", false)
			val damaged = file.readString()

			val reopened = newMetaData(root)
			assertEquals(MetaData.StoredValue.Absent, reopened.read(key, TestSettings::class, cached = false))
			// What the callers do on a null result, and what destroys the evidence if it was never moved aside.
			reopened.save(key, TestSettings().apply { difficulty = "hard" })

			val survivors = root.file().walkTopDown().filter { it.isFile && it.readText() == damaged }.toList()
			assertTrue(survivors.isNotEmpty(), "the damaged bytes were lost once the numbered names were taken")
		}
	}

	/**
	 * `load` answers `null` both for "nothing is stored" and for "this could not be read right now". Any caller that
	 * writes defaults on `null` therefore destroys a good file whenever a read fails transiently - a scanner or a
	 * sync client holding a brief lock. `loadProfile` was the one caller doing it.
	 */
	@Test
	fun `loading an input profile does not write one`() {
		withMetaData { metaData, root ->
			MetaUiInputBindings().loadProfile(metaData)

			val created = root.file().walkTopDown().filter { it.isFile }.toList()
			assertTrue(created.isEmpty(), "loading wrote ${created.map { it.name }}")
		}
	}

	/**
	 * The whole reason the three outcomes exist: read-or-initialise must write for one of them and not the other.
	 * `load(key) ?: defaults().also { save(it) }` could not tell them apart, so a file held for a moment by a scanner
	 * was answered as "nothing here" and then overwritten.
	 */
	@Test
	fun `getOrCreate writes for an absent value but not for an unreadable one`() {
		withMetaData { metaData, root ->
			val fresh = MetaDataKey<TestSettings>("fresh.json")
			assertEquals(0.5f, metaData.getOrCreate(fresh, TestSettings::class).masterVolume)
			assertTrue(metaData.has(fresh), "an absent value should have been created")

			val locked = MetaDataKey<TestSettings>("locked.json")
			metaData.save(locked, TestSettings().apply { difficulty = "brutal" })
			val file = metaData.getCachedHandle(locked).file()
			val original = file.readText()
			// Present and stat-able, every read of it fails: what a lock or a permission blip looks like from here.
			assertTrue(file.delete())
			assertTrue(file.mkdir())

			val reopened = newMetaData(root)
			assertEquals(0.5f, reopened.getOrCreate(locked, TestSettings::class).masterVolume, "should use a default")
			assertTrue(file.isDirectory, "getOrCreate wrote over a value it could not read")

			// And once the obstruction clears, the original is still exactly where it was.
			file.delete()
			file.writeText(original)
			assertEquals("brutal", newMetaData(root).stored(locked)?.difficulty)
		}
	}

	/**
	 * A redirection can be more than one hop, and the last hop is the one that does not exist yet on a first save.
	 * Following only the first link publishes over the intermediary, which replaces half of the chain with a regular
	 * file and leaves the destination the player set up unwritten.
	 *
	 * Symlink creation needs privileges Windows does not grant by default, so this runs on CI (Linux).
	 */
	@Test
	fun `saving through a chain of links reaches the far end`() {
		withMetaData { metaData, root ->
			val directory = root.file().toPath()
			root.mkdirs()
			val destination = directory.resolve("destination.json")
			val intermediary = directory.resolve("intermediary.json")
			val entry = directory.resolve("chained.json")
			try {
				Files.createSymbolicLink(intermediary, destination)
				Files.createSymbolicLink(entry, intermediary)
			} catch (_: Exception) {
				assumeTrue(false, "cannot create symbolic links here")
				return@withMetaData
			}

			metaData.save(MetaDataKey<TestSettings>("chained.json"), TestSettings().apply { difficulty = "brutal" })

			assertTrue(Files.isSymbolicLink(entry), "the entry link was replaced by a regular file")
			assertTrue(Files.isSymbolicLink(intermediary), "the intermediate link was replaced by a regular file")
			assertTrue(Files.exists(destination, java.nio.file.LinkOption.NOFOLLOW_LINKS), "nothing reached the end")
			assertTrue(String(Files.readAllBytes(destination)).contains("brutal"), "the destination was not written")
		}
	}

	/**
	 * A chain that loops has no destination to write. Walking it hits the hop limit, and whichever link the walk
	 * stopped on must not be published over - that would replace part of the loop with a regular file. Refusing is
	 * the only answer that leaves the setup as it was found.
	 *
	 * Symlink-only, so this runs on CI (Linux).
	 */
	@Test
	fun `saving through a looping chain of links is refused`() {
		withMetaData { metaData, root ->
			root.mkdirs()
			val directory = root.file().toPath()
			val first = directory.resolve("loop.json")
			val second = directory.resolve("loop-back.json")
			try {
				Files.createSymbolicLink(first, second)
				Files.createSymbolicLink(second, first)
			} catch (_: Exception) {
				assumeTrue(false, "cannot create symbolic links here")
				return@withMetaData
			}

			assertFailsWith<GdxRuntimeException> {
				metaData.save(MetaDataKey<TestSettings>("loop.json"), TestSettings().apply { difficulty = "brutal" })
			}

			assertTrue(Files.isSymbolicLink(first), "the entry link was replaced")
			assertTrue(Files.isSymbolicLink(second), "the link it points at was replaced")
		}
	}

	/**
	 * Settings that exist but could not be read must not be saved over. Everything in `MetaAudioVideoState` persists
	 * on change and the editor saves on an ordinary window resize, so a momentary read failure at startup otherwise
	 * turns the next resize into a write of defaults over settings that are still on disk and still intact.
	 */
	@Test
	fun `unreadable audio video settings suspend saving over them`() {
		withMetaData { metaData, root ->
			val stored = MetaAudioVideoData(masterVolume = 0.9f, maxFps = 240)
			metaData.save(audioVideoDataKey, stored)
			val file = metaData.getCachedHandle(audioVideoDataKey).file()
			val original = file.readText()

			// Present and stat-able, every read fails: a scanner or a sync client holding it for a moment.
			assertTrue(file.delete())
			assertTrue(file.mkdir())

			MetaInject.global { singleton(newMetaData(root)) }
			MetaAudioVideoState.initialize(newMetaData(root).read(audioVideoDataKey, MetaAudioVideoData::class))
			assertTrue(MetaAudioVideoState.persistenceSuspended, "an unreadable read should suspend persistence")

			// What a window resize does.
			MetaAudioVideoState.update { width = 1280 }
			assertTrue(file.isDirectory, "a save landed on top of settings that could not be read")

			file.delete()
			file.writeText(original)
			assertEquals(240, newMetaData(root).get(audioVideoDataKey, MetaAudioVideoData::class).maxFps)
		}
	}

	/**
	 * A supplied root is the whole root. The legacy layout - a flat `.meta<key>` file in the user's home - is only a
	 * fallback for the root this class picks itself; comparing paths let a supplied root match itself and inherit it,
	 * so an instance pointed at a temporary directory would read, and then save over, a real file in the home folder.
	 *
	 * This does briefly create such a file, because nothing else proves the fallback is skipped. Uniquely named and
	 * removed again.
	 */
	@Test
	fun `a supplied root never falls back to the legacy home-folder layout`() {
		val name = "legacy-probe-${System.nanoTime()}.json"
		val legacy = Gdx.files.external(MetaData.GLOBAL_DATA_FOLDER_NAME + name)
		withMetaData { metaData, root ->
			legacy.writeString("""{"difficulty":"from-the-home-folder"}""", false)
			try {
				val key = MetaDataKey<TestSettings>(name)

				assertEquals(
					MetaData.StoredValue.Absent,
					metaData.read(key, TestSettings::class, cached = false),
					"a supplied root should see nothing, not the home-folder file",
				)

				metaData.save(key, TestSettings().apply { difficulty = "in-the-supplied-root" })
				assertEquals(
					"""{"difficulty":"from-the-home-folder"}""",
					legacy.readString(),
					"the save landed on the home-folder file instead of the supplied root",
				)
				assertTrue(root.child(name).exists(), "the save should have created the value under the supplied root")
			} finally {
				legacy.delete()
			}
		}
	}

	/** The stored value, for the cases where only the value matters. Always uncached, as `load` used to be. */
	private fun MetaData.stored(key: MetaDataKey<TestSettings>): TestSettings? =
		read(key, TestSettings::class, cached = false).valueOrNull

	private fun newMetaData(root: FileHandle): MetaData = MetaData(root)

	private fun withMetaData(block: (MetaData, FileHandle) -> Unit) {
		val directory = Files.createTempDirectory("meta-data-test").toFile()
		val root = FileHandle(directory)
		MetaInject.global { singleton("gameName") { "metadata-test" } }
		try {
			block(newMetaData(root), root)
		} finally {
			MetaInject.global(clear = true) {}
			directory.deleteRecursively()
		}
	}

	companion object {
		@JvmStatic
		@BeforeAll
		fun initializeGdx() = GdxTestEnvironment.ensure()
	}
}
