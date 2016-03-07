package de.fatox.meta;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.entity.MetaEntityManager;
import de.fatox.meta.graphics.MetaFontProvider;
import de.fatox.meta.graphics.renderer.BufferRenderer;
import de.fatox.meta.graphics.shader.MetaShaderLibrary;
import de.fatox.meta.injection.Log;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.input.MetaInput;


public class MetaModule {

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
    public ShaderLibrary shaderLibrary(MetaShaderLibrary metaShaderLibrary) {
        return metaShaderLibrary;
    }

    @Provides
    @Singleton
    @Named("default")
    public Camera camera(PerspectiveCamera perspectiveCamera) {
        return perspectiveCamera;
    }

    @Provides
    @Singleton
    @Named("default")
    public EntityManager entityManager(MetaEntityManager metaEntityManager) {
        return metaEntityManager;
    }

    @Provides
    @Singleton
    @Named("default")
    public SpriteBatch spriteBatch() {
        return new SpriteBatch();
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
