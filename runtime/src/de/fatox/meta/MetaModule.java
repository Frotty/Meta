package de.fatox.meta;

import com.badlogic.gdx.Screen;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.ide.lang.LanguageBundle;
import de.fatox.meta.api.ide.persist.PersistanceManager;
import de.fatox.meta.api.ide.ui.UIRenderer;
import de.fatox.meta.graphics.renderer.BufferRenderer;
import de.fatox.meta.ide.lang.MetaLanguageBundle;
import de.fatox.meta.ide.persist.YamlPersistanceManager;
import de.fatox.meta.ide.screens.MetaEditorScreen;
import de.fatox.meta.ide.ui.MetaUIRenderer;
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
    public PersistanceManager persistanceManager(YamlPersistanceManager persistanceManager) {
        return persistanceManager;
    }

    @Provides
    @Singleton
    @Named("default")
    public Screen firstScreen(MetaEditorScreen editorScreen) {
        return editorScreen;
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
    public UIRenderer uiRenderer(MetaUIRenderer uiRenderer) {
        return uiRenderer;
    }

    @Provides
    @Singleton
    @Named("default")
    public LanguageBundle languageBundle(MetaLanguageBundle metaLanguageBundle) {
        return metaLanguageBundle;
    }
}
