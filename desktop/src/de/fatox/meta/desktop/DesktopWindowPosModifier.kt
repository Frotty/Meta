package de.fatox.meta.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.trace
import org.slf4j.Logger

private val log: Logger = MetaLoggerFactory.logger {}

class DesktopWindowPosModifier : PosModifier {
	private val currentWindow: Lwjgl3Window by lazy(LazyThreadSafetyMode.NONE) {
		(Gdx.graphics as Lwjgl3Graphics).window
	}

	override val x: Int get() = currentWindow.positionX
	override val y: Int get() = currentWindow.positionY

	override fun modify(x: Int, y: Int) {
		log.trace { "${this.x},${this.y} -> $x,$y" }
		currentWindow.setPosition(x, y)
	}
}
