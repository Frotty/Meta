package de.fatox.meta.api.model

import com.badlogic.gdx.Gdx

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
	var soundVolume: Float = 1f
) {

	fun apply() {
		if (fullscreen) {
			if (!Gdx.graphics.isFullscreen) {
				Gdx.graphics.setFullscreenMode(Gdx.graphics.displayModes[displayMode])
			}
		} else {
			Gdx.graphics.setUndecorated(borderless)
			Gdx.graphics.setWindowedMode(width, height)
		}
		Gdx.graphics.setVSync(vsyncEnabled)
	}
}
