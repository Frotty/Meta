package de.fatox.meta.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import de.fatox.meta.api.AssetProvider;

public class MetaAssetProvider implements AssetProvider {
    private static TextureLoader.TextureParameter defaultTexParam = new TextureLoader.TextureParameter();
    private static ModelLoader.ModelParameters defaultModelParam = new ModelLoader.ModelParameters();
    private AssetManager assetManager = new AssetManager();
    private Array<FileHandle> assetFolders = new Array<>();
    private Array<TextureAtlas> atlasCache = new Array<>();
    private IntMap<Array<? extends TextureRegion>> animCache = new IntMap<>();

    static {
        defaultTexParam.genMipMaps = true;
        defaultTexParam.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        defaultModelParam.textureParameter = defaultTexParam;
    }

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
        if (type == Model.class) {
            assetManager.load(name, Model.class, defaultModelParam);
        } if (type == Texture.class) {
            assetManager.load(name, Texture.class, defaultTexParam);
        } else {
            assetManager.load(name, type);
        }

        assetManager.finishLoading();
        if (type == Model.class) {
            Model model = assetManager.get(name, Model.class);
            TextureAttribute attribute = (TextureAttribute) model.materials.first().get(TextureAttribute.Diffuse);
            attribute.textureDescription.texture.bind();
            Gdx.gl30.glTexParameterf(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16);
        }

        if (type == Texture.class) {
            Texture texture = assetManager.get(name, Texture.class);
            texture.bind();
            Gdx.gl30.glTexParameterf(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16);
        }

        if (type == TextureAtlas.class) {
            atlasCache.add(assetManager.get(name, TextureAtlas.class));
        }
    }


    @Override
    public <T> T get(String name, Class<T> type, int index) {
        if (assetManager.isLoaded(name, type)) {
            return assetManager.get(name, type);
        } else if (type == TextureRegion.class) {
            for (TextureAtlas atlas : atlasCache) {
                TextureAtlas.AtlasRegion region;
                if (index <= 0) {
                    region = atlas.findRegion(name);
                } else {
                    region = atlas.findRegion(name, index);
                }
                if (region != null) {
                    return (T) region;
                }
            }

            Texture texture = get(name, Texture.class);
            if (texture != null) {
                return (T) new TextureRegion(texture);
            }


        } else {
            load(name, type);
            return get(name, type);
        }
        return null;
    }

    public <T> T get(String name, Class<T> type) {
        return get(name, type, -1);
    }

    @Override
    public FileHandle get(String fileName) {
        //TODO
        return Gdx.files.internal(fileName);
    }

    @Override
    public Drawable getDrawable(String name) {
        return new TextureRegionDrawable(get(name, TextureRegion.class));
    }

    @Override
    public void finish() {
        assetManager.finishLoading();
    }

    /**
     * Returns a cached list of TextureRegions that represent the animation of the given texture
     *
     * @param baseName name of the texture
     * @param frames   limit frames of animations
     * @return
     */
    public Array<? extends TextureRegion> loadAnimationFrames(String baseName, int frames) {
        int key = baseName.hashCode();
        if (!animCache.containsKey(key)) {
            Array<TextureAtlas.AtlasRegion> regions = null;
            for (TextureAtlas atlas : atlasCache) {
                regions = atlas.findRegions(baseName);
                if (regions != null) {
                    continue;
                }
            }
            if (regions != null) {
                animCache.put(key, regions);
                regions.setSize(frames);
            }
        }
        return animCache.get(key);
    }

}
