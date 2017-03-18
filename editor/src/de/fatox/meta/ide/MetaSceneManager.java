package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaSceneData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.tabs.SceneTab;

import java.io.File;

/**
 * Created by Frotty on 15.06.2016.
 */
public class MetaSceneManager implements SceneManager {
    private static final String FOLDER = "scenes" + File.separator;
    @Inject
    private ProjectManager projectManager;
    @Inject
    private MetaEditorUI metaEditorUI;
    @Inject
    private Json json;

    public MetaSceneManager() {
        Meta.inject(this);
    }

    @Override
    public MetaSceneData createNew(String name) {
        MetaSceneData metaSceneData = new MetaSceneData(name);
        projectManager.getCurrentProject().root.child(FOLDER + name).writeBytes(json.toJson(metaSceneData).getBytes(), false);
        metaEditorUI.addTab(new SceneTab(metaSceneData));
        return metaSceneData;
    }

    @Override
    public MetaSceneData loadScene(FileHandle projectFile) {
        return json.fromJson(MetaSceneData.class, projectFile.readString());
    }

    @Override
    public void saveScene(MetaSceneData sceneData) {

    }
}
