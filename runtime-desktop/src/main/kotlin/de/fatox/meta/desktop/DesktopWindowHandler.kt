package de.fatox.meta.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.utils.reflect.ClassReflection
import com.badlogic.gdx.utils.reflect.ReflectionException
import de.fatox.meta.api.PosModifier
import org.lwjgl.glfw.GLFW

class DesktopWindowHandler : PosModifier {

    override fun modify(x: Int, y: Int) {
        try {
            val field = ClassReflection.getDeclaredField(Lwjgl3Application::class.java, "currentWindow")
            field.isAccessible = true
            val result = field.get(Gdx.app) as Lwjgl3Window
            GLFW.glfwSetWindowPos(result.windowHandle, x, y)
        } catch (e: ReflectionException) {
            e.printStackTrace()
        }

    }

    override fun getX(): Int {
        try {
            val field = ClassReflection.getDeclaredField(Lwjgl3Application::class.java, "currentWindow")
            field.isAccessible = true
            val result = field.get(Gdx.app) as Lwjgl3Window
            return result.positionX
        } catch (e: ReflectionException) {
            e.printStackTrace()
        }

        return 2
    }

    override fun getY(): Int {
        try {
            val field = ClassReflection.getDeclaredField(Lwjgl3Application::class.java, "currentWindow")
            field.isAccessible = true
            val result = field.get(Gdx.app) as Lwjgl3Window
            return result.positionY
        } catch (e: ReflectionException) {
            e.printStackTrace()
        }

        return 12
    }
}
