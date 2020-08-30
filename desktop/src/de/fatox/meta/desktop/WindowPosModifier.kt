package de.fatox.meta.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.ReflectionException
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger

private val log: Logger = MetaLoggerFactory.logger {}

private const val CURRENT_WINDOW = "currentWindow"

class WindowPosModifier : PosModifier {
	private val currentWindow: Lwjgl3Window? by lazy(LazyThreadSafetyMode.NONE) {
		try {
			log.debug { "Trying to access current lwjgl3 window." }
			val currentWindowField = ClassReflection.getDeclaredField(Lwjgl3Application::class.java, CURRENT_WINDOW)
			currentWindowField.isAccessible = true
			val result = currentWindowField.get(Gdx.app) as Lwjgl3Window
			currentWindowField.isAccessible = false
			log.debug { "Access of lwjgl3 window succeeded!" }
			result
		} catch (e: ReflectionException) {
			log.error(e) { "Access of lwjgl3 window failed!" }
			null
		}
	}

	override val x: Int get() = currentWindow?.positionX ?: 2
	override val y: Int get() = currentWindow?.positionY ?: 12

	override fun modify(x: Int, y: Int) {
		currentWindow?.let { GLFW.glfwSetWindowPos(it.windowHandle, x, y) }
	}
}
