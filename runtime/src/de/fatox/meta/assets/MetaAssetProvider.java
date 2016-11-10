package de.fatox.meta.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.api.AssetProvider;

public class MetaAssetProvider implements AssetProvider {
    private TextureLoader.TextureParameter defaultTexParam = new TextureLoader.TextureParameter();
    private AssetManager assetManager = new AssetManager();
    private Array<FileHandle> assetFolders = new Array<>();
    private Array<TextureAtlas> atlasCache = new Array<>();


    @Override
    public boolean addAssetFolder(FileHandle folder) {
        if (folder.isDirectory()) {
            assetFolders.add(folder);
            return true;
        }
        return false;
    }

    @Override
    public <T> void load(String name, Class<T> type) {
        assetManager.load(name, type);
        assetManager.finishLoading();
        if (type == TextureAtlas.class) {
            atlasCache.add(assetManager.get(name, TextureAtlas.class));
        }
    }


    @Override
    public <T> T get(String name, Class<T> type) {
        if (assetManager.isLoaded(name, type)) {
            return assetManager.get(name, type);
        } else if (type == TextureRegion.class) {
            for (TextureAtlas atlas : atlasCache) {
                TextureAtlas.AtlasRegion region = atlas.findRegion(name);
                if (region != null) {
                    return (T) region;
                }
            }
        } else {
            load(name, type);
            return get(name, type);
        }
        return null;
    }

    @Override
    public Drawable getDrawable(String name) {
        return new TextureRegionDrawable(get(name, TextureRegion.class));
    }

    @Override
    public void finish() {
        assetManager.finishLoading();
    }

}
