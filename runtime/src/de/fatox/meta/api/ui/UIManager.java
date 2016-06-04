package de.fatox.meta.api.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.toast.ToastTable;

/**
 * Created by Frotty on 20.05.2016.
 */
public interface UIManager {
    void update();

    void draw();

    void resize(int width, int height);

    void addTable(Table table);

    void addWindow(Window window);

    void addMenuBar(MenuBar menuBar);

    void addToast(ToastTable toast);
}
