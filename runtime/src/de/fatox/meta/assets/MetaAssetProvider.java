package de.fatox.meta.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
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
import com.badlogic.gdx.utils.ObjectMap;
import de.fatox.meta.api.AssetProvider;

public class MetaAssetProvider implements AssetProvider {
    private static TextureLoader.TextureParameter defaultTexParam = new TextureLoader.TextureParameter();
    private static ModelLoader.ModelParameters defaultModelParam = new ModelLoader.ModelParameters();
    private AssetManager assetManager = new AssetManager();
    private Array<TextureAtlas> atlasCache = new Array<>();
    private IntMap<Array<? extends TextureRegion>> animCache = new IntMap<>();

    private ObjectMap<String, XPKFileHandle> packFileCache = new ObjectMap<>();

    static {
        defaultTexParam.genMipMaps = true;
        defaultTexParam.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        defaultModelParam.textureParameter = defaultTexParam;
    }

    @Override
    public boolean loadAssetsFromFolder(FileHandle folder) {
        if (folder.isDirectory()) {
            for (FileHandle itrHandle : folder.list()) {
                if (itrHandle.extension().equalsIgnoreCase(XPKLoader.EXTENSION)) {
                    Array<XPKFileHandle> list = XPKLoader.INSTANCE.getList(itrHandle);
                    list.forEach(it -> {
                        packFileCache.put(it.name(), it);
                    });
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public <T> void load(String name, Class<T> type) {
        loadIntern(new AssetDescriptor(name, type));
    }


    private <T> void loadIntern(AssetDescriptor<T> descr) {
        if (descr.type == Model.class) {
            assetManager.load(descr.fileName, Model.class, defaultModelParam);
        } else if (descr.type == Texture.class && !descr.fileName.contains("ui")) {
            assetManager.load(descr.fileName, Texture.class, defaultTexParam);
        } else {
            assetManager.load(descr);
        }

        assetManager.finishLoading();
        if (descr.type == Model.class) {
            Model model = assetManager.get(descr.fileName, Model.class);
            TextureAttribute attribute = (TextureAttribute) model.materials.first().get(TextureAttribute.Diffuse);
            attribute.textureDescription.texture.bind();
            Gdx.gl30.glTexParameterf(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16);
        }

        if (descr.type == Texture.class) {
            Texture texture = assetManager.get(descr.fileName, Texture.class);
            texture.bind();
            Gdx.gl30.glTexParameterf(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_ANISOTROPY_EXT, 16);
        }

        if (descr.type == TextureAtlas.class) {
            atlasCache.add(assetManager.get(descr.fileName, TextureAtlas.class));
        }
    }


    @Override
    public <T> T get(String name, Class<T> type, int index) {
        if (type == FileHandle.class) {
            if (packFileCache.containsKey(name)) {
                return (T) packFileCache.get(name);
            } else {
                return (T) Gdx.files.internal(name);
            }
        }
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


        } else if (packFileCache.containsKey(name)) {
            XPKFileHandle xpkFileHandle = packFileCache.get(name);
            AssetDescriptor assetDescriptor = new AssetDescriptor(xpkFileHandle, type);
            loadIntern(assetDescriptor);
            return get(name, type);
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
