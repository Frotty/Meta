package de.fatox.meta.desktop

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALSound
import de.fatox.meta.api.SoundHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import org.slf4j.Logger

private val log: Logger = MetaLoggerFactory.logger {}

class DesktopSoundHandler : SoundHandler {
	override fun duration(sound: Sound): Float {
		require(sound is OpenALSound)

		return sound.duration()
	}
}
