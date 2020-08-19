package de.fatox.meta.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.ReflectionException
import de.fatox.meta.api.PosModifier
import org.lwjgl.glfw.GLFW
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger(WindowPosModifier::class.java)

private const val REFLECTION_ERROR = "Reflection failed!"
private const val CURRENT_WINDOW = "currentWindow"

class WindowPosModifier : PosModifier {
	private val currentWindow: Lwjgl3Window? by lazy(LazyThreadSafetyMode.NONE) {
		try {
			val currentWindowField = ClassReflection.getDeclaredField(Lwjgl3Application::class.java, CURRENT_WINDOW)
			currentWindowField.isAccessible = true
			val result = currentWindowField.get(Gdx.app) as Lwjgl3Window
			currentWindowField.isAccessible = false
			result
		} catch (e: ReflectionException) {
			log.error(REFLECTION_ERROR, e)
			null
		}
	}

	override val x: Int get() = currentWindow?.positionX ?: 2
	override val y: Int get() = currentWindow?.positionY ?: 12

	override fun modify(x: Int, y: Int) {
		currentWindow?.let { GLFW.glfwSetWindowPos(it.windowHandle, x, y) }
	}
}
