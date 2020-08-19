package de.fatox.meta.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import de.fatox.meta.Meta

object MetaDesktopLauncher {
	fun init(config: Lwjgl3ApplicationConfiguration, meta: Meta) {
		config.setWindowedMode(329, 210)
		config.setDecorated(false)
//		config.setTransparentFramebuffer(true)
		config.useOpenGL3(true, 3, 2)
		config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4)
		Lwjgl3Application(meta, config)
	}
}
