package de.fatox.meta.api;

import com.badlogic.gdx.assets.AssetLoaderParameters;

public interface AssetProvider {
    <T> T get (String fileName, Class<T> type);
    <T> void load (String fileName, Class<T> type);
    <T> void load (String fileName, Class<T> type, AssetLoaderParameters<T> parameter);
}
