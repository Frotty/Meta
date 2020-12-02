package de.fatox.meta.api.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics


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

	fun equalsDisplayMode(dm: Graphics.DisplayMode) =
		width == dm.width &&
			height == dm.height &&
			refreshRate == dm.refreshRate &&
			bitsPerPixel == dm.bitsPerPixel
}

private inline val Graphics.DisplayMode.monitorIndex
	get() = if (this is Lwjgl3Graphics.Lwjgl3DisplayMode) Gdx.graphics.monitors.indexOfFirst {
		if (it is Lwjgl3Graphics.Lwjgl3Monitor) {
			it.monitorHandle == monitor
		} else false
	}.coerceAtLeast(0) else 0

fun Graphics.DisplayMode.toMetaDisplayMode() = MetaDisplayMode(this)

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
	var metaDisplayMode: MetaDisplayMode = Gdx.graphics.displayMode.toMetaDisplayMode()
) {
	fun apply() {
		if (fullscreen && Gdx.graphics.supportsDisplayModeChange()) {
			val dms = Gdx.graphics.monitors.mapNotNull {
				Gdx.graphics.getDisplayModes(it).firstOrNull { metaDisplayMode.equalsDisplayMode(it) }
			}
			val dm = dms.firstOrNull { metaDisplayMode.monitorIndex == it.monitorIndex }
				?: dms.firstOrNull()
				?: Gdx.graphics.getDisplayMode(Gdx.graphics.primaryMonitor)

			metaDisplayMode = dm.toMetaDisplayMode()

			Gdx.graphics.setFullscreenMode(dm)
		} else {
			Gdx.graphics.setUndecorated(borderless)
			Gdx.graphics.setWindowedMode(width, height)
		}
		Gdx.graphics.setVSync(vsyncEnabled)
	}
}
