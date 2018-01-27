package de.fatox.meta.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.kotcrab.vis.ui.widget.MenuBar;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.dao.MetaWindowData;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.assets.MetaData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.input.MetaInput;
import de.fatox.meta.ui.windows.MetaDialog;

import java.io.File;

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
    private MenuBar mainMenuBar;

    private Table contentTable = new Table();
    private String currentScreenId = "(none)";

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
        this.currentScreenId = screenIdentifier;
        metaInput.changeScreen();

        // Close or move currently shown windows
        for (Window window : displayedWindows) {
            String name = window.getClass().getName();
            if (metaHas(name)) {
                MetaWindowData metaWindowData = metaGet(name, MetaWindowData.class);
                if (metaWindowData.getDisplayed()) {
                    // There exists saved window metadata
                    metaWindowData.set(window);
                } else {
                    cacheWindow(window, true);
                }
            } else {
                cacheWindow(window, true);
            }
        }
        displayedWindows.removeAll(cachedWindows, true);
        contentTable.remove();
        contentTable.clear();
        if (mainMenuBar != null) {
            contentTable.row().height(26);
            contentTable.add(mainMenuBar.getTable()).growX().top();
            mainMenuBar.getTable().toFront();
        }
        uiRenderer.addActor(contentTable);
        restoreWindows();
    }

    private void restoreWindows() {
        FileHandle[] list = metaData.getCachedHandle(currentScreenId).list();
        outer:
        for (FileHandle fh : list) {
            if (fh.name().endsWith("Window")) {
                try {
                    Class windowclass = ClassReflection.forName(fh.name());
                    MetaWindowData metaWindowData = metaGet(windowclass.getName(), MetaWindowData.class);
                    for (Window displayedWindow : displayedWindows) {
                        if (displayedWindow.getClass() == windowclass) {
                            if (!metaWindowData.getDisplayed()) {
                                cacheWindow(displayedWindow, true);
                            }
                            continue outer;
                        }
                    }
                    if (metaWindowData.getDisplayed()) {
                        metaWindowData.set(showWindow(windowclass));
                    }
                } catch (ReflectionException e) {
                    fh.delete();
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void addTable(Table table, boolean gx, boolean gy) {
        contentTable.row();
        Cell<Table> add = contentTable.add(table);
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
        log.debug(TAG, "show window: " + windowClass.getName());
        T window = displayWindow(windowClass);

        window.setVisible(true);

        if (metaHas(windowClass.getName())) {
            // There exists metadata for this window.
            MetaWindowData windowData = metaGet(windowClass.getName(), MetaWindowData.class);
            windowData.set(window);
            if (!windowData.getDisplayed()) {
                windowData.setDisplayed(true);
                metaSave(windowClass.getName(), windowData);
            }
        } else {
            // First time the window has been shown on this screen
            metaSave(windowClass.getName(), new MetaWindowData(window));
        }
        return window;
    }

    @Override
    public <T extends MetaDialog> T showDialog(Class<? extends T> dialogClass) {
        log.debug(TAG, "show dialog: " + dialogClass.getName());
        // Dialogs are just Window subtypes so we show it as usual
        T dialog = showWindow(dialogClass);
        dialog.show();
        return dialog;
    }

    @Override
    public void setMainMenuBar(MenuBar menuBar) {
        if (menuBar != null) {
            contentTable.row().height(26);
            contentTable.add(menuBar.getTable()).growX().top();
        } else if (mainMenuBar != null) {
            contentTable.removeActor(mainMenuBar.getTable());
        }
        mainMenuBar = menuBar;
    }

    @Override
    public <T extends Window> T getWindow(Class<? extends T> windowClass) {
        // TODO avoid NPE
        return getDisplayedClass(windowClass);
    }

    @Override
    public void closeWindow(Window window) {
        Window displayedWindow = getDisplayedInstance(window);
        if (displayedWindow != null) {
            displayedWindows.removeValue(window, true);
            MetaWindowData metaWindowData = metaGet(window.getClass().getName(), MetaWindowData.class);
            if (metaWindowData != null) {
                metaWindowData.setDisplayed(false);
                metaSave(displayedWindow.getClass().getName(), metaWindowData);
            }
            cacheWindow(window, false);
        }
    }

    @Override
    public void updateWindow(Window window) {
        String name = window.getClass().getName();
        if (metaHas(name)) {
            MetaWindowData metaWindowData = metaGet(name, MetaWindowData.class);
            metaWindowData.setFrom(window);
            metaSave(name, metaWindowData);
        }
    }

    @Override
    public void bringWindowsToFront() {
        for (Window window : displayedWindows) {
            window.toFront();
        }
        mainMenuBar.getTable().toFront();
    }


    public <T extends Window> T displayWindow(Class<? extends T> windowClass) {
        // Check if this window is a singleton. If it is and it is displayed, return displayed instance
        T theWindow = checkSingleton(windowClass);
        if (theWindow != null) {
            log.debug(TAG, "singleton already displaying");
            return theWindow;
        }
        // Check for a cached instance
        for (Window cachedWindow : cachedWindows) {
            if (cachedWindow.getClass() == windowClass) {
                log.debug(TAG, "found cached");
                theWindow = (T) cachedWindow;
                break;
            }
        }
        // If there was no cached instance we create a new one
        if (theWindow != null) {
            cachedWindows.removeValue(theWindow, true);
        } else {
            try {
                log.debug(TAG, "try instance");
                theWindow = windowClass.newInstance();

            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        uiRenderer.addActor(theWindow);
        displayedWindows.add(theWindow);
        return theWindow;
    }

    private <T extends Window> T checkSingleton(Class<? extends T> windowClass) {
        if (windowClass.isAnnotationPresent(Singleton.class)) {
            T displayedWindow = getDisplayedClass(windowClass);
            if (displayedWindow != null) {
                return displayedWindow;
            }
        }
        return null;
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


    private void cacheWindow(Window window, boolean forceClose) {
        cachedWindows.add(window);
        window.setVisible(false);
        if (forceClose) {
            window.remove();
        }
    }

    public boolean metaHas(String name) {
        return metaData.has(currentScreenId + File.separator + name);
    }

    public <T> T metaGet(String name, Class<T> c) {
        return metaData.get(currentScreenId + File.separator + name, c);
    }

    public void metaSave(String name, Object windowData) {
        String id = currentScreenId + File.separator + name;
        if (TimeUtils.timeSinceMillis(metaData.getCachedHandle(id).lastModified()) > 200) {
            metaData.save(id, windowData);
        }
    }
}
