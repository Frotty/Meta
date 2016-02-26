package de.fatox.meta;

import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.graphics.renderer.BufferRenderer;
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
    public Renderer renderer(BufferRenderer  renderer) {
        return renderer;
    }
}
