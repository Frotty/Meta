package de.fatox.meta.assets

import com.badlogic.gdx.graphics.Pixmap
import de.fatox.meta.test.GdxTestEnvironment
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetaAssetProviderTest {
	@Test
	fun `queued assets complete through incremental updates`() {
		val provider = MetaAssetProvider()
		provider.load("meta-icon-error.png", Pixmap::class.java)
		assertEquals(0f, provider.progress, "load() must queue rather than finish the asset synchronously")

		var complete = false
		var attempts = 0
		while (!complete && attempts++ < MAX_UPDATE_ATTEMPTS) {
			complete = provider.update(UPDATE_BUDGET_MS)
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
		private const val UPDATE_BUDGET_MS = 10
		private const val MAX_UPDATE_ATTEMPTS = 100

		@JvmStatic
		@BeforeAll
		fun initializeGdx() = GdxTestEnvironment.ensure()
	}
}
