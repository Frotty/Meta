package de.fatox.meta.sound

import com.badlogic.gdx.Audio
import com.badlogic.gdx.Application
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
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.test.GdxTestEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MetaSoundSourceTest {
	private lateinit var originalApplication: Application
	private lateinit var assetProvider: RecordingAssetProvider
	private lateinit var audio: RecordingAudio

	@BeforeEach
	fun setUp() {
		GdxTestEnvironment.ensure()
		originalApplication = Gdx.app
		Gdx.app = immediateApplication(originalApplication)
		stopAllSoundSources()
		silencePositionalSounds(false)
		MetaAudioVideoState.initialize(MetaAudioVideoData(masterVolume = 1f, soundVolume = 1f))
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
		Gdx.app = originalApplication
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

	@Test
	fun `smooth falloff quickly quiets middle and far field`() {
		assertEquals(1f, MetaSoundFalloff.gain(0f, MetaSoundAttenuation.SMOOTH, 0f), 0.0001f)
		assertEquals(0.25f, MetaSoundFalloff.gain(0.5f, MetaSoundAttenuation.SMOOTH, 0f), 0.0001f)
		assertTrue(MetaSoundFalloff.gain(0.8f, MetaSoundAttenuation.SMOOTH, 0f) < 0.012f)
		assertEquals(0f, MetaSoundFalloff.gain(1f, MetaSoundAttenuation.SMOOTH, 0f), 0.0001f)
	}

	@Test
	fun `distant tail preserves quiet awareness for important events`() {
		assertEquals(0.05f, MetaSoundFalloff.gain(1f, MetaSoundAttenuation.SMOOTH, 0.05f), 0.0001f)
	}

	@Test
	fun `event critical positional playback admits every requested event when cooldown is disabled`() {
		val player = MetaSoundPlayer()
		val definition = recordingDefinition(maxInstances = 16).apply { minimumPauseMs = 0f }

		repeat(8) {
			assertNotNull(player.playSound(definition, Vector2.Zero, soundPos = Vector2(10f, 0f)))
		}

		assertEquals(8, audio.sound.playCalls)
	}

	@Test
	fun `default positional playback applies a modest anti spam cooldown`() {
		val player = MetaSoundPlayer()
		val definition = recordingDefinition()

		assertEquals(50f, definition.minimumPauseMs)
		assertEquals(3f, definition.distantCooldownMultiplier)
		assertNotNull(player.playSound(definition, Vector2.Zero, soundPos = Vector2(10f, 0f)))
		assertNull(player.playSound(definition, Vector2.Zero, soundPos = Vector2(10f, 0f)))
		assertEquals(1, audio.sound.playCalls)
	}

	@Test
	fun `configured cooldown throttles repeated positional detail`() {
		val player = MetaSoundPlayer()
		val definition = recordingDefinition().apply { minimumPauseMs = 250f }

		assertNotNull(player.playSound(definition, Vector2.Zero, soundPos = Vector2(10f, 0f)))
		assertNull(player.playSound(definition, Vector2.Zero, soundPos = Vector2(10f, 0f)))
		assertEquals(1, audio.sound.playCalls)
	}

	@Test
	fun `distance multiplier only scales an explicitly configured cooldown`() {
		assertEquals(0f, MetaSoundPlaybackPolicy.cooldownMs(0f, 1f, 5f))
		assertEquals(100f, MetaSoundPlaybackPolicy.cooldownMs(100f, 0f, 4f))
		assertEquals(175f, MetaSoundPlaybackPolicy.cooldownMs(100f, 0.5f, 4f))
		assertEquals(400f, MetaSoundPlaybackPolicy.cooldownMs(100f, 1f, 4f))
	}

	@Test
	fun `out of range positional event is rejected before playback`() {
		val player = MetaSoundPlayer()
		val definition = recordingDefinition().apply { setSoundRange(100f) }

		assertNull(player.playSound(definition, Vector2.Zero, soundPos = Vector2(101f, 0f)))
		assertEquals(0, audio.sound.playCalls)
	}

	@Test
	fun `quiet positive positional volume is not hidden behind a playback threshold`() {
		val player = MetaSoundPlayer()
		val definition = recordingDefinition().apply {
			volume = 0.001f
			minimumPauseMs = 0f
		}

		assertNotNull(player.playSound(definition, Vector2.Zero, soundPos = Vector2.Zero))
		assertEquals(0.001f, audio.sound.lastPlayVolume, 0.00001f)
	}

	@Test
	fun `distant playback starts and remains at attenuated volume`() {
		val player = MetaSoundPlayer()
		val definition = recordingDefinition().apply {
			volume = 1f
			setSoundRange(100f)
		}
		val listener = Vector2.Zero

		assertNotNull(player.playSound(definition, listener, soundPos = Vector2(50f, 0f)))
		assertEquals(0.25f, audio.sound.lastPlayVolume, 0.0001f)

		player.updateDynamicSounds(listener, 1f / 60f)

		assertEquals(0.25f, audio.sound.lastPanVolume, 0.0001f)
	}

	private fun recordingDefinition(maxInstances: Int = 4): MetaSoundDefinition {
		return MetaSoundDefinition("sound/test.wav", maxInstances).also { definition ->
			// Avoid loading/backend duration lookup: this test supplies a deterministic Sound directly.
			val soundField = MetaSoundDefinition::class.java.getDeclaredField("sound")
			soundField.isAccessible = true
			soundField.set(definition, audio.sound)
		}
	}
}

private fun immediateApplication(delegate: Application): Application {
	return Proxy.newProxyInstance(
		Application::class.java.classLoader,
		arrayOf(Application::class.java),
	) { _, method, args ->
		if (method.name == "postRunnable") {
			(args!![0] as Runnable).run()
			Unit
		} else {
			method.invoke(delegate, *(args ?: emptyArray()))
		}
	} as Application
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
	var playCalls = 0
	var lastPlayVolume = 0f
	var lastPanVolume = 0f

	override fun play(): Long = 1L
	override fun play(volume: Float): Long = 1L
	override fun play(volume: Float, pitch: Float, pan: Float): Long {
		playCalls++
		lastPlayVolume = volume
		return playCalls.toLong()
	}
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
	override fun setPan(soundId: Long, pan: Float, volume: Float) {
		lastPanVolume = volume
	}
}
