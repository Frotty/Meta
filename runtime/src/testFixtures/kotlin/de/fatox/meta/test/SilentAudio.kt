package de.fatox.meta.test

import com.badlogic.gdx.Application
import com.badlogic.gdx.Audio
import com.badlogic.gdx.audio.AudioDevice
import com.badlogic.gdx.audio.AudioRecorder
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle

/**
 * An [Audio] that hands out sounds which do nothing, and counts what was asked of it.
 *
 * Written as real classes rather than a [java.lang.reflect.Proxy] for the same reason [NoOpGL20] is: a proxy boxes
 * its arguments into a fresh `Object[]` on every call, so a test measuring how much the audio path allocates would
 * be measuring the stub. Six methods is a small price for a reading that means something.
 *
 * [newSoundCalls] is what catches a decode happening at the wrong moment. `Gdx.audio.newSound` decodes the whole
 * file and uploads a buffer, so a play path that reaches it is a frame stall in a shipped game; a test can assert
 * the count is unchanged across a burst of plays.
 */
class SilentAudio : Audio {
	/** Every sound this handed out, so a test can assert on plays without holding the reference itself. */
	val sound: SilentSound = SilentSound()

	/** How many times a decode was requested. Each one is a blocking file read and buffer upload in production. */
	var newSoundCalls: Int = 0
		private set

	override fun newSound(fileHandle: FileHandle): Sound {
		newSoundCalls++
		return sound
	}

	override fun newMusic(file: FileHandle): Music =
		throw UnsupportedOperationException("SilentAudio serves sounds only; streaming music is a different path")

	override fun newAudioDevice(samplingRate: Int, isMono: Boolean): AudioDevice =
		throw UnsupportedOperationException("SilentAudio has no output device")

	override fun newAudioRecorder(samplingRate: Int, isMono: Boolean): AudioRecorder =
		throw UnsupportedOperationException("SilentAudio has no input device")

	override fun switchOutputDevice(deviceIdentifier: String?): Boolean = true

	override fun getAvailableOutputDevices(): kotlin.Array<String> = EMPTY_DEVICES

	/** Inherited from `Disposable`; there is nothing native behind this stub to release. */
	override fun dispose() = Unit

	private companion object {
		/** Shared so the accessor itself never allocates: it is reachable from a measured path. */
		val EMPTY_DEVICES = emptyArray<String>()
	}
}

/**
 * A [Sound] that plays nothing and records how often it was asked to.
 *
 * Allocation-free by construction, so it can sit inside an [AllocationProbe] measurement.
 */
class SilentSound : Sound {
	var playCalls: Int = 0
		private set
	var loopCalls: Int = 0
		private set

	/** Distinct ids, so anything keying state on a handle id sees distinct handles. */
	private var nextId = 0L

	fun resetCounts() {
		playCalls = 0
		loopCalls = 0
	}

	override fun play(): Long = play(1f)

	override fun play(volume: Float): Long {
		playCalls++
		return ++nextId
	}

	override fun play(volume: Float, pitch: Float, pan: Float): Long = play(volume)

	override fun loop(): Long = loop(1f)

	override fun loop(volume: Float): Long {
		loopCalls++
		return ++nextId
	}

	override fun loop(volume: Float, pitch: Float, pan: Float): Long = loop(volume)

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

/**
 * An [Application] that runs posted runnables immediately instead of deferring them to the next frame.
 *
 * Meta dispatches sound starts and other deferred work through `Gdx.app.postRunnable`, which in a test would
 * otherwise never run — there is no frame loop to drain the queue. Running inline keeps a test single-stepped and
 * deterministic.
 *
 * Kotlin's `by` generates real forwarding methods, so unlike a proxy this delegation costs nothing and allocates
 * nothing. That is what lets it sit inside an allocation measurement of a path that posts.
 */
class ImmediateApplication(private val delegate: Application) : Application by delegate {
	/** How many runnables were posted, so a test can assert a path defers exactly as often as intended. */
	var postedRunnables: Int = 0
		private set

	override fun postRunnable(runnable: Runnable) {
		postedRunnables++
		runnable.run()
	}
}
