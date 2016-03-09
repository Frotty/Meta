package de.fatox.meta;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.injection.Feather;
import de.fatox.meta.injection.Inject;

public class Meta extends Game {
    private static Meta metaInstance;
    private Feather feather;
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

    private final void setupFeather() {
        feather = Feather.with(modules);
    }

    public static final void inject(Object object) {
        getMetaInstance().feather.injectFields(object);
    }

    @Override
    public void create() {
        inject(this);
        setScreen(firstScreen);
    }
}
