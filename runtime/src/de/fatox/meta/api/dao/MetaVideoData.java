package de.fatox.meta.api.dao;

import com.google.gson.annotations.Expose;

/**
 * Created by Frotty on 05.11.2016.
 */
public class MetaVideoData {
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
    private boolean vsyncEnabled = false;
    @Expose
    private boolean videoDebug = false;

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
}
