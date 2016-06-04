package de.fatox.meta.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.Menu;
import com.kotcrab.vis.ui.widget.MenuBar;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.Separator;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.ui.windows.ProjectWizardDialog;

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
        menuBar.getTable().add().growX();
        menuBar.getTable().row().height(1).left();
        menuBar.getTable().add(new Separator()).colspan(2).left().growX();
    }

    private Menu createFileMenu() {
        final Menu fileMenu = new Menu(languageBundle.get("filemenu_title"));
        MenuItem menuItemNew = new MenuItem(languageBundle.get("filemenu_new"), new Image(assetProvider.get("ui/appbar.new.png", Texture.class)));
        menuItemNew.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ProjectWizardDialog projectWizard = new ProjectWizardDialog(languageBundle.get("newproj_dia_title"));
                projectWizard.show(fileMenu.getStage());
            }
        });
        fileMenu.addItem(menuItemNew);
        fileMenu.addItem(new MenuItem(languageBundle.get("filemenu_open")));
        fileMenu.setWidth(200);
        return fileMenu;
    }

    private Menu createEditMenu() {
        Menu editMenu = new Menu(languageBundle.get("editmenu_title"));
        editMenu.addItem(new MenuItem("ffff"));
        editMenu.addItem(new MenuItem("aaaaaa"));
        return editMenu;
    }

}
