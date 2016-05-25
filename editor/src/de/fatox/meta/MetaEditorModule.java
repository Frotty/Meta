package de.fatox.meta;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.ide.persist.PersistanceManager;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.lang.MetaLanguageBundle;
import de.fatox.meta.persist.YamlPersistanceManager;
import de.fatox.meta.screens.MetaEditorScreen;
import de.fatox.meta.shader.MetaShaderLibrary;
import de.fatox.meta.ui.MetaUIRenderer;

public class MetaEditorModule {
    @Provides
    @Singleton
    @Named("default")
    public PersistanceManager persistanceManager(YamlPersistanceManager persistanceManager) {
        return persistanceManager;
    }

    @Provides
    @Singleton
    @Named("default")
    public UIRenderer uiRenderer(MetaUIRenderer uiRenderer) {
        return uiRenderer;
    }

    @Provides
    @Singleton
    @Named("default")
    public Screen firstScreen(MetaEditorScreen editorScreen) {
        return editorScreen;
    }

    @Provides
    @Singleton
    @Named("default")
    public LanguageBundle languageBundle(MetaLanguageBundle metaLanguageBundle) {
        return metaLanguageBundle;
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
    public DefaultTextureBinder textureBinder() {
        return new DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN, 10);
    }

}
