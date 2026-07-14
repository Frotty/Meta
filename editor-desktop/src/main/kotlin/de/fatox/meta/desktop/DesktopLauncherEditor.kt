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
		// Avoid a visible flash of LWJGL3's tiny 640x480 default before the splash screen finishes loading and
		// applies the persisted window bounds (see MetaAudioVideoData.apply()).
		setWindowedMode(1280, 900)
	}

	MetaDesktopLauncher.init(config, EditorMeta(posModifier, DesktopMonitorHandler(), DesktopSoundHandler(), DesktopGraphicsHandler()))
}