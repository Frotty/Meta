package de.fatox.meta.api.model

import com.badlogic.gdx.Gdx

/**
 * Created by Frotty on 05.11.2016.
 */
data class MetaAudioVideoData(var profile: String = "default") {
	var hd = true
    var resizeable = true
    var borderless = false
    var fullscreen = false
    var x = 32
    var y = 32
    var width = 1280
    var height = 720
    var displayMode = 0
    var vsyncEnabled = true
	var maxFps = 128;
    var videoDebug = false

    var masterVolume = 0.5f
    var musicVolume = 1f
    var soundVolume = 1f

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
