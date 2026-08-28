package de.fatox.meta.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener
import de.fatox.meta.Meta
import de.fatox.meta.api.MonitorHandler
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import org.slf4j.Logger
import java.awt.DisplayMode

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
	 * The window's content scale as GLFW reports it - on Windows, the ratio of the current DPI to the platform
	 * default of 96, which is exactly the percentage in the display settings.
	 *
	 * libGDX does not surface this. `Graphics.density` is `getPpiX() / 160`, derived from the monitor's physical
	 * size, so it describes the *panel* rather than what the user asked for: a 189 PPI laptop reports 1.18 whether
	 * Windows is set to 100% or 200%.
	 *
	 * Both axes are read and the larger taken. They agree on every configuration seen in practice, and picking the
	 * larger fails towards a readable interface rather than a small one.
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
