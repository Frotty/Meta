package de.fatox.meta.sound

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALSound
import com.badlogic.gdx.backends.lwjgl3.audio.mock.MockSound
import com.badlogic.gdx.math.Vector2
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

object UninitializedSound: MockSound()

class MetaSoundDefinition(soundName: String, val maxInstances: Int = 4) {
	val soundName: String = soundName.replace("/".toRegex(), "\\\\")
	var sound: Sound = UninitializedSound
		set(value) {
			field = value

			if (value is OpenALSound)
				this.duration = (value.duration() * 1000L).toLong() // TODO The only sound without duration is MockSound
		}
	var isLooping = false
	var soundRange2 = DEFAULT_SOUND_RANGE2
		private set
	var volume = DEFAULT_SOUND_VOLUME
	var duration = DEFAULT_SOUND_DURATION
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
		const val DEFAULT_SOUND_VOLUME = 0.4f
		const val DEFAULT_SOUND_RANGE2 = 600f * 600f
	}
}