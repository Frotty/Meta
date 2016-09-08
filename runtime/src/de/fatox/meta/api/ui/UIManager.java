package de.fatox.meta.api.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.kotcrab.vis.ui.widget.MenuBar;

/**
 * Created by Frotty on 20.05.2016.
 */
public interface UIManager {
    void resize(int width, int height);

    /**
     * Indicates a screen change. This will remove/modify the elements of the current screen
     * and load the saved elements
     *
     * @param screenIdentifier name of the screen for the json persitence
     */
    void changeScreen(String screenIdentifier);

    void addTable(Table table, boolean gx, boolean gy);

    /**
     * @param windowClass The window to show
     */
    <T extends Window> T showWindow(Class<? extends T> windowClass);

    void addMenuBar(MenuBar menuBar);

    <T extends Window> T  getWindow(Class<? extends T> windowClass);

}
