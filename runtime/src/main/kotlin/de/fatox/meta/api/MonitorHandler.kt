package de.fatox.meta.api

import com.badlogic.gdx.Graphics

interface MonitorHandler {
	fun monitorIndex(displayMode: Graphics.DisplayMode): Int
}

object NoMonitorHandler : MonitorHandler {
	override fun monitorIndex(displayMode: Graphics.DisplayMode): Int = 0
}