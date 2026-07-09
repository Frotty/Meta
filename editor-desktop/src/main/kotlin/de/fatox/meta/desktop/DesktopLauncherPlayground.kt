package de.fatox.meta.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import de.fatox.meta.playground.MetaUiPlaygroundMeta

fun main() {
	val posModifier = DesktopWindowHandler()
	val config = Lwjgl3ApplicationConfiguration().apply {
		setResizable(true)
		setTitle("Meta UI Playground")
		setWindowIcon("meta-icon.png")
		setWindowListener(posModifier)
		setWindowedMode(1280, 900)
	}

	MetaDesktopLauncher.init(
		config,
		MetaUiPlaygroundMeta(posModifier, DesktopMonitorHandler(), DesktopSoundHandler(), DesktopGraphicsHandler()),
		splashWindow = false,
	)
}
