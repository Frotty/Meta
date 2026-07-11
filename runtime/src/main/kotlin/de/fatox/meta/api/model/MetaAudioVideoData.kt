package de.fatox.meta.api.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import de.fatox.meta.Meta


data class MetaDisplayMode(
	val width: Int = 0,
	val height: Int = 0,
	val refreshRate: Int = 0,
	val bitsPerPixel: Int = 0,
	val monitorIndex: Int = 0 // Simplified to index, might cause problems when having more than two monitors
) {
	constructor(dm: Graphics.DisplayMode) : this(
		dm.width,
		dm.height,
		dm.refreshRate,
		dm.bitsPerPixel,
		dm.monitorIndex
	)

	fun equalsDisplayMode(dm: Graphics.DisplayMode): Boolean =
		width == dm.width &&
			height == dm.height &&
			refreshRate == dm.refreshRate &&
			bitsPerPixel == dm.bitsPerPixel
}

private inline val Graphics.DisplayMode.monitorIndex
	get() = Meta.instance.monitorHandler.monitorIndex(this)

fun Graphics.DisplayMode.toMetaDisplayMode(): MetaDisplayMode = MetaDisplayMode(this)

private fun getCurrentMonitor(): Graphics.Monitor {
	val graphics = Gdx.graphics
	val monitors = graphics.monitors
	val windowX = Meta.instance.windowHandler.x
	val windowY = Meta.instance.windowHandler.y
	val windowWidth = graphics.width.coerceAtLeast(1)
	val windowHeight = graphics.height.coerceAtLeast(1)
	val windowRight = windowX + windowWidth
	val windowBottom = windowY + windowHeight
	var bestMonitor = graphics.primaryMonitor
	var bestOverlap = -1L
	var bestCenterDistance = Long.MAX_VALUE

	for (i in monitors.indices) {
		val monitor = monitors[i]
		val mode = graphics.getDisplayMode(monitor)
		val monitorRight = monitor.virtualX + mode.width
		val monitorBottom = monitor.virtualY + mode.height
		val overlapWidth = (minOf(windowRight, monitorRight) - maxOf(windowX, monitor.virtualX)).coerceAtLeast(0)
		val overlapHeight = (minOf(windowBottom, monitorBottom) - maxOf(windowY, monitor.virtualY)).coerceAtLeast(0)
		val overlap = overlapWidth.toLong() * overlapHeight.toLong()
		val centerDx = (windowX * 2L + windowWidth) - (monitor.virtualX * 2L + mode.width)
		val centerDy = (windowY * 2L + windowHeight) - (monitor.virtualY * 2L + mode.height)
		val centerDistance = centerDx * centerDx + centerDy * centerDy
		if (overlap > bestOverlap || overlap == bestOverlap && centerDistance < bestCenterDistance) {
			bestOverlap = overlap
			bestCenterDistance = centerDistance
			bestMonitor = monitor
		}
	}
	return bestMonitor
}

private fun displayModeFor(monitor: Graphics.Monitor, requested: MetaDisplayMode): Graphics.DisplayMode {
	val graphics = Gdx.graphics
	val modes = graphics.getDisplayModes(monitor)
	var resolutionMatch: Graphics.DisplayMode? = null
	for (i in modes.indices) {
		val mode = modes[i]
		if (mode.width != requested.width || mode.height != requested.height) continue
		if (mode.refreshRate == requested.refreshRate && mode.bitsPerPixel == requested.bitsPerPixel) return mode
		if (resolutionMatch == null || mode.refreshRate > resolutionMatch.refreshRate) resolutionMatch = mode
	}
	return resolutionMatch ?: graphics.getDisplayMode(monitor)
}

/**
 * Created by Frotty on 05.11.2016.
 */
data class MetaAudioVideoData(
	var profile: String = "default",
	var hd: Boolean = true,
	var resizeable: Boolean = true,
	var borderless: Boolean = false,
	var fullscreen: Boolean = false,
	var x: Int = 32,
	var y: Int = 32,
	var width: Int = 1536,
	var height: Int = 864,
	var displayMode: Int = 0,
	var vsyncEnabled: Boolean = true,
	var maxFps: Int = 128,
	var videoDebug: Boolean = false,
	var masterVolume: Float = 0.5f,
	var musicVolume: Float = 1f,
	var soundVolume: Float = 1f,
	var metaDisplayMode: MetaDisplayMode = MetaDisplayMode(),
	var runWithUI: Boolean = true,
) {
	fun captureWindowedBounds() {
		if (Gdx.graphics.isFullscreen) return
		x = Meta.instance.windowHandler.x
		y = Meta.instance.windowHandler.y
		width = Gdx.graphics.width
		height = Gdx.graphics.height
	}

	fun apply() {
		if (!runWithUI) return

		if (fullscreen && Gdx.graphics.supportsDisplayModeChange()) {
			val monitor = getCurrentMonitor()
			val dm = displayModeFor(monitor, metaDisplayMode)
			metaDisplayMode = dm.toMetaDisplayMode()
			Gdx.graphics.setFullscreenMode(dm)
		} else {
			Gdx.graphics.setUndecorated(borderless)
			Gdx.graphics.setWindowedMode(width, height)
			Meta.instance.windowHandler.modify(x, y)
		}
		Gdx.graphics.setVSync(vsyncEnabled)
		// LWJGL3 treats 0 as "never sleep" (uncapped); map any non-positive value to that.
		// Applied regardless of vsync so the cap takes effect immediately if vsync is toggled off.
		Gdx.graphics.setForegroundFPS(if (maxFps > 0) maxFps else 0)
		Meta.instance.windowHandler.focus()
	}
}
