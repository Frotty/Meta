package de.fatox.meta.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import de.fatox.meta.Meta

object MetaDesktopLauncher {
	/**
	 * Starts Meta in the application's real window. The former transparent 352x148 bootstrap window was unreliable
	 * on HiDPI macOS and could leak its temporary geometry/framebuffer attributes into the game presentation.
	 *
	 * [splashWindow] remains only for source/binary compatibility and is intentionally ignored. Games can keep using
	 * Meta's [de.fatox.meta.api.SplashScreen] as an ordinary opaque loading screen inside their configured window.
	 */
	@Suppress("UNUSED_PARAMETER")
	fun init(config: Lwjgl3ApplicationConfiguration, meta: Meta, splashWindow: Boolean = false) {
		config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2)
		config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4)
		// Don't burn full frames while unfocused/minimised.
		config.setIdleFPS(10)
		config.setPauseWhenMinimized(true)
		Lwjgl3Application(meta, config)
	}
}
