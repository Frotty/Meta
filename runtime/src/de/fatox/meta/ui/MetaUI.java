package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.widget.MenuBar;

/**
 * Created by Frotty on 19.05.2016.
 */
public class MetaUI {
    private Array<MetaWindow> windowCache = new Array<>();
    private Array<MenuBar> menuBars = new Array<>();
    private Stage stage;

    public MetaUI() {
    }

    public void refresh() {
        stage = new Stage(new ScreenViewport());
    }

    public void addMenubar(MenuBar menuBar) {
        menuBars.add(menuBar);
        stage.addActor(menuBar.getTable());
    }

    public void addWindow(MetaWindow metaWindow) {
        // TODO move away from stuff
        stage.addActor(metaWindow);
    }

}
