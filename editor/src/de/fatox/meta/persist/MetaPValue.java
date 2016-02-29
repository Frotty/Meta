package de.fatox.meta.persist;

import com.badlogic.gdx.files.FileHandle;
import de.fatox.meta.api.ide.persist.PersistentValue;

public class MetaPValue<VALUETYPE> implements PersistentValue<VALUETYPE> {
    private VALUETYPE value;
    private FileHandle persistFile;
    private String key;

    public MetaPValue(FileHandle persistFile) {
        this.persistFile = persistFile;
    }

    @Override
    public VALUETYPE get() {
        return value;
    }

    @Override
    public void set(VALUETYPE value) {
        this.value = value;
    }

}
