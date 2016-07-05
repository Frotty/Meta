package de.fatox.meta.dao;

import com.google.gson.annotations.Expose;
import de.fatox.meta.ui.windows.MetaWindow;

/**
 * Created by Frotty on 28.06.2016.
 */
public class MetaWindowData {
    @Expose
    private float x, y;

    @Expose
    private float width, height;

    public void set(MetaWindow metaWindow) {
        this.x = metaWindow.getX();
        this.y = metaWindow.getY();
        this.width = metaWindow.getWidth();
        this.height = metaWindow.getHeight();
    }
}
