package de.fatox.meta.ui.tabs;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.dao.MetaProjectData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.components.TextWidget;
import de.fatox.meta.ui.windows.AssetDiscovererWindow;
import de.fatox.meta.ui.windows.ShaderLibraryWindow;

/**
 * Created by Frotty on 06.06.2016.
 */
public class ProjectHomeTab extends MetaTab {
    private final MetaProjectData projectData;
    private VisTable visTable = new VisTable();

    @Inject
    private UIManager uiManager;
    @Inject
    private AssetDiscovererWindow assetDiscovererWindow;
    @Inject
    private ShaderLibraryWindow shaderLibraryWindow;

    public ProjectHomeTab(MetaProjectData metaProjectData) {
        super(true, true);
        this.projectData = metaProjectData;
        setupTable(metaProjectData);
        uiManager.addWindow(assetDiscovererWindow);
        uiManager.addWindow(shaderLibraryWindow);
        assetDiscovererWindow.refresh();
    }

    private void setupTable(MetaProjectData metaProjectData) {
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

    @Override
    public void onDisplay() {
    }
}
