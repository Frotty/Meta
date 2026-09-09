package de.fatox.meta.sound

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.injection.MetaInject.Companion.global
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MetaMusicPlayerTest {
	@AfterEach
	fun tearDown() {
		global(clear = true) {}
	}

	@Test
	fun `adding tracks to a pool does not load them`() {
		val provider = RecordingAssetProvider()
		global(clear = true) {
			singleton<AssetProvider> { provider }
		}

		MetaMusicPlayer().addMusicToPool("music/menu.mp3")

		assertEquals(0, provider.resourceRequests)
	}

	private class RecordingAssetProvider : AssetProvider {
		var resourceRequests = 0

		override fun loadPackedAssetsFromFolder(folder: FileHandle) = false
		override fun loadRawAssetsFromFolder(folder: FileHandle) = false
		override fun <T : Any> load(name: String, type: Class<T>) = Unit
		override fun <T : Any> getResource(fileName: String, type: Class<T>, index: Int): T {
			resourceRequests++
			error("No resource should be requested while adding a pooled track")
		}
		override fun getDrawable(name: String): Drawable = error("unused")
		override fun finish() = Unit
		override fun loadAnimationFrames(baseName: String, frames: Int): Array<out TextureRegion> = Array()
	}
}
