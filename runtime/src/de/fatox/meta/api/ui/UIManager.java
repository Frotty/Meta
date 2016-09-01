package de.fatox.meta.api.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.VisWindow;

/**
 * Created by Frotty on 20.05.2016.
 */
public interface UIManager {
    void resize(int width, int height);

    /**
     * Indicates a screen change. This will remove/modify the elements of the current screen
     * and load the saved elements
     * @param screenIdentifier name of the screen for the json persitence
     */
    void changeScreen(String screenIdentifier);

    void addTable(Table table, boolean gx, boolean gy);

    /**
     *
     * @param window the window to add
     * @param startup is this an event-driven add or does it happen at startup
     */
    void addWindow(VisWindow window, boolean startup);

    void addMenuBar(MenuBar menuBar);

}
