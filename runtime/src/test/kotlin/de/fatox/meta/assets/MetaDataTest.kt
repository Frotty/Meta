package de.fatox.meta.assets

import com.badlogic.gdx.files.FileHandle
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.test.GdxTestEnvironment
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
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
			assertEquals("brutal", metaData.load(key, TestSettings::class)?.difficulty, "should read back before delete")

			assertTrue(metaData.getCachedHandle(key).delete(), "test could not delete the file")

			assertNull(metaData.load(key, TestSettings::class), "a deleted value must not come back from the cache")
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
			assertEquals("brutal", reopened.load(key, TestSettings::class)?.difficulty, "a valid save read as missing")
			assertTrue(reopened.has(key), "has must agree with load")
		}
	}

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
