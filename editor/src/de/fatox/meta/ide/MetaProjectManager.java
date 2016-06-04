package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.toast.ToastTable;
import de.fatox.meta.Meta;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 04.06.2016.
 */
public class MetaProjectManager implements ProjectManager {
    @Inject
    private Gson gson;
    @Inject
    private UIManager uiManager;

    public MetaProjectManager() {
        Meta.inject(this);
    }

    @Override
    public MetaProjectData loadProject(FileHandle projectFile) {
        MetaProjectData metaProjectData = gson.fromJson(projectFile.readString(), MetaProjectData.class);
        createFolders(metaProjectData);
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
        uiManager.addToast(toastTable);
    }

    private void createFolders(MetaProjectData projectData) {
        projectData.root.child("assets").mkdirs();
        projectData.root.child("scenes").mkdirs();
    }
}
