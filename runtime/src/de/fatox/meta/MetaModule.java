package de.fatox.meta;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.entity.MetaEntityManager;
import de.fatox.meta.graphics.MetaFontProvider;
import de.fatox.meta.graphics.renderer.BufferRenderer;
import de.fatox.meta.injection.Log;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.input.MetaInput;
import de.fatox.meta.ui.MetaUiManager;


public class MetaModule {

    @Provides
    @Singleton
    @Named("default")
    public UIManager uiManager(MetaUiManager metaUiManager) {
        return metaUiManager;
    }

    @Provides
    @Singleton
    @Named("default")
    public MetaInput metaInput() {
        return new MetaInput();
    }

    @Provides
    @Singleton
    @Named("default")
    public Renderer renderer(BufferRenderer renderer) {
        return renderer;
    }

    @Provides
    @Singleton
    @Named("default")
    public PerspectiveCamera perspectiveCamera() {
        PerspectiveCamera cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(2f, 2f, 2f);
        cam.lookAt(0, 0, 0);
        cam.near = .1f;
        cam.far = 300f;
        cam.update();
        return cam;
    }

//    @Provides
//    @Singleton
//    @Named("default")
//    public EntityManager entityManager(MetaEntityManager metaEntityManager) {
//        return metaEntityManager;
//    }

    @Provides
    @Singleton
    @Named("default")
    public EntityManager<Meta3DEntity> entityManager() {
        return new MetaEntityManager();
    }


    @Provides
    @Singleton
    @Named("default")
    public SpriteBatch spriteBatch() {
        SpriteBatch spriteBatch = new SpriteBatch();
        spriteBatch.enableBlending();
        return spriteBatch;
    }

    @Provides
    @Singleton
    @Named("default")
    public AssetProvider assetProvider(MetaAssetProvider metaAssetProvider) {
        return metaAssetProvider;
    }

    @Provides
    @Singleton
    @Named("default")
    public FontProvider fontProvider(MetaFontProvider metaFontProvider) {
        return metaFontProvider;
    }


    @Provides
    @Singleton
    @Log
    public Logger log(MetaLogger metaLogger) {
        return metaLogger;
    }

}
