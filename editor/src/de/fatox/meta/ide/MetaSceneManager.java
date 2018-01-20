package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
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
        MetaSceneData metaSceneData = new MetaSceneData(name, projectManager.relativize(currentComposition.getCompositionHandle()), Vector3.Y, true);
        FileHandle sceneFile = projectManager.getCurrentProjectRoot().child(FOLDER + name + "." + EXTENSION);
        sceneFile.writeBytes(json.toJson(metaSceneData).getBytes(), false);
        MetaSceneHandle metaSceneHandle = new MetaSceneHandle(metaSceneData, shaderComposer.getCurrentComposition(), sceneFile);
        metaEditorUI.addTab(new SceneTab(metaSceneHandle));
        return metaSceneHandle;
    }

    @Override
    public void loadScene(FileHandle sceneFile) {
        if (metaEditorUI.hasTab(sceneFile.name())) {
            metaEditorUI.focusTab(sceneFile.name());
            return;
        }
        MetaSceneData metaSceneData = json.fromJson(MetaSceneData.class, sceneFile.readString());
        ShaderComposition composition = shaderComposer.getComposition(metaSceneData.getCompositionPath());
        metaEditorUI.addTab(new SceneTab(new MetaSceneHandle(metaSceneData, composition, sceneFile)));
    }

    @Override
    public void saveScene(MetaSceneData sceneData) {

    }
}
