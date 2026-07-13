package de.fatox.meta.sound

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.Vector2
import de.fatox.meta.Meta
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

object UninitializedSound : Sound {
	override fun dispose(): Unit = Unit

	override fun play(): Long = 0L

	override fun play(volume: Float): Long = 0L

	override fun play(volume: Float, pitch: Float, pan: Float): Long = 0L

	override fun loop(): Long = 0L

	override fun loop(volume: Float): Long = 0L

	override fun loop(volume: Float, pitch: Float, pan: Float): Long = 0L

	override fun stop(): Unit = Unit

	override fun stop(soundId: Long): Unit = Unit

	override fun pause(): Unit = Unit

	override fun pause(soundId: Long): Unit = Unit

	override fun resume(): Unit = Unit

	override fun resume(soundId: Long): Unit = Unit

	override fun setLooping(soundId: Long, looping: Boolean): Unit = Unit

	override fun setPitch(soundId: Long, pitch: Float): Unit = Unit

	override fun setVolume(soundId: Long, volume: Float): Unit = Unit

	override fun setPan(soundId: Long, pan: Float, volume: Float): Unit = Unit
}

class MetaSoundDefinition(soundName: String, val maxInstances: Int = 4) {
	val soundName: String = soundName.replace("/".toRegex(), "\\\\")
	var sound: Sound = UninitializedSound
		set(value) {
			field = value

			this.durationMs = (Meta.instance.soundHandler.duration(value) * 1000L).toLong()
		}
	var isLooping: Boolean = false
	var audibleRange: Float = DEFAULT_SOUND_RANGE
		private set
	var audibleRange2: Float = DEFAULT_SOUND_RANGE2
		private set
	var volume: Float = DEFAULT_SOUND_VOLUME
	/** Positional falloff. SMOOTH is intentionally steep in the middle/far field to suppress ambient impact spam. */
	var attenuation: MetaSoundAttenuation = MetaSoundAttenuation.SMOOTH
	/** Fraction of base volume at max range: zero for detail sounds, roughly 0.03-0.06 for important distant events. */
	var distantVolume: Float = 0f
	/** Base repeat cooldown in milliseconds. Set to zero for event-critical sounds that must preserve every request. */
	var minimumPauseMs: Float = DEFAULT_MINIMUM_PAUSE_MS
	/** Multiplies [minimumPauseMs] toward max range. Has no effect while [minimumPauseMs] is zero. */
	var distantCooldownMultiplier: Float = DEFAULT_DISTANT_COOLDOWN_MULTIPLIER
	/** Subtle pitch variation applied only to positional playback. */
	var randomPitchRange: Float = 0.035f
	var durationMs: Long = DEFAULT_SOUND_DURATION
		private set

	private val soundPlayer: MetaSoundPlayer by lazyInject()

	fun play(
		listenerPosition: Vector2?,
		soundPosition: Vector2,
		minimumPause: Float = minimumPauseMs,
	): MetaSoundHandle? {
		return soundPlayer.playSound(this, listenerPosition, minimumPause, soundPosition)
	}

	fun play(minimumPause: Float): MetaSoundHandle? {
		return soundPlayer.playSound(this, minimumPause = minimumPause)
	}

	fun play(): MetaSoundHandle? {
		return soundPlayer.playSound(this, minimumPause = minimumPauseMs)
	}

	fun setSoundRange(soundRange: Float) {
		audibleRange = soundRange
		audibleRange2 = soundRange * soundRange
	}

	companion object {
		const val DEFAULT_SOUND_DURATION: Long = 2000
		const val DEFAULT_SOUND_VOLUME: Float = 0.4f
		const val DEFAULT_SOUND_RANGE: Float = 600f
		const val DEFAULT_SOUND_RANGE2: Float = DEFAULT_SOUND_RANGE * DEFAULT_SOUND_RANGE
		/** Generic effects may repeat rapidly nearby, but should not form a continuous wall of sound. */
		const val DEFAULT_MINIMUM_PAUSE_MS: Float = 50f
		/** At maximum range, generic repeated effects are admitted at one third of their near-field rate. */
		const val DEFAULT_DISTANT_COOLDOWN_MULTIPLIER: Float = 3f
	}
}
