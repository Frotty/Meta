package de.fatox.meta.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import de.fatox.meta.EditorMeta

fun main() {
	val posModifier = DesktopWindowHandler()
	val config = Lwjgl3ApplicationConfiguration().apply {
		setResizable(true)
		setTitle("Meta")
		setWindowIcon("meta-icon.png")
		setWindowListener(posModifier)
	}

	MetaDesktopLauncher.init(config, EditorMeta(posModifier, DesktopMonitorHandler(), DesktopSoundHandler(), DesktopGraphicsHandler()))
}