package de.fatox.meta.api.dao;

import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.google.gson.annotations.Expose;
import de.fatox.meta.ui.windows.MetaDialog;

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
    @Expose
    public boolean displayed = false;
    @Expose
    public boolean dialog = false;

    public MetaWindowData() {
    }

    public MetaWindowData(Window metaWindow) {
        setFrom(metaWindow);
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
        this.dialog = MetaDialog.class.isInstance(metaWindow);
        if (! dialog) {
            displayed = true;
        }
    }

    public void set(Window metaWindow) {
        metaWindow.setPosition(x, y);
        metaWindow.setSize(width, height);
    }
}
