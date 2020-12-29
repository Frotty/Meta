package de.fatox.meta.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener
import com.badlogic.gdx.backends.lwjgl3.audio.OpenALSound
import de.fatox.meta.Meta
import de.fatox.meta.api.MonitorHandler
import de.fatox.meta.api.SoundHandler
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import org.slf4j.Logger
import java.awt.DisplayMode

private val log: Logger = MetaLoggerFactory.logger {}

class DesktopSoundHandler : SoundHandler {
	override fun duration(sound: Sound): Float {
		require(sound is OpenALSound)

		return sound.duration()
	}
}
