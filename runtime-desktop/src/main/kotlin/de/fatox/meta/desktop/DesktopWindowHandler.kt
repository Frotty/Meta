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
	private var maximized = false

	override val x: Int get() = if (::currentWindow.isInitialized) currentWindow.positionX else 0
	override val y: Int get() = if (::currentWindow.isInitialized) currentWindow.positionY else 0
	override val isMaximized: Boolean get() = maximized

	override fun modify(x: Int, y: Int) {
		if (!::currentWindow.isInitialized) return
		log.debug { "Modify $currentWindow from ${this.x},${this.y} to $x,$y!" }
		currentWindow.setPosition(x, y)
		focus()
	}

	override fun focus() {
		if (!::currentWindow.isInitialized) return
		currentWindow.setVisible(true)
		// GLFW's restore operation can undo setWindowedMode on Linux by restoring the previous geometry.
		currentWindow.focusWindow()
	}

	override fun maximize() {
		if (!::currentWindow.isInitialized) return
		currentWindow.maximizeWindow()
		maximized = true
	}

	override fun restore() {
		if (!::currentWindow.isInitialized) return
		currentWindow.restoreWindow()
		maximized = false
	}

	override fun iconify() {
		if (!::currentWindow.isInitialized) return
		log.debug { "Iconify $currentWindow!" }
		currentWindow.iconifyWindow()
	}

	override fun created(window: Lwjgl3Window) {
		currentWindow = window
		focus()
	}

	override fun iconified(isIconified: Boolean) = Meta.instance.iconified(isIconified)

	override fun maximized(isMaximized: Boolean) {
		maximized = isMaximized
		Meta.instance.maximized(isMaximized)
	}

	override fun focusLost() = Meta.instance.onFocusLost()

	override fun focusGained() = Meta.instance.onFocusGained()

	override fun closeRequested(): Boolean {
		Gdx.app.exit()
		return true
	}

	override fun filesDropped(files: Array<out String>) = Unit

	override fun refreshRequested() = Unit
}
