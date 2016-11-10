package de.fatox.meta.api;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

public interface AssetProvider {
    boolean addAssetFolder(FileHandle folder);

    <T> void load(String name, Class<T> type);

    <T> T get (String fileName, Class<T> type);

    Drawable getDrawable(String name);

    void finish();
}
