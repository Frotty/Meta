package de.fatox.meta;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Metastasis;

public class Meta extends Game {
    private static Meta metaInstance;
    private Metastasis metastasis;
    private Array<Object> modules = new Array<>();
    private static long lastChange = 0;
    private Screen lastScreen;

    @Inject
    private Screen firstScreen;

    public static Meta getInstance() {
        return metaInstance != null ? metaInstance : new Meta();
    }

    public Meta() {
        metaInstance = this;
        addModule(new MetaModule());
    }

    public static void addModule(Object module) {
        getInstance().modules.add(module);
        getInstance().setupMetastasis();
    }

    public static void registerMetaAnnotation(Class annotationClass) {

    }

    public static boolean canChangeScreen() {
        return (TimeUtils.millis() > lastChange + 250);
    }

    public static void newLastScreen() {
        if(getInstance().lastScreen != null) {
            try {
                changeScreen(getInstance().lastScreen.getClass().newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static void changeScreen(final Screen newScreen) {
        if (canChangeScreen()) {
            lastChange = TimeUtils.millis();
            Screen oldScreen = getInstance().getScreen();
            if (oldScreen != null && !oldScreen.getClass().isInstance(newScreen)) {
                getInstance().lastScreen = oldScreen;
            }
            Gdx.app.postRunnable(() -> getInstance().setScreen(newScreen));
//            inputManager.clearDownKeys();
        }
    }

    private final void setupMetastasis() {
        metastasis = Metastasis.with(modules);
    }

    public static final void inject(Object object) {
        getInstance().metastasis.injectFields(object);
    }

    @Override
    public void create() {
        inject(this);
        changeScreen(firstScreen);
    }

    public Class getLastScreenType() {
        return lastScreen.getClass();
    }
}