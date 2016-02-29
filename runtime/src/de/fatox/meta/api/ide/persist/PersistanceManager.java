package de.fatox.meta.api.ide.persist;

import com.badlogic.gdx.files.FileHandle;

public interface PersistanceManager {

    FileHandle injectObject(Object object, int id);

    void deleteObject(Object object);
}
