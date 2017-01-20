package de.fatox.meta.api.dao;

import com.google.gson.annotations.Expose;

/**
 * Created by Frotty on 05.11.2016.
 */
public class MetaAudioVideoData {
    @Expose
    private boolean resizeable = true;
    @Expose
    private boolean borderless = false;
    @Expose
    private boolean fullscreen = false;
    @Expose
    private int width = 1280, height = 720;
    @Expose
    private int displayMode = 0;
    @Expose
    private boolean vsyncEnabled = true;
    @Expose
    private boolean videoDebug = false;

    @Expose
    private float masterVolume = 0.5f;
    @Expose
    private float musicVolume = 1f;
    @Expose
    private float soundVolume = 1f;

    public boolean isResizeable() {
        return resizeable;
    }

    public void setResizeable(boolean resizeable) {
        this.resizeable = resizeable;
    }

    public boolean isBorderless() {
        return borderless;
    }

    public void setBorderless(boolean borderless) {
        this.borderless = borderless;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(int displayMode) {
        this.displayMode = displayMode;
    }

    public void setVsyncEnabled(boolean vsyncEnabled) {
        this.vsyncEnabled = vsyncEnabled;
    }

    public boolean isVsyncEnabled() {
        return vsyncEnabled;
    }

    public boolean isVideoDebug() {
        return videoDebug;
    }

    public void setVideoDebug(boolean videoDebug) {
        this.videoDebug = videoDebug;
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float masterVolume) {
        this.masterVolume = masterVolume;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = musicVolume;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = soundVolume;
    }
}
