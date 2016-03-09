package de.fatox.meta;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.injection.Metastasis;
import de.fatox.meta.injection.Inject;

public class Meta extends Game {
    private static Meta metaInstance;
    private Metastasis metastasis;
    private Array<Object> modules = new Array<>();

    @Inject
    private Screen firstScreen;

    public static Meta getMetaInstance() {
        return metaInstance != null ? metaInstance : new Meta();
    }

    public Meta() {
        metaInstance = this;
        addModule(new MetaModule());
    }

    public static void addModule(Object module) {
        getMetaInstance().modules.add(module);
        getMetaInstance().setupFeather();
    }

    public static void registerMetaAnnotation(Class annotationClass) {

    }

    private final void setupFeather() {
        metastasis = Metastasis.with(modules);
    }

    public static final void inject(Object object) {
        getMetaInstance().metastasis.injectFields(object);
    }

    @Override
    public void create() {
        inject(this);
        setScreen(firstScreen);
    }
}
