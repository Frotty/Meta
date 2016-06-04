package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.toast.ToastTable;
import de.fatox.meta.Meta;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 20.05.2016.
 */
public class MetaUiManager implements UIManager {
    @Inject
    private UIRenderer uiRenderer;

    private Array<Window> windowCache = new Array<>();
    private Array<Table> tables = new Array<>();
    private Array<MenuBar> menuBars = new Array<>();

    public MetaUiManager() {
        Meta.inject(this);
    }

    @Override
    public void update() {
        uiRenderer.update();
    }

    @Override
    public void draw() {
        uiRenderer.draw();
    }

    @Override
    public void resize(int width, int height) {
        uiRenderer.resize(width, height);
    }

    @Override
    public void addTable(Table table) {
        uiRenderer.getStage().addActor(table);
    }

    @Override
    public void addWindow(Window window) {
        windowCache.add(window);
        uiRenderer.addActor(window);
    }

    @Override
    public void addMenuBar(MenuBar menuBar) {
        RTable rTable = new RTable();
        rTable.setFillParent(true);
        rTable.top().row();
        rTable.add(menuBar.getTable()).fillX().expandX();
        rTable.row().height(0.5f);
        rTable.add();
        menuBars.add(menuBar);
        uiRenderer.addActor(rTable);
    }

    @Override
    public void addToast(ToastTable toast) {
    }
}
