package de.fatox.meta.api.dao;

import com.badlogic.gdx.scenes.scene2d.ui.Window;
import de.fatox.meta.ui.windows.MetaDialog;

/**
 * Created by Frotty on 28.06.2016.
 */
public class MetaWindowData {
    public String name;
    public float x, y;

    public float width, height;
    public boolean displayed = false;
    public boolean dialog = false;

    public MetaWindowData() {
    }

    public MetaWindowData(Window metaWindow) {
        setFrom(metaWindow);
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
