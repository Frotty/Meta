package de.fatox.meta.sound

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.Vector2
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.injection.Inject
import java.lang.reflect.InvocationTargetException

class MetaSoundDefinition @JvmOverloads constructor(soundName: String, maxInstances: Int = 4) {
    val soundName: String
    val maxInstances: Int
    var sound: Sound? = null
        private set
    var isLooping = false
    var soundRange2 = DEFAULT_SOUND_RANGE2
        private set
    var volume = DEFAULT_SOUND_VOLUME
    var duration = DEFAULT_SOUND_DURATION
        private set

    @Inject
    private val soundPlayer: MetaSoundPlayer? = null
    fun play(listenerPosition: Vector2?, soundPosition: Vector2?): MetaSoundHandle? {
        return soundPlayer!!.playSound(this, listenerPosition, soundPosition)
    }

    fun play(): MetaSoundHandle? {
        return soundPlayer!!.playSound(this)
    }

    fun setSound(sound: Sound) {
        this.sound = sound
        try {
            val duration = sound.javaClass.getMethod("duration")
            if (duration != null) {
                this.duration = (duration.invoke(sound) as Float * 1000L).toLong()
            }
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    fun setSoundRange(soundRange: Float) {
        soundRange2 = soundRange * soundRange
    }

    companion object {
        const val DEFAULT_SOUND_DURATION: Long = 2000
        const val DEFAULT_SOUND_VOLUME = 0.4f
        const val DEFAULT_SOUND_RANGE2 = 600f * 600f
    }

    init {
        this.soundName = soundName.replace("/".toRegex(), "\\\\")
        this.maxInstances = maxInstances
        inject(this)
    }
}