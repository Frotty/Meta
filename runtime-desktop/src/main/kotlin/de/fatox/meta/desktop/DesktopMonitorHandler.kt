package de.fatox.meta.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import de.fatox.meta.api.MonitorHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import org.slf4j.Logger

private val log: Logger = MetaLoggerFactory.logger {}

class DesktopMonitorHandler : MonitorHandler {
	override fun monitorIndex(displayMode: Graphics.DisplayMode): Int {
		require(displayMode is Lwjgl3Graphics.Lwjgl3DisplayMode)

		return Gdx.graphics.monitors.indexOfFirst {
			if (it is Lwjgl3Graphics.Lwjgl3Monitor) {
				it.monitorHandle == displayMode.monitor
			} else false
		}.coerceAtLeast(0)
	}

	/**
	 * GLFW reports the user's display scale, such as 150% in Windows settings. `Graphics.density` uses the
	 * monitor's physical PPI. Take the larger scale when the two axes differ to keep the UI readable.
	 */
	override val osContentScale: Float
		get() {
			val window = (Gdx.graphics as? Lwjgl3Graphics)?.window?.windowHandle ?: return 1f
			return try {
				val x = FloatArray(1)
				val y = FloatArray(1)
				org.lwjgl.glfw.GLFW.glfwGetWindowContentScale(window, x, y)
				val scale = maxOf(x[0], y[0])
				if (scale.isFinite() && scale > 0f) scale else 1f
			} catch (failure: Throwable) {
				// A platform without content-scale support must degrade to unscaled rather than fail to start.
				log.debug { "GLFW did not report a window content scale; assuming no OS scaling: $failure" }
				1f
			}
		}
}
