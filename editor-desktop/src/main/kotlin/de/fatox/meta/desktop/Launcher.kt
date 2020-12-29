package de.fatox.meta.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import de.fatox.meta.EditorMeta
import de.fatox.meta.desktop.MetaDesktopLauncher.init

fun main() {
	val config = Lwjgl3ApplicationConfiguration()
	config.setResizable(true)
	config.setTitle("Meta")
	config.setWindowIcon("meta-icon.png")
	val editorMeta = EditorMeta(WindowPosModifier())
	init(config, editorMeta)
}
