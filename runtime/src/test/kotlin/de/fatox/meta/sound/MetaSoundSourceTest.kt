package de.fatox.meta.sound

import com.badlogic.gdx.Audio
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.AudioDevice
import com.badlogic.gdx.audio.AudioRecorder
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.test.GdxTestEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetaSoundSourceTest {
	private lateinit var assetProvider: RecordingAssetProvider
	private lateinit var audio: RecordingAudio

	@BeforeEach
	fun setUp() {
		GdxTestEnvironment.ensure()
		stopAllSoundSources()
		silencePositionalSounds(false)
		assetProvider = RecordingAssetProvider()
		audio = RecordingAudio()
		Gdx.audio = audio
		global(clear = true) {
			singleton<AssetProvider> { assetProvider }
		}
	}

	@AfterEach
	fun tearDown() {
		stopAllSoundSources()
		silencePositionalSounds(false)
		global(clear = true) {}
	}

	@Test
	fun `positional sound loads through normalized asset key`() {
		MetaPositionalSoundDefinition("sound/spider_walk.wav").sound

		assertEquals("sound\\spider_walk.wav", assetProvider.lastRequestedFileName)
		assertEquals(1, assetProvider.resourceRequests)
		assertEquals(1, audio.newSoundCalls)
	}

	@Test
	fun `silenced positional sound does not load or play`() {
		silencePositionalSounds(true)
		addSoundSource(MetaPositionalSoundDefinition("sound/spider_walk.wav"), Vector2.Zero)

		updateSoundSources(Vector2.Zero, 1f / 60f)

		assertEquals(0, assetProvider.resourceRequests)
		assertEquals(0, audio.newSoundCalls)
		assertEquals(0, audio.sound.loopCalls)
	}
}

private class RecordingAssetProvider : AssetProvider {
	var resourceRequests = 0
	var lastRequestedFileName: String? = null

	override fun loadPackedAssetsFromFolder(folder: FileHandle): Boolean = false

	override fun loadRawAssetsFromFolder(folder: FileHandle): Boolean = false

	override fun <T : Any> load(name: String, type: Class<T>) = Unit

	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> getResource(fileName: String, type: Class<T>, index: Int): T {
		resourceRequests++
		lastRequestedFileName = fileName
		return FileHandle(fileName) as T
	}

	override fun getDrawable(name: String): Drawable {
		error("Drawable lookup is not used by this test")
	}

	override fun finish() = Unit

	override fun loadAnimationFrames(baseName: String, frames: Int): Array<out TextureRegion> = Array()
}

private class RecordingAudio : Audio {
	val sound = RecordingSound()
	var newSoundCalls = 0

	override fun newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDevice {
		error("Audio device creation is not used by this test")
	}

	override fun newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorder {
		error("Audio recorder creation is not used by this test")
	}

	override fun newSound(fileHandle: FileHandle): Sound {
		newSoundCalls++
		return sound
	}

	override fun newMusic(file: FileHandle): Music {
		error("Music creation is not used by this test")
	}

	override fun switchOutputDevice(deviceIdentifier: String): Boolean = false

	override fun getAvailableOutputDevices(): kotlin.Array<String> = emptyArray()

	override fun dispose() = Unit
}

private class RecordingSound : Sound {
	var loopCalls = 0

	override fun play(): Long = 1L
	override fun play(volume: Float): Long = 1L
	override fun play(volume: Float, pitch: Float, pan: Float): Long = 1L
	override fun loop(): Long {
		loopCalls++
		return 1L
	}

	override fun loop(volume: Float): Long {
		loopCalls++
		return 1L
	}

	override fun loop(volume: Float, pitch: Float, pan: Float): Long {
		loopCalls++
		return 1L
	}

	override fun stop() = Unit
	override fun pause() = Unit
	override fun resume() = Unit
	override fun dispose() = Unit
	override fun stop(soundId: Long) = Unit
	override fun pause(soundId: Long) = Unit
	override fun resume(soundId: Long) = Unit
	override fun setLooping(soundId: Long, looping: Boolean) = Unit
	override fun setPitch(soundId: Long, pitch: Float) = Unit
	override fun setVolume(soundId: Long, volume: Float) = Unit
	override fun setPan(soundId: Long, pan: Float, volume: Float) = Unit
}
