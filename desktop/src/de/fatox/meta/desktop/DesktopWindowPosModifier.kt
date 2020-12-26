package de.fatox.meta.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener
import de.fatox.meta.Meta
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import org.slf4j.Logger

private val log: Logger = MetaLoggerFactory.logger {}

class DesktopWindowPosModifier : PosModifier, Lwjgl3WindowListener {
	private lateinit var currentWindow: Lwjgl3Window

	override val x: Int get() = currentWindow.positionX
	override val y: Int get() = currentWindow.positionY

	override fun modify(x: Int, y: Int) {
		log.debug { "Modify $currentWindow from ${this.x},${this.y} to $x,$y" }
		currentWindow.setPosition(x, y)
	}

	override fun created(window: Lwjgl3Window) {
		currentWindow = window
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
