package de.fatox.meta.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import de.fatox.meta.EditorMeta

object DesktopLauncherEditor {

	@JvmStatic
	fun main(arg: Array<String>) {
		val config = Lwjgl3ApplicationConfiguration()
		config.setResizable(true)
		config.setTitle("Meta")
		config.setWindowIcon("meta-icon.png")
		val editorMeta = EditorMeta(DesktopWindowPosModifier())
		MetaDesktopLauncher.init(config, editorMeta)
	}
}
