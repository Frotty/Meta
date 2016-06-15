package de.fatox.meta.ui.tabs;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import de.fatox.meta.Meta;
import de.fatox.meta.dao.MetaProjectData;
import de.fatox.meta.ui.components.TextWidget;

/**
 * Created by Frotty on 06.06.2016.
 */
public class ProjectHomeTab extends Tab {
    private final MetaProjectData projectData;
    private VisTable visTable = new VisTable();

    public ProjectHomeTab(MetaProjectData metaProjectData) {
        super(true, true);
        Meta.inject(this);
        this.projectData = metaProjectData;
        visTable.top();
        visTable.row().height(128);
        visTable.add(new TextWidget(metaProjectData.name));
        visTable.row().height(64);
        visTable.add();
        visTable.row();
        VisLabel visLabel = new VisLabel("This is your project home tab.");
        visLabel.setAlignment(Align.center);
        visTable.add(visLabel).padBottom(128);
    }

    @Override
    public String getTabTitle() {
        return "home@" + projectData.name;
    }

    @Override
    public Table getContentTable() {
        return visTable;
    }
}
