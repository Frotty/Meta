package de.fatox.meta.api.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import de.fatox.meta.Meta
import kotlin.math.abs


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
	val meta = Gdx.app.applicationListener as Meta

	var currentMonitor = Gdx.graphics.primaryMonitor // Default to primary monitor
	var minDistance = Int.MAX_VALUE // To find the closest monitor

	for (monitor in Gdx.graphics.monitors) {
		val mode = Gdx.graphics.getDisplayMode(monitor)
		// Calculate the center of the monitor
		val monitorCenterX = monitor.virtualX + mode.width / 2
		val monitorCenterY = monitor.virtualY + mode.height / 2
		// Calculate distance from window's top-left corner to monitor's center
		val distance = (abs((monitorCenterX - meta.windowHandler.x).toDouble()) + abs((monitorCenterY - meta.windowHandler.y).toDouble())).toInt()

		if (distance < minDistance) {
			minDistance = distance
			currentMonitor = monitor
		}
	}
	return currentMonitor
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
	var width: Int = 1280,
	var height: Int = 720,
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
	fun apply() {
		if (!runWithUI) return

		if (fullscreen && Gdx.graphics.supportsDisplayModeChange()) {

			val dm = Gdx.graphics.getDisplayMode(getCurrentMonitor())
			metaDisplayMode = dm.toMetaDisplayMode()

			Gdx.graphics.setFullscreenMode(dm)
		} else {
			Gdx.graphics.setUndecorated(borderless)
			Gdx.graphics.setWindowedMode(width, height)
		}
		Gdx.graphics.setVSync(vsyncEnabled)
	}
}
