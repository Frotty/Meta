package de.fatox.meta;

import com.badlogic.gdx.Screen;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.ide.persist.PersistanceManager;
import de.fatox.meta.graphics.renderer.BufferRenderer;
import de.fatox.meta.ide.MetaProject;
import de.fatox.meta.ide.persist.YamlPersistanceManager;
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
    public Screen firstScreen(MetaProject metaProject) {
        return metaProject;
    }
}
