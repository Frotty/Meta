package de.fatox.meta.assets

import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.GdxRuntimeException
import de.fatox.meta.assets.xpk.XpkProfile
import de.fatox.meta.assets.xpk.XpkWriter
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.test.GdxTestEnvironment
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MetaAssetProviderTest {
	/**
	 * A loose file and a packed entry naming the same asset must be reported, not silently ranked.
	 *
	 * `cacheFile` already refuses an overlap between loose files and v1 archives. Resolving loose-versus-v2 with an
	 * `?:` instead would have made one lookup path loud and another quiet about the same mistake, so a stale pack
	 * could serve different bytes depending on which route reached it.
	 */
	@Test
	fun `an asset in both a loose folder and a packed archive is reported`() {
		val root = Files.createTempDirectory("meta-overlap").toFile()
		try {
			val loose = File(root, "assets/data").apply { mkdirs() }
			File(loose, "shared.bin").writeBytes(ByteArray(16) { 1 })

			val profile = XpkProfile.of(
				rootKey = ByteArray(32) { 1 },
				nameHashKey = ByteArray(32) { 2 },
				profileId = 42,
				footerMask = 0x5EED_5EED_5EED_5EEDuL.toLong(),
			)
			MetaInject.global { singleton(profile) }
			try {
				val packs = File(root, "packs").apply { mkdirs() }
				File(packs, "content.xpk").writeBytes(
					XpkWriter(profile).apply { add("data/shared.bin", ByteArray(16) { 2 }) }.build(),
				)

				val provider = MetaAssetProvider()
				assertTrue(provider.loadRawAssetsFromFolder(FileHandle(File(root, "assets"))))
				assertTrue(provider.loadPackedAssetsFromFolder(FileHandle(packs)))

				// Every spelling must reach both sources. While the indexed key folded only slashes and case while
				// the packed one also stripped empty and `.` segments, `./data//shared.bin` missed the index, hit
				// the archive, and returned the packed entry instead of reporting the clash.
				for (spelling in listOf("data/shared.bin", "./data//shared.bin", "Data\\Shared.BIN")) {
					val failure = assertFailsWith<GdxRuntimeException>("spelling '$spelling' should have clashed") {
						provider.getResource(spelling, FileHandle::class.java)
					}
					assertTrue(
						failure.message.orEmpty().contains("packed archive"),
						"the overlap should name both sources, was: ${failure.message}",
					)
				}
				provider.dispose()
			} finally {
				MetaInject.global(clear = true) {}
			}
		} finally {
			root.deleteRecursively()
		}
	}

	@Test
	fun `texture atlas dependency discovery uses an asynchronous loader`() {
		assertTrue(
			AsynchronousAssetLoader::class.java.isAssignableFrom(MetaTextureAtlasLoader::class.java),
			"Atlas parsing and XPK decompression must not run in AssetManager.update on the render thread",
		)
		assertTrue(
			AsynchronousAssetLoader::class.java.isAssignableFrom(MetaTextureLoader::class.java),
			"Texture decoding and mip generation must stay on AssetManager's worker",
		)
	}

	@Test
	fun `queued assets complete through incremental updates`() {
		val provider = MetaAssetProvider()
		Thread { provider.load("meta-icon-error.png", Pixmap::class.java) }.apply {
			start()
			join()
		}
		assertEquals(0f, provider.progress, "load() must queue rather than finish the asset synchronously")
		assertEquals(false, provider.update(0), "A zero budget must poll without advancing AssetManager")
		assertEquals(0f, provider.progress, "A zero budget must not start queued work")

		var complete = false
		var attempts = 0
		while (!complete && attempts++ < MAX_UPDATE_ATTEMPTS) {
			complete = provider.update(UPDATE_BUDGET_MS)
			if (!complete) Thread.sleep(1)
		}

		assertTrue(complete, "Asset queue did not complete")
		assertEquals(1f, provider.progress)
		provider.getResource("meta-icon-error.png", Pixmap::class.java)
		provider.dispose()
	}

	@Test
	fun `lazy retrieval still loads a single unqueued asset`() {
		val provider = MetaAssetProvider()
		provider.getResource("meta-icon-error.png", Pixmap::class.java)
		assertEquals(1f, provider.progress)
		provider.dispose()
	}

	@Test
	fun `raw asset lookup ignores filename case and separator style`() {
		val root = FileHandle.tempDirectory("meta-case-assets")
		val font = root.child("Fonts").child("Oxanium-SemiBold.TTF")
		font.parent().mkdirs()
		font.writeString("test", false)
		val provider = MetaAssetProvider()
		try {
			assertTrue(provider.loadRawAssetsFromFolder(root))
			assertTrue(provider.getResource("fonts\\oxanium-semibold.ttf", FileHandle::class.java).exists())
		} finally {
			provider.dispose()
			root.deleteDirectory()
		}
	}

	@Test
	fun `asset lookup keys normalize separators and case without changing the source path`() {
		assertEquals("fonts/montserrat.ttf", assetPathKey("Fonts\\Montserrat.TTF"))
	}

	companion object {
		private const val UPDATE_BUDGET_MS = 1
		private const val MAX_UPDATE_ATTEMPTS = 1_000

		@JvmStatic
		@BeforeAll
		fun initializeGdx() = GdxTestEnvironment.ensure()
	}
}
