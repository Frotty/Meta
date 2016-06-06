package de.fatox.meta.ui.tabs;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import de.fatox.meta.ui.components.TextWidget;

/**
 * Created by Frotty on 05.06.2016.
 */
public class WelcomeTab extends Tab {
    private VisTable visTable = new VisTable();

    public WelcomeTab() {
        super(false, false);
        visTable.top();
        visTable.row().height(128);
        visTable.add(new TextWidget("Meta"));
        visTable.row().height(64);
        visTable.add();
        visTable.row();
        visTable.add(new VisLabel("Welcome to the Meta Engine\nCreate or load a project"));
    }

    @Override
    public String getTabTitle() {
        return "Home";
    }

    @Override
    public Table getContentTable() {
        return visTable;
    }
}
