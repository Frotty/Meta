package de.fatox.meta;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Json;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.assets.MetaAssetProvider;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.entity.MetaEntityManager;
import de.fatox.meta.graphics.font.MetaFontProvider;
import de.fatox.meta.graphics.renderer.BufferRenderer;
import de.fatox.meta.injection.Log;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.input.MetaInput;
import de.fatox.meta.sound.MetaSoundPlayer;
import de.fatox.meta.task.MetaTaskManager;
import de.fatox.meta.ui.MetaUIRenderer;
import de.fatox.meta.ui.MetaUiManager;

@Singleton
public class MetaModule {

    @Provides
    @Singleton
    public MetaSoundPlayer metaSoundPlayer() {
        return new MetaSoundPlayer();
    }

    @Provides
    @Singleton
    public UIRenderer uiRenderer(MetaUIRenderer metaUIRenderer) {
        return metaUIRenderer;
    }

    @Provides
    @Singleton
    public UIManager uiManager(MetaUiManager metaUiManager) {
        return metaUiManager;
    }

    @Provides
    @Singleton
    public MetaInput metaInput() {
        return new MetaInput();
    }

    @Provides
    @Singleton
    public Renderer renderer(BufferRenderer renderer) {
        return renderer;
    }

    @Provides
    @Singleton
    public PerspectiveCamera perspectiveCamera() {
        PerspectiveCamera cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0, 50f, 50f);
//        cam.lookAt(0, 0, 0);
        cam.near = 1;
        cam.far = 750f;
        return cam;
    }

    @Provides
    @Singleton
    public ModelBuilder modelBuilder() {
        return new ModelBuilder();
    }

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
    @Named("default")
    public MetaTaskManager taskManager() {
        return new MetaTaskManager();
    }

    @Provides
    @Singleton
    @Log
    public Logger log(MetaLogger metaLogger) {
        return metaLogger;
    }

    @Provides
    @Singleton
    @Named("default")
    public Json json() {
        return new Json();
    }

}
