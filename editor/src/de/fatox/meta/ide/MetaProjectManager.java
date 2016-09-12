package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.toast.ToastTable;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.api.dao.MetaProjectData;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.tabs.ProjectHomeTab;
import de.fatox.meta.ui.windows.AssetDiscovererWindow;

/**
 * Created by Frotty on 04.06.2016.
 */
public class MetaProjectManager implements ProjectManager {
    @Inject
    private Gson gson;
    @Inject
    private UIManager uiManager;
    @Inject
    private MetaEditorUI editorUI;
    @Inject
    private AssetDiscoverer assetDiscoverer;
    @Inject
    private MetaData metaData;

    private MetaProjectData currentProject;

    public MetaProjectManager() {
        Meta.inject(this);
    }

    @Override
    public MetaProjectData getCurrentProject() {
        return currentProject;
    }

    @Override
    public MetaProjectData loadProject(FileHandle projectFile) {
        MetaProjectData metaProjectData = gson.fromJson(projectFile.readString(), MetaProjectData.class);
        metaProjectData.setRoot(projectFile.parent());
        createFolders(metaProjectData);
        currentProject = metaProjectData;
        assetDiscoverer.setFromProject(currentProject);
        editorUI.addTab(new ProjectHomeTab(metaProjectData));
        metaData.addLastProject(projectFile.path());
        return metaProjectData;
    }

    @Override
    public void saveProject(MetaProjectData projectData) {
        FileHandle root = projectData.root;
        if(! root.exists()) {
            root.mkdirs();
        }
        FileHandle child = root.child(projectData.name);
        child.mkdirs();
        projectData.setRoot(child);

        createFolders(projectData);
        child = child.child(MetaProjectData.PROJECT_FILE_NAME);
        child.writeBytes(gson.toJson(projectData).getBytes(), false);
        ToastTable toastTable = new ToastTable();
        toastTable.add(new VisLabel("Project created"));
    }

    @Override
    public boolean verifyProjectFile(FileHandle file) {
        try {
            MetaProjectData metaProjectData = gson.fromJson(file.readString(), MetaProjectData.class);
            if(metaProjectData.isValid()) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }


    private void createFolders(MetaProjectData projectData) {
        projectData.root.child("assets").mkdirs();
        projectData.root.child("scenes").mkdirs();
    }
}
