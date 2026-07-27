package de.fatox.meta.assets

import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.graphics.Pixmap
import de.fatox.meta.test.GdxTestEnvironment
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetaAssetProviderTest {
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

	companion object {
		private const val UPDATE_BUDGET_MS = 1
		private const val MAX_UPDATE_ATTEMPTS = 1_000

		@JvmStatic
		@BeforeAll
		fun initializeGdx() = GdxTestEnvironment.ensure()
	}
}
