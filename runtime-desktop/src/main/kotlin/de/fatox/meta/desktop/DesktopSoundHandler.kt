package de.fatox.meta.desktop

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALSound
import de.fatox.meta.api.SoundHandler

class DesktopSoundHandler : SoundHandler {
	override fun duration(sound: Sound): Float {
		require(sound is OpenALSound)
		return sound.duration()
	}
}
