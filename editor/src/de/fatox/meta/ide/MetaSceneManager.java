package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import de.fatox.meta.Meta;
import de.fatox.meta.dao.MetaSceneData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.tabs.SceneTab;

/**
 * Created by Frotty on 15.06.2016.
 */
public class MetaSceneManager implements SceneManager {
    @Inject
    private Gson gson;
    @Inject
    private MetaEditorUI metaEditorUI;

    public MetaSceneManager() {
        Meta.inject(this);
    }

    @Override
    public MetaSceneData createNew(String name) {
        MetaSceneData metaSceneData = new MetaSceneData(name);
        gson.toJson(metaSceneData);
        metaEditorUI.addTab(new SceneTab(metaSceneData));
        return metaSceneData;
    }

    @Override
    public MetaSceneData loadScene(FileHandle projectFile) {
        return gson.fromJson(projectFile.readString(), MetaSceneData.class);
    }

    @Override
    public void saveScene(MetaSceneData sceneData) {

    }
}
