package de.fatox.meta.sound

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.injection.MetaInject.Companion.global
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

	@Test
	fun `selecting a pooled track queues it and waits for asynchronous completion`() {
		val provider = RecordingAssetProvider()
		global(clear = true) { singleton<AssetProvider> { provider } }
		val player = MetaMusicPlayer()
		player.addMusicToPool("music/menu.mp3")

		player.nextFromPool()

		assertEquals(listOf("music/menu.mp3"), provider.queuedNames)
		assertEquals(1, provider.updates)
		assertEquals(0, provider.resourceRequests)
		provider.loaded = true

		player.nextFromPool()

		assertEquals(1, provider.resourceRequests)
		assertTrue(provider.music.playing)
	}

	@Test
	fun `a failed selected track is quarantined so the pool can advance`() {
		val provider = RecordingAssetProvider()
		global(clear = true) { singleton<AssetProvider> { provider } }
		val player = MetaMusicPlayer()
		player.addMusicToPool("music/good.mp3")
		player.addMusicToPool("music/broken.mp3")
		player.nextFromPool()
		player.discardSelectedTrack()

		player.nextFromPool()

		assertEquals(listOf("music/broken.mp3", "music/good.mp3"), provider.queuedNames)
		assertFalse(provider.loaded)
	}

	private class RecordingAssetProvider : AssetProvider {
		var resourceRequests = 0
		var updates = 0
		var loaded = false
		val queuedNames = mutableListOf<String>()
		val music = RecordingMusic()

		override fun loadPackedAssetsFromFolder(folder: FileHandle) = false
		override fun loadRawAssetsFromFolder(folder: FileHandle) = false
		override fun <T : Any> load(name: String, type: Class<T>) {
			queuedNames.add(name)
		}
		override fun <T : Any> isLoaded(name: String, type: Class<T>) = loaded
		override fun update(millis: Int): Boolean {
			updates++
			return loaded
		}
		override fun <T : Any> getResource(fileName: String, type: Class<T>, index: Int): T {
			resourceRequests++
			return type.cast(music)
		}
		override fun getDrawable(name: String): Drawable = error("unused")
		override fun finish() = Unit
		override fun loadAnimationFrames(baseName: String, frames: Int): Array<out TextureRegion> = Array()
	}

	private class RecordingMusic : Music {
		var playing = false
		private var volume = 0f
		override fun play() { playing = true }
		override fun pause() { playing = false }
		override fun stop() { playing = false }
		override fun isPlaying() = playing
		override fun setLooping(isLooping: Boolean) = Unit
		override fun isLooping() = false
		override fun setVolume(volume: Float) { this.volume = volume }
		override fun getVolume() = volume
		override fun setPan(pan: Float, volume: Float) { this.volume = volume }
		override fun setPosition(position: Float) = Unit
		override fun getPosition() = 0f
		override fun dispose() = Unit
		override fun setOnCompletionListener(listener: Music.OnCompletionListener?) = Unit
	}
}
