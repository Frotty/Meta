package de.fatox.meta.api;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import de.fatox.meta.api.ui.AssetPromise;

public interface AssetProvider {
    boolean addAssetFolder(FileHandle folder);

    Drawable getDrawable(String texturename);

    <T> T get(String fileName, Class<T> type);
    <T> void load(String fileName, Class<T> type);
    <T> void load (String fileName, Class<T> type, AssetLoaderParameters<T> parameter);

    FileHandle get(String s);

    void finish();

    <T> AssetPromise<T> getPromise(String name, Class<T> type);
}
