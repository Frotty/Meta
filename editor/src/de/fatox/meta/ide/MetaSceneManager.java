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
    private static final String EXTENSION = "metascene";
    @Inject
    private ProjectManager projectManager;
    @Inject
    private MetaEditorUI metaEditorUI;
    @Inject
    private AssetDiscoverer assetDiscoverer;
    @Inject
    private Json json;

    public MetaSceneManager() {
        Meta.inject(this);
        assetDiscoverer.addOpenListener(EXTENSION, this::loadScene);
    }

    @Override
    public MetaSceneData createNew(String name) {
        MetaSceneData metaSceneData = new MetaSceneData(name);
        projectManager.getCurrentProject().root.child(FOLDER + name + "." + EXTENSION).writeBytes(json.toJson(metaSceneData).getBytes(), false);
        metaEditorUI.addTab(new SceneTab(metaSceneData));
        return metaSceneData;
    }

    @Override
    public void loadScene(FileHandle projectFile) {
        if(metaEditorUI.hasTab(projectFile.name())) {
            metaEditorUI.focusTab(projectFile.name());
        }
        metaEditorUI.addTab(new SceneTab(json.fromJson(MetaSceneData.class, projectFile.readString())));
    }

    @Override
    public void saveScene(MetaSceneData sceneData) {

    }
}
