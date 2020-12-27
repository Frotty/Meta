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
	var metaDisplayMode: MetaDisplayMode = MetaDisplayMode()
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
