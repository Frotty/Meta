package de.fatox.meta;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.TimeUtils;
import de.fatox.meta.api.DummyPosModifier;
import de.fatox.meta.api.PosModifier;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Metastasis;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Meta extends Game {
    private static Meta metaInstance;
    private Metastasis metastasis;

    private long lastChange = 0;
    private Screen lastScreen;

    @Inject
    protected Screen firstScreen;
    @Inject
    protected UIManager uiManager;
    protected PosModifier modifier;

    public static Meta getInstance() {
        return metaInstance != null ? metaInstance : new Meta();
    }

    public Meta() {
        this(new DummyPosModifier());
    }

    public Meta(PosModifier modifier) {
        this.modifier = modifier;
		setUncaughtHandler();
		metaInstance = this;
        setupMetastasis();
        addModule(new MetaModule());
    }

	public Meta(PosModifier modifier, Object... modules) {
		this.modifier = modifier;
		setUncaughtHandler();
		metaInstance = this;
		setupMetastasis();
		for (Object module: modules) {
			addModule(module);
		}
		addModule(new MetaModule());
	}

	private void setUncaughtHandler() {
		Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
			exception.printStackTrace();
			try {
				javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			JTextArea jTextField = new JTextArea();
			jTextField.setText("Please report this crash with the following info:\n" + sw.toString());
			jTextField.setEditable(false);
			JOptionPane.showMessageDialog(null, jTextField, "Uncaught Exception", JOptionPane.ERROR_MESSAGE);
		});
	}

	public static void addModule(Object module) {
        getInstance().metastasis.loadModule(module);
    }

    public static void registerMetaAnnotation(Class annotationClass) {

    }

    public static boolean canChangeScreen() {
        return (TimeUtils.millis() > metaInstance.lastChange + 150);
    }

    public static void newLastScreen() {
        if (getInstance().lastScreen != null) {
            try {
                changeScreen(getInstance().lastScreen.getClass().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void changeScreen(final Screen newScreen) {
        if (canChangeScreen()) {
            metaInstance.lastChange = TimeUtils.millis();
            Screen oldScreen = getInstance().getScreen();
            if (oldScreen != null && !oldScreen.getClass().isInstance(newScreen)) {
                getInstance().lastScreen = oldScreen;
            }
            Gdx.app.postRunnable(() -> getInstance().setScreen(newScreen));
        }
    }

    private void setupMetastasis() {
        metastasis = new Metastasis();
    }

    public static void inject(Object object) {
        getInstance().metastasis.injectFields(object);
    }

    @Override
    public void create() {
        inject(this);
        uiManager.setPosModifier(modifier);
        changeScreen(firstScreen);
    }

    public Class getLastScreenType() {
        return lastScreen.getClass();
    }

}
