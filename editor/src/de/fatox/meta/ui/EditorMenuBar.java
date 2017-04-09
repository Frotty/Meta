package de.fatox.meta.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.dialogs.OpenProjectDialog;
import de.fatox.meta.ui.dialogs.ProjectWizardDialog;
import de.fatox.meta.ui.dialogs.SceneWizardDialog;
import de.fatox.meta.ui.windows.*;

public class EditorMenuBar {
    @Inject
    @Log
    private Logger log;
    @Inject
    private LanguageBundle languageBundle;
    @Inject
    private AssetProvider assetProvider;
    @Inject
    private ProjectManager projectManager;
    @Inject
    private UIManager uiManager;

    public final MenuBar menuBar;

    public EditorMenuBar() {
        Meta.inject(this);
        this.menuBar = new MenuBar();
        log.info("EditorMenuBar", "Created MenuBar");
        Menu fileMenu = createFileMenu();
        menuBar.addMenu(fileMenu);
        menuBar.addMenu(createWindowsMenu());
        log.info("EditorMenuBar", "Added File Menu");
        menuBar.getTable().add().growX();
        menuBar.getTable().row().height(1).left();
        menuBar.getTable().add(new Separator()).colspan(2).left().growX();
    }

    private Menu createFileMenu() {
        final Menu fileMenu = new Menu(languageBundle.get("filemenu_title"));
        MenuItem menuItemNewProject = new MenuItem(languageBundle.get("filemenu_new_proj"), new Image(assetProvider.get("ui/appbar.new.png", Texture.class)));
        menuItemNewProject.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                uiManager.showDialog(ProjectWizardDialog.class);
            }
        });
        fileMenu.addItem(menuItemNewProject);
        MenuItem menuItemNewScene = new MenuItem(languageBundle.get("filemenu_new_scene"), new Image(assetProvider.get("ui/appbar.new.png", Texture.class)));
        menuItemNewScene.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(projectManager.getCurrentProject() != null) {
                    uiManager.showDialog(SceneWizardDialog.class);
                } else {
                    new MetaConfirmDialog("Project required", "Please open a project first").show(fileMenu.getStage());
                }
            }
        });
        fileMenu.addItem(menuItemNewScene);
        MenuItem menuItemOpen = new MenuItem(languageBundle.get("filemenu_open"), new Image(assetProvider.get("ui/appbar.folder.open.png", Texture.class)));
        menuItemOpen.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                uiManager.showDialog(OpenProjectDialog.class);
            }
        });
        fileMenu.addItem(menuItemOpen);
        fileMenu.setWidth(200);
        return fileMenu;
    }

    private Menu createWindowsMenu() {
        Menu editMenu = new Menu(languageBundle.get("windowsmenu_title"));
        MenuItem assetItem = new MenuItem("Asset Discoverer");
        assetItem.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.showWindow(AssetDiscovererWindow.class);
            }
        });
        MenuItem primitivesItem = new MenuItem("Primitives");
        primitivesItem.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.showWindow(PrimitivesWindow.class);
            }
        });
        MenuItem cameraItem = new MenuItem("Camera");
        cameraItem.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.showWindow(CameraWindow.class);
            }
        });
        MenuItem shaderMenu = new MenuItem("Shader Library");
        shaderMenu.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.showWindow(ShaderLibraryWindow.class);
            }
        });
        MenuItem shaderPipeMenu = new MenuItem("Shader Pipeline");
        shaderPipeMenu.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.showWindow(RenderBufferWindow.class);
            }
        });
        editMenu.addItem(assetItem);
        editMenu.addItem(cameraItem);
        editMenu.addItem(primitivesItem);
        editMenu.addItem(shaderMenu);
        editMenu.addItem(shaderPipeMenu);
        return editMenu;
    }

}
