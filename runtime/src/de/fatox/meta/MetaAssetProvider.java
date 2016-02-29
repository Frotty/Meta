package de.fatox.meta;

import com.badlogic.gdx.assets.AssetManager;
import de.fatox.meta.api.AssetProvider;

public class MetaAssetProvider implements AssetProvider {
    private AssetManager assetManager = new AssetManager();

    public MetaAssetProvider() {
    }

    @Override
    public <T> T get(String fileName, Class<T> type) {
        return null;
    }
}
