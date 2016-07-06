package de.fatox.meta.api.dao;

import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.google.gson.annotations.Expose;

/**
 * Created by Frotty on 28.06.2016.
 */
public class MetaWindowData {
    @Expose
    public String name;
    @Expose
    private float x, y;

    @Expose
    private float width, height;

    public MetaWindowData() {
    }

    public MetaWindowData(Window metaWindow) {
        this.name = metaWindow.getTitleLabel().getText().toString();
        this.x = metaWindow.getX();
        this.y = metaWindow.getY();
        this.width = metaWindow.getWidth();
        this.height = metaWindow.getHeight();
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setFrom(Window metaWindow) {
        this.x = metaWindow.getX();
        this.y = metaWindow.getY();
        this.width = metaWindow.getWidth();
        this.height = metaWindow.getHeight();
    }
}
