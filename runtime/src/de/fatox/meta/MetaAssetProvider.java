package de.fatox.meta;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.ui.AssetPromise;

public class MetaAssetProvider implements AssetProvider {
    private Array<FileHandle> assetFolders = new Array<>();
    private TextureLoader.TextureParameter textureParam;
    private AssetManager assetManager = new AssetManager();

    public MetaAssetProvider() {
        textureParam = new TextureLoader.TextureParameter();
        textureParam.magFilter = Texture.TextureFilter.Linear;
        textureParam.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        textureParam.genMipMaps = true;
        assetManager.load("textures/ui/rotate_clockwise.png", Texture.class);
        finish();
    }

    @Override
    public boolean addAssetFolder(FileHandle folder) {
        if(folder.isDirectory()) {
            assetFolders.add(folder);
            return true;
        }
        return false;
    }

    @Override
    public Drawable getDrawable(String texturename) {
        return new TextureRegionDrawable(new TextureRegion(get(texturename, Texture.class)));
    }

    @Override
    public <T> T get(String fileName, Class<T> type) {
        return assetManager.get(fileName, type);
    }

    @Override
    public <T> void load(String fileName, Class<T> type) {
        if (type == Texture.class) {
            assetManager.load(fileName, type);
        } else {
            assetManager.load(fileName, type);
        }
    }


    @Override
    public <T> void load(String fileName, Class<T> type, AssetLoaderParameters<T> parameter) {
        assetManager.load(fileName, type, parameter);
    }

    @Override
    public FileHandle get(String s) {
        return Gdx.files.internal(s);
    }

    @Override
    public void finish() {
        assetManager.finishLoading();
    }

    @Override
    public <T> AssetPromise<T> getPromise(String name, Class<T> type) {
        return new AssetPromise<>(get("textures/ui/rotate_clockwise.png", type));
    }

}
