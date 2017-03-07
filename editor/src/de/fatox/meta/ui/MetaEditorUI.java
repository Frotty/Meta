package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.ui.tabs.MetaTab;
import de.fatox.meta.ui.tabs.WelcomeTab;

/**
 * Created by Frotty on 04.06.2016.
 */
public class MetaEditorUI {
    private static final String TAG = "MetaEditorUI";
    @Inject
    @Log
    private Logger log;
    @Inject
    private UIManager uiManager;

    private TabbedPane tabbedPane;
    private Table tabTable = new Table();

    public MetaEditorUI() {
        Meta.inject(this);
    }

    public void setup() {
        EditorMenuBar metaToolbar = new EditorMenuBar();
        log.info(TAG, "Toolbar created");
        uiManager.addMenuBar(metaToolbar.menuBar);

        tabbedPane = new TabbedPane();
        tabbedPane.addListener(new TabbedPaneAdapter() {
            @Override
            public void switchedTab(Tab tab) {
                ((MetaTab)tab).onDisplay();
                Table content = tab.getContentTable();

                tabTable.clearChildren();
                tabTable.add(content).expand().fill();
            }

        });
        VisTable visTable = new VisTable();
        visTable.add().top();
        visTable.add().grow();
        addTab(new WelcomeTab());
        uiManager.addTable(tabbedPane.getTable(), true, false);
        uiManager.addTable(tabTable, true, true);
    }

    public void addTab(Tab tab) {
        tabbedPane.add(tab);
    }
}
