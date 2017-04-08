package de.fatox.meta.api.dao;

import com.badlogic.gdx.Gdx;

/**
 * Created by Frotty on 05.11.2016.
 */
public class MetaAudioVideoData {
    public boolean resizeable = true;
    public boolean borderless = false;
    public boolean fullscreen = false;
    public int width = 1280, height = 720;
    public int displayMode = 0;
    public boolean vsyncEnabled = true;
    public boolean videoDebug = false;

    public float masterVolume = 0.5f;
    public float musicVolume = 1f;
    public float soundVolume = 1f;

    public void apply() {
        if (fullscreen) {
            if (!Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayModes()[displayMode]);
            }
        } else {
            Gdx.graphics.setUndecorated(borderless);
            Gdx.graphics.setWindowedMode(width, height);
        }
        Gdx.graphics.setVSync(vsyncEnabled);
    }
}
