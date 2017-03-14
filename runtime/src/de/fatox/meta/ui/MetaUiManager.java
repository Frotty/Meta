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
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.api.dao.MetaData2;
import de.fatox.meta.api.dao.MetaWindowData;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.ui.UIRenderer;
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
    private MetaData2 metaData2;
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
                // There exists saved window metadata
                metaGetWindow(name).set(window);
            } else {
                cacheWindow(window, true);
            }
        }
        contentTable.remove();
        contentTable.clear();
        if (mainMenuBar != null) {
            contentTable.row().height(26);
            contentTable.add(mainMenuBar.getTable()).growX().top();
        }
        uiRenderer.addActor(contentTable);
        restoreWindows();
    }

    private void restoreWindows() {
        FileHandle[] list = metaData2.getCachedRootHandle(currentScreenId).list();
        outer:
        for (FileHandle fh : list) {
            if (fh.name().endsWith("Window")) {
                try {
                    Class windowclass = ClassReflection.forName(fh.name());
                    for (Window displayedWindow : displayedWindows) {
                        if (displayedWindow.getClass() == windowclass) {
                            continue outer;
                        }
                    }
                    showWindow(windowclass);
                } catch (ReflectionException e) {
                    e.printStackTrace();
                }
            }
        }
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
        log.debug(TAG, "show window: " + windowClass.getName());
        T window = displayWindow(windowClass);

        window.setVisible(true);
        uiRenderer.addActor(window);

        if (metaHas(windowClass.getName())) {
            // There exists metadata for this window.
            MetaWindowData windowData = metaGetWindow(windowClass.getName());
            windowData.set(window);
        } else {
            // First time the window has been shown on this screen
            metaSaveWindow(windowClass.getName(), new MetaWindowData(window));
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
            MetaWindowData metaWindowData = metaGetWindow(window.getClass().getName());
            metaWindowData.displayed = false;
            metaSaveWindow(displayedWindow.getClass().getName(), metaWindowData);
            cacheWindow(window, false);
        }
    }

    @Override
    public void updateWindow(Window window) {
        String name = window.getClass().getName();
        if(metaHas(name)) {
            MetaWindowData metaWindowData = metaGetWindow(name);
            metaWindowData.setFrom(window);
            metaSaveWindow(name, metaWindowData);
        }
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
        displayedWindows.removeValue(window, true);
        cachedWindows.add(window);
        if (forceClose) {
            window.remove();
        }
    }

    private boolean metaHas(String name) {
        return metaData2.has(currentScreenId + File.separator + name);
    }

    private MetaWindowData metaGetWindow(String name) {
        return metaData2.get(currentScreenId + File.separator + name, MetaWindowData.class);
    }

    private void metaSaveWindow(String name, MetaWindowData windowData) {
        String id = currentScreenId + File.separator + name;
        if(TimeUtils.timeSinceMillis(metaData2.getCachedRootHandle(id).lastModified()) > 200) {
            metaData2.save(id, windowData);
        }
    }

}
