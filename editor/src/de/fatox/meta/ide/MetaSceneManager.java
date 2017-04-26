package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaSceneData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.shader.MetaSceneHandle;
import de.fatox.meta.shader.MetaShaderComposer;
import de.fatox.meta.shader.ShaderComposition;
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
    private MetaShaderComposer shaderComposer;
    @Inject
    private Json json;

    public MetaSceneManager() {
        Meta.inject(this);
        assetDiscoverer.addOpenListener(EXTENSION, this::loadScene);
    }

    @Override
    public MetaSceneHandle createNew(String name) {
        ShaderComposition currentComposition = shaderComposer.getCurrentComposition();
        MetaSceneData metaSceneData = new MetaSceneData(name, currentComposition.getCompositionHandle().path());
        projectManager.getCurrentProjectRoot().child(FOLDER + name + "." + EXTENSION).writeBytes(json.toJson(metaSceneData).getBytes(), false);
        MetaSceneHandle metaSceneHandle = new MetaSceneHandle(metaSceneData, shaderComposer.getCurrentComposition());
        metaEditorUI.addTab(new SceneTab(metaSceneHandle));
        return metaSceneHandle;
    }

    @Override
    public void loadScene(FileHandle projectFile) {
        if(metaEditorUI.hasTab(projectFile.name())) {
            metaEditorUI.focusTab(projectFile.name());
        }
        MetaSceneData metaSceneData = json.fromJson(MetaSceneData.class, projectFile.readString());
        ShaderComposition composition = shaderComposer.getComposition(metaSceneData.compositionPath);
        if(composition != null) {
            metaEditorUI.addTab(new SceneTab(new MetaSceneHandle(metaSceneData, composition)));
        }
    }

    @Override
    public void saveScene(MetaSceneData sceneData) {

    }
}
