package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.toast.ToastTable;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.api.dao.MetaProjectData;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.tabs.ProjectHomeTab;

/**
 * Created by Frotty on 04.06.2016.
 */
public class MetaProjectManager implements ProjectManager {
    @Inject
    private Json json;
    @Inject
    private UIManager uiManager;
    @Inject
    private MetaEditorUI editorUI;
    @Inject
    private AssetDiscoverer assetDiscoverer;
    @Inject
    private MetaData metaData;

    private MetaProjectData currentProject;
    private FileHandle currentProjectRoot;

    public MetaProjectManager() {
        Meta.inject(this);
    }

    @Override
    public MetaProjectData getCurrentProject() {
        return currentProject;
    }

    @Override
    public MetaProjectData loadProject(FileHandle projectFile) {
        MetaProjectData metaProjectData = json.fromJson(MetaProjectData.class, projectFile.readString());
        currentProjectRoot = projectFile.parent();
        createFolders(metaProjectData);
        currentProject = metaProjectData;
        editorUI.closeTab("home");
        editorUI.addTab(new ProjectHomeTab(metaProjectData));
        assetDiscoverer.setRoot(currentProjectRoot);
        Array<String> lastProjects;
        if (metaData.has("lastProjects")) {
            lastProjects = metaData.get("lastProjects", Array.class);
        } else {
            lastProjects = new Array<>();
        }
        if (!lastProjects.contains(projectFile.path(), false)) {
            lastProjects.add(projectFile.path());
            metaData.save("lastProjects", lastProjects);
        }
        return metaProjectData;
    }

    @Override
    public void saveProject(MetaProjectData projectData) {
        FileHandle root = currentProjectRoot;
        if (!root.exists()) {
            root.mkdirs();
        }
        FileHandle child = root.child(projectData.name);
        child.mkdirs();
        currentProjectRoot = child;

        createFolders(projectData);
        child = child.child(MetaProjectData.PROJECT_FILE_NAME);
        child.writeBytes(json.toJson(projectData).getBytes(), false);
        ToastTable toastTable = new ToastTable();
        toastTable.add(new VisLabel("Project created"));
    }

    @Override
    public boolean verifyProjectFile(FileHandle file) {
        try {
            MetaProjectData metaProjectData = json.fromJson(MetaProjectData.class, file.readString());
            if (metaProjectData.isValid()) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public FileHandle getCurrentProjectRoot() {
        return currentProjectRoot;
    }


    private void createFolders(MetaProjectData projectData) {
        currentProjectRoot.child("assets").mkdirs();
        currentProjectRoot.child("scenes").mkdirs();
    }
}
