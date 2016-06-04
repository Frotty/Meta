package de.fatox.meta.api;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.files.FileHandle;

public interface AssetProvider {
    <T> T get(String fileName, Class<T> type);
    <T> void load(String fileName, Class<T> type);
    <T> void load (String fileName, Class<T> type, AssetLoaderParameters<T> parameter);

    FileHandle get(String s);
}
