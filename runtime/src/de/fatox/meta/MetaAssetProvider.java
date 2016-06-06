package de.fatox.meta;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.BitmapFontLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import de.fatox.meta.api.AssetProvider;

public class MetaAssetProvider implements AssetProvider {
    private AssetManager assetManager = new AssetManager();

    public MetaAssetProvider() {
        BitmapFontLoader.BitmapFontParameter fontParam = new BitmapFontLoader.BitmapFontParameter();
        fontParam.genMipMaps = true;
        fontParam.magFilter = Texture.TextureFilter.Linear;
        fontParam.minFilter = Texture.TextureFilter.MipMapLinearLinear;

        TextureLoader.TextureParameter textParam = new TextureLoader.TextureParameter();
        textParam.magFilter = Texture.TextureFilter.Linear;
        textParam.minFilter = Texture.TextureFilter.MipMapLinearLinear;

        load("models/white.png", Texture.class);
        load("models/sphere.g3db", Model.class);
        load("models/cryofan.g3db", Model.class);
        load("models/CryoFanNM.jpg", Texture.class);
        load("ui/appbar.new.png", Texture.class);
        load("ui/appbar.folder.open.png", Texture.class);
        assetManager.finishLoading();
    }

    @Override
    public <T> T get(String fileName, Class<T> type) {
        return assetManager.get(fileName, type);
    }

    @Override
    public <T> void load(String fileName, Class<T> type) {
        assetManager.load(fileName, type);
    }

    @Override
    public <T> void load(String fileName, Class<T> type, AssetLoaderParameters<T> parameter) {
        assetManager.load(fileName, type, parameter);
    }

    @Override
    public FileHandle get(String s) {
        return Gdx.files.internal(s);
    }
}
