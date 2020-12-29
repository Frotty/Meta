package de.fatox.meta.desktop

import de.fatox.meta.desktop.MetaDesktopLauncher.init
import kotlin.jvm.JvmStatic
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import de.fatox.meta.EditorMeta
import de.fatox.meta.desktop.WindowPosModifier
import de.fatox.meta.desktop.MetaDesktopLauncher

object Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()
        config.setResizable(true)
        config.setTitle("Meta")
        config.setWindowIcon("meta-icon.png")
        val editorMeta = EditorMeta(WindowPosModifier())
        init(config, editorMeta)
    }
}