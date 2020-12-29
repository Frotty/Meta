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

			this.duration = (Meta.instance.soundHandler.duration(value) * 1000L).toLong()
		}
	var isLooping: Boolean = false
	var soundRange2: Float = DEFAULT_SOUND_RANGE2
		private set
	var volume: Float = DEFAULT_SOUND_VOLUME
	var duration: Long = DEFAULT_SOUND_DURATION
		private set

	private val soundPlayer: MetaSoundPlayer by lazyInject()

	fun play(listenerPosition: Vector2?, soundPosition: Vector2): MetaSoundHandle? {
		return soundPlayer.playSound(this, listenerPosition, soundPosition)
	}

	fun play(): MetaSoundHandle? {
		return soundPlayer.playSound(this)
	}

	fun setSoundRange(soundRange: Float) {
		soundRange2 = soundRange * soundRange
	}

	companion object {
		const val DEFAULT_SOUND_DURATION: Long = 2000
		const val DEFAULT_SOUND_VOLUME: Float = 0.4f
		const val DEFAULT_SOUND_RANGE2: Float = 600f * 600f
	}
}