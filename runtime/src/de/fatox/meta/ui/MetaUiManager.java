package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.MenuBar;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.input.MetaInput;

/**
 * Created by Frotty on 20.05.2016.
 */
@Singleton
public class MetaUiManager implements UIManager {
    private static final String TAG = "MetaUiManager";
    @Inject
    @Log
    private Logger log;
    @Inject
    private UIRenderer uiRenderer;
    @Inject
    private MetaData metaData;
    @Inject
    private MetaInput metaInput;

    private Array<Window> displayedWindows = new Array<>();
    private Array<Window> cachedWindows = new Array<>();
    private Array<MenuBar> menuBars = new Array<>();
    private Table contentTable = new Table();

    public MetaUiManager() {
        Meta.inject(this);
        contentTable.top().left();
        contentTable.setPosition(0, 0);
        contentTable.setFillParent(true);
        uiRenderer.addActor(contentTable);
    }

    @Override
    public void resize(int width, int height) {
        uiRenderer.resize(width, height);
    }

    @Override
    public void changeScreen(String screenIdentifier) {
        metaInput.changeScreen();
        metaData.getScreenData(screenIdentifier);
        for (Window window : displayedWindows) {
            cacheWindow(window);
        }
        displayedWindows.clear();
        contentTable.remove();
        contentTable.clear();
        uiRenderer.addActor(contentTable);
    }

    @Override
    public void addTable(Table table, boolean gx, boolean gy) {
        contentTable.row();
        Cell<Table> add = contentTable.add(table).top();
        if (gx)
            add.growX();
        if (gy)
            add.growY();
    }

    /**
     * Shows an instance of the given class on the current screen.
     * If metadata exists for the window, it will be loaded.
     *
     * @param windowClass The window to show
     */
    @Override
    public <T extends Window> T showWindow(Class<? extends T> windowClass) {
        // If the window is annotated singleton only one instance should be displayed
        if (windowClass.isAnnotationPresent(Singleton.class)) {
            T displayedWindow = getDisplayedClass(windowClass);
            if (displayedWindow != null) {
                return displayedWindow;
            }
        }
        // Check for a cached instance
        Window theWindow = null;
        for (Window cachedWindow : cachedWindows) {
            if (cachedWindow.getClass() == windowClass) {
                log.debug(TAG, "found cached window");
                theWindow = cachedWindow;
                break;
            }
        }
        // If there was no cached instance we create a new one
        if (theWindow != null) {
            cachedWindows.removeValue(theWindow, true);
        } else {
            try {
                theWindow = windowClass.newInstance();

            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        displayedWindows.add(theWindow);
        theWindow.setVisible(true);
        uiRenderer.addActor(theWindow);
        if (metaData.hasWindowData(windowClass)) {
            // If there is saved metadata, restore configuration
            metaData.getWindowData(theWindow).set(theWindow);
        }
        return (T) theWindow;
    }

    @Override
    public void addMenuBar(MenuBar menuBar) {
        menuBars.add(menuBar);
        contentTable.row().height(26);
        contentTable.add(menuBar.getTable()).growX().top();
    }

    @Override
    public <T extends Window> T getWindow(Class<? extends T> windowClass) {
        // TODO avoid NPE
        return getDisplayedClass(windowClass);
    }

    @Override
    public void closeWindow(Window window) {
        Window displayedWindow = getDisplayedInstance(window);
        if(displayedWindow != null) {
            metaData.getWindowData(displayedWindow).displayed = false;
            displayedWindows.removeValue(window, true);
            cacheWindow(window);
        }
    }

    private <T extends Window> T getDisplayedClass(Class<? extends Window> windowClass) {
        for (Window displayedWindow : displayedWindows) {
            if (displayedWindow.getClass() == windowClass) {
                return (T) displayedWindow;
            }
        }
        return null;
    }

    private Window getDisplayedInstance(Window window) {
        for (Window displayedWindow : displayedWindows) {
            if (displayedWindow == window) {
                return displayedWindow;
            }
        }
        return null;
    }


    private void cacheWindow(Window window) {
        window.remove();
        cachedWindows.add(window);
        window.setVisible(false);
    }
}
