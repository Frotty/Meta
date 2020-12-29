package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import de.fatox.meta.api.model.MetaSceneData;
import de.fatox.meta.shader.MetaSceneHandle;

/**
 * Created by Frotty on 15.06.2016.
 */
public interface SceneManager {

    MetaSceneHandle createNew(String name);

    void loadScene(FileHandle projectFile);

    void saveScene(MetaSceneData sceneData);
}
