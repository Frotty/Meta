package de.fatox.meta.ide;

import com.badlogic.gdx.files.FileHandle;
import de.fatox.meta.api.dao.MetaSceneData;

/**
 * Created by Frotty on 15.06.2016.
 */
public interface SceneManager {

    MetaSceneData createNew(String name);

    MetaSceneData loadScene(FileHandle projectFile);

    void saveScene(MetaSceneData sceneData);
}
