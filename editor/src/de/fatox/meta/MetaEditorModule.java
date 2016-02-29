package de.fatox.meta;

import com.badlogic.gdx.Screen;
import de.fatox.meta.api.ide.lang.LanguageBundle;
import de.fatox.meta.api.ide.persist.PersistanceManager;
import de.fatox.meta.api.ide.ui.UIRenderer;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.lang.MetaLanguageBundle;
import de.fatox.meta.persist.YamlPersistanceManager;
import de.fatox.meta.screens.MetaEditorScreen;
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
    public Screen firstScreen(MetaEditorScreen editorScreen) {
        return editorScreen;
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
