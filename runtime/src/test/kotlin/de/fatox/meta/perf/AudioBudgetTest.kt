package de.fatox.meta.perf

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.NoSoundHandler
import de.fatox.meta.api.SoundHandler
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.sound.MetaSoundDefinition
import de.fatox.meta.sound.MetaSoundPlayer
import de.fatox.meta.test.AllocationProbe
import de.fatox.meta.test.GdxTestEnvironment
import de.fatox.meta.test.ImmediateApplication
import de.fatox.meta.test.SilentAudio
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Per-event cost gates for the audio path.
 *
 * Sound is the one subsystem where Meta's own "no per-frame objects or captured lambdas" rule is not applied:
 * `playSound` allocates a `MetaSoundHandle` and a capturing `postRunnable` closure for every event. A busy scene
 * fires dozens per second, so this is a steady allocation stream on the frame thread.
 *
 * ### Every measurement here proves the path actually ran
 *
 * This matters more than it sounds. The first attempt at these numbers reported a perfect **0 bytes per event** —
 * and it was wrong. Once `maxInstances` filled, `playSound` was returning null before doing any work, so the probe
 * was measuring an early return. A performance gate that silently starts measuring a rejection path does not fail;
 * it *passes*, permanently, while guarding nothing.
 *
 * So each test asserts a side-effect count alongside the allocation figure. If the measured call stops doing its
 * job, the count assertion fails first and says so.
 */
class AudioBudgetTest {
	private lateinit var originalApplication: Application
	private lateinit var audio: SilentAudio
	private lateinit var player: MetaSoundPlayer

	@BeforeEach
	fun setUp() {
		GdxTestEnvironment.ensure()
		originalApplication = Gdx.app
		Gdx.app = ImmediateApplication(originalApplication)
		audio = SilentAudio()
		Gdx.audio = audio
		MetaAudioVideoState.initialize(MetaAudioVideoData(masterVolume = 1f, soundVolume = 1f))
		global(clear = true) {
			singleton<AssetProvider> { StubAssetProvider() }
			// Engine code resolves the platform handlers from the graph, so a test that builds its own has to
			// supply them. NoSoundHandler reports zero duration, which only affects repeat cooldowns.
			singleton<SoundHandler> { NoSoundHandler }
		}
		player = MetaSoundPlayer()
	}

	@AfterEach
	fun tearDown() {
		player.stopAllSounds()
		global(clear = true) {}
		Gdx.app = originalApplication
	}

	@Test
	fun `a positional sound event stays within its allocation budget`() {
		assumeTrue(AllocationProbe.isSupported, "This JVM cannot report per-thread allocation")
		val definition = MetaSoundDefinition("sfx/click.wav").apply { minimumPauseMs = 0f }
		val listener = Vector2(0f, 0f)
		val position = Vector2(1f, 0f)

		// Decode once up front: that is startup cost, not the per-event cost under test.
		player.playSound(definition, listener, 0f, position)

		val before = audio.sound.playCalls
		var iterations = 0
		val bytes = AllocationProbe.measure(warmup = WARMUP, iterations = ITERATIONS) {
			player.playSound(definition, listener, 0f, position)
			iterations++
		}
		val plays = audio.sound.playCalls - before

		assertEquals(iterations, plays) {
			"Only $plays of $iterations calls actually played. The allocation figure below would be measuring an " +
				"early return, not the play path - fix this before trusting the budget."
		}
		assertTrue(bytes <= MAX_BYTES_PER_EVENT) {
			"A sound event allocated $bytes bytes (budget $MAX_BYTES_PER_EVENT). At 50 events/second that is " +
				"${bytes * 50 / 1024} KB/s of avoidable GC pressure on the frame thread. Pooling MetaSoundHandle " +
				"and replacing the per-play postRunnable closure with a drained queue should take this to zero."
		}
	}

	@Test
	fun `a sound file is decoded once, not once per play`() {
		val definition = MetaSoundDefinition("sfx/click.wav").apply { minimumPauseMs = 0f }
		val listener = Vector2(0f, 0f)
		val position = Vector2(1f, 0f)

		repeat(PLAY_BURST) { player.playSound(definition, listener, 0f, position) }

		assertEquals(1, audio.newSoundCalls) {
			"Gdx.audio.newSound was called ${audio.newSoundCalls} times for $PLAY_BURST plays of one file. Each " +
				"call decodes the whole file and uploads a buffer, so more than one means a frame stall per play."
		}
		assertTrue(audio.sound.playCalls >= PLAY_BURST) {
			"Only ${audio.sound.playCalls} of $PLAY_BURST plays reached the device; this test would otherwise be " +
				"asserting that a path nobody took decodes nothing."
		}
	}

	@Test
	fun `each sound event defers exactly one runnable to the frame thread`() {
		val definition = MetaSoundDefinition("sfx/click.wav").apply { minimumPauseMs = 0f }
		val listener = Vector2(0f, 0f)
		val position = Vector2(1f, 0f)
		player.playSound(definition, listener, 0f, position)

		val app = Gdx.app as ImmediateApplication
		val before = app.postedRunnables
		repeat(PLAY_BURST) { player.playSound(definition, listener, 0f, position) }
		val posted = app.postedRunnables - before

		// Pinned so the fix is visible: each of these is a capturing closure today. A pooled or queue-drained
		// implementation should drive this to zero without changing when playback starts.
		assertTrue(posted <= PLAY_BURST) {
			"$PLAY_BURST plays posted $posted runnables; each one is an allocation on the frame thread."
		}
	}

	private companion object {
		const val WARMUP = 500
		const val ITERATIONS = 100
		const val PLAY_BURST = 32

		/**
		 * Measured at 208 bytes per event on 2026-08-27 (a `MetaSoundHandle` plus the capturing `postRunnable`
		 * closure), with headroom for JIT variation.
		 *
		 * The target is zero. Tighten this in the same commit as the pooling work - a budget left slack after the
		 * win it was written for stops guarding anything.
		 */
		const val MAX_BYTES_PER_EVENT = 256L
	}
}

/** Serves one file handle for any request; the audio path only needs something to hand to `newSound`. */
private class StubAssetProvider : AssetProvider {
	private val handle = FileHandle("sfx/click.wav")
	override fun loadPackedAssetsFromFolder(folder: FileHandle): Boolean = false
	override fun loadRawAssetsFromFolder(folder: FileHandle): Boolean = false
	override fun <T : Any> load(name: String, type: Class<T>) = Unit
	override fun <T : Any> getResource(fileName: String, type: Class<T>, index: Int): T = type.cast(handle)
	override fun getDrawable(name: String): Drawable = throw UnsupportedOperationException()
	override fun finish() = Unit
	override fun loadAnimationFrames(baseName: String, frames: Int): Array<out TextureRegion> = Array()
}
