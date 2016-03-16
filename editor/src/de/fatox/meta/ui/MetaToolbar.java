package de.fatox.meta.ui;

import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

public class MetaToolbar {
    @Inject
    @Log
    private Logger log;
    @Inject
    private LanguageBundle languageBundle;
    @Inject
    private AssetProvider assetProvider;

    public final MenuBar menuBar;

    public MetaToolbar() {
        Meta.inject(this);
        this.menuBar = new MenuBar();
        log.info("MetaToolbar", "Created MenuBar");
        Menu fileMenu = createFileMenu();
        menuBar.addMenu(fileMenu);
        menuBar.addMenu(createEditMenu());
        log.info("MetaToolbar", "Added File Menu");
    }

    private Menu createFileMenu() {
        Menu fileMenu = new Menu(languageBundle.get("filemenu_title"));
        MenuItem filemenu_new = new MenuItem(languageBundle.get("filemenu_new"));
        fileMenu.addItem(filemenu_new);
        fileMenu.addItem(new MenuItem(languageBundle.get("filemenu_open")));
        return fileMenu;
    }

    private Menu createEditMenu() {
        Menu editMenu = new Menu(languageBundle.get("editmenu_title"));
        editMenu.addItem(new MenuItem("ffff"));
        editMenu.addItem(new MenuItem("aaaaaa"));
        return editMenu;
    }

}
