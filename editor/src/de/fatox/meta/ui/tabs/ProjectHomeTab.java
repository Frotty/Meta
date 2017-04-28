package de.fatox.meta.ui.tabs;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.api.dao.MetaProjectData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.components.TextWidget;
import de.fatox.meta.ui.windows.AssetDiscovererWindow;
import de.fatox.meta.ui.windows.ShaderComposerWindow;
import de.fatox.meta.ui.windows.ShaderLibraryWindow;

/**
 * Created by Frotty on 06.06.2016.
 */
public class ProjectHomeTab extends MetaTab {
    private final MetaProjectData projectData;
    private VisTable visTable = new VisTable();

    @Inject
    private MetaEditorUI editorUI;

    public ProjectHomeTab(MetaProjectData metaProjectData) {
        super(true, false);
        this.projectData = metaProjectData;
        setupTable();
    }

    private void setupTable() {
        visTable.top();
        visTable.row().height(128);
        visTable.add(new TextWidget(projectData.name));
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
    public void onShow() {
        super.onShow();
        editorUI.metaToolbar.clear();
        editorUI.metaToolbar.addAvailableWindow(AssetDiscovererWindow.class, null);
        editorUI.metaToolbar.addAvailableWindow(ShaderLibraryWindow.class, null);
        editorUI.metaToolbar.addAvailableWindow(ShaderComposerWindow.class, null);
    }
}
