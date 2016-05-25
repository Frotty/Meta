package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.ui.windows.ProjectWizard;

public class MetaEditorMenuBar {
    @Inject
    @Log
    private Logger log;
    @Inject
    private LanguageBundle languageBundle;
    @Inject
    private AssetProvider assetProvider;

    public final MenuBar menuBar;

    public MetaEditorMenuBar() {
        Meta.inject(this);
        this.menuBar = new MenuBar();
        log.info("MetaEditorMenuBar", "Created MenuBar");
        Menu fileMenu = createFileMenu();
        menuBar.addMenu(fileMenu);
        menuBar.addMenu(createEditMenu());
        log.info("MetaEditorMenuBar", "Added File Menu");
    }

    private Menu createFileMenu() {
        final Menu fileMenu = new Menu(languageBundle.get("filemenu_title"));
        MenuItem filemenu_new = new MenuItem(languageBundle.get("filemenu_new"));
        filemenu_new.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ProjectWizard newproject_dialog_title = new ProjectWizard(languageBundle.get("newproj_dia_title"));
                newproject_dialog_title.show(fileMenu.getStage());
            }
        });
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
