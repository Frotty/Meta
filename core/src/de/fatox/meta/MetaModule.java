package de.fatox.meta;

import de.fatox.meta.injection.Provides;
import de.fatox.meta.input.MetaInput;

import javax.inject.Named;
import javax.inject.Singleton;

public class MetaModule {

    @Provides
    @Singleton
    @Named("default")
    public MetaInput metaInput() {
        return new MetaInput();
    }
}
