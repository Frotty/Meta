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
}
