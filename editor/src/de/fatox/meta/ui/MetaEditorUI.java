package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter;
import de.fatox.meta.Meta;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.tabs.WelcomeTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Frotty on 04.06.2016.
 */
public class MetaEditorUI {
	private static final Logger log = LoggerFactory.getLogger(MetaEditorUI.class);
    @Inject
    private UIManager uiManager;

    public EditorMenuBar metaToolbar;

    private TabbedPane tabbedPane;
    private Table tabTable = new Table();

    public MetaEditorUI() {
        Meta.inject(this);
    }

    public void setup() {
        metaToolbar = new EditorMenuBar();
        log.info("Toolbar created");
        uiManager.setMainMenuBar(metaToolbar.getMenuBar());

        tabbedPane = new TabbedPane();
        tabbedPane.addListener(new TabbedPaneAdapter() {
            @Override
            public void switchedTab(Tab tab) {
                tabbedPane.getActiveTab().onHide();
                uiManager.changeScreen(tab.getClass().getName());
                apply();
                Table content = tab.getContentTable();
                tabTable.clearChildren();
                tabTable.add(content).grow();
                tabTable.toFront();
                content.toBack();
                uiManager.bringWindowsToFront();
            }

        });
        VisTable visTable = new VisTable();
        visTable.add().top();
        visTable.add().grow();
        addTab(new WelcomeTab());
    }

    public void apply() {
        uiManager.addTable(tabbedPane.getTable(), true, false);
        uiManager.addTable(tabTable, true, true);
    }

    public void addTab(Tab tab) {
        tabbedPane.add(tab);
    }

    public boolean hasTab(String name) {
        return getTab(name) != null;
    }

    private Tab getTab(String name) {
        for (Tab tab : tabbedPane.getTabs()) {
            if (tab.getTabTitle().equalsIgnoreCase(name)) {
                return tab;
            }
        }
        return null;
    }

    public Tab getCurrentTab() {
        return tabbedPane.getActiveTab();
    }

    public void focusTab(String name) {
        if (hasTab(name)) {
            tabbedPane.switchTab(getTab(name));
        }
    }

    public void closeTab(String name) {
        if (hasTab(name)) {
            getTab(name).removeFromTabPane();
        }
    }
}
