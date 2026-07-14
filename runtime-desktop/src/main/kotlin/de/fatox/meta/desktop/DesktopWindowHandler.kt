package de.fatox.meta.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener
import de.fatox.meta.Meta
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import org.slf4j.Logger

private val log: Logger = MetaLoggerFactory.logger {}

class DesktopWindowHandler : WindowHandler, Lwjgl3WindowListener {
	private lateinit var currentWindow: Lwjgl3Window

	// WindowHandler documents x/y as "defaults to zero if unknown" - guard the same way focus() already does,
	// so a read before created() fires (e.g. no window listener registered) degrades instead of crashing.
	override val x: Int get() = if (::currentWindow.isInitialized) currentWindow.positionX else 0
	override val y: Int get() = if (::currentWindow.isInitialized) currentWindow.positionY else 0

	override fun modify(x: Int, y: Int) {
		log.debug { "Modify $currentWindow from ${this.x},${this.y} to $x,$y!" }
		currentWindow.setPosition(x, y)
		focus()
	}

	override fun focus() {
		if (!::currentWindow.isInitialized) return
		currentWindow.setVisible(true)
		// restoreWindow() also restores GLFW's previous window geometry. Calling it after a mode transition can undo
		// setWindowedMode on Linux and resurrect the launcher's tiny bootstrap size (238x127). Focusing must not mutate
		// size/position; explicit iconify restoration belongs to an explicit restore operation if one is ever needed.
		currentWindow.focusWindow()
	}

	override fun iconify() {
		log.debug { "Iconify $currentWindow!" }
		currentWindow.iconifyWindow()
	}

	override fun created(window: Lwjgl3Window) {
		currentWindow = window
		focus()
	}

	override fun iconified(isIconified: Boolean) = Meta.instance.iconified(isIconified)

	override fun maximized(isMaximized: Boolean) =  Meta.instance.maximized(isMaximized)

	override fun focusLost() = Meta.instance.onFocusLost()

	override fun focusGained() = Meta.instance.onFocusGained()

	override fun closeRequested(): Boolean {
		Gdx.app.exit()
		return true
	}

	override fun filesDropped(files: Array<out String>) = Unit

	override fun refreshRequested() = Unit
}
