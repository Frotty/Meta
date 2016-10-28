package de.fatox.meta.api;

import com.badlogic.gdx.files.FileHandle;

public interface AssetProvider {
    boolean addAssetFolder(FileHandle folder);

    <T> void load(String name, Class<T> type);

    <T> T get (String fileName, Class<T> type);

    void finish();
}
