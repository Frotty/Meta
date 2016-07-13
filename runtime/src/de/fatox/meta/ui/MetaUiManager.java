package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.toast.ToastTable;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaEditorData;
import de.fatox.meta.api.dao.MetaWindowData;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 20.05.2016.
 */
public class MetaUiManager implements UIManager {
    @Inject
    private UIRenderer uiRenderer;
    @Inject
    private MetaEditorData editorData;

    private Array<Window> windowCache = new Array<>();
    private Array<Table> tables = new Array<>();
    private Array<MenuBar> menuBars = new Array<>();
    private Table contentTable = new Table();

    public MetaUiManager() {
        Meta.inject(this);
        contentTable.top().left();
        contentTable.setPosition(0, 0);
        contentTable.setFillParent(true);
        uiRenderer.getStage().addActor(contentTable);
    }

    @Override
    public void resize(int width, int height) {
        uiRenderer.resize(width, height);
    }

    @Override
    public void addTable(Table table, boolean gx, boolean gy) {
        contentTable.row();
        Cell<Table> add = contentTable.add(table).top();
        if (gx)
            add.growX();
        if (gy)
            add.growY();
    }

    @Override
    public void addWindow(VisWindow window, boolean startup) {
        MetaWindowData windowData = editorData.getWindowData(window);
        if(startup && !windowData.displayed) {
            return;
        }
        if(!startup) {
            windowData.displayed = true;
            editorData.write();
        }
        window.setSize(windowData.getWidth(), windowData.getHeight());
        window.setPosition(windowData.getX(), windowData.getY());
        if (!windowCache.contains(window, true)) {
            windowCache.add(window);
            uiRenderer.addActor(window);
        } else {
            window.fadeIn();
        }
    }

    @Override
    public void addMenuBar(MenuBar menuBar) {
        menuBars.add(menuBar);
        contentTable.row().height(26);
        contentTable.add(menuBar.getTable()).growX().top();
    }

    @Override
    public void addToast(ToastTable toast) {
    }
}
