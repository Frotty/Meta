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
import de.fatox.meta.ui.windows.AssetDiscovererWindow;
import de.fatox.meta.ui.windows.MetaConfirmDialog;
import de.fatox.meta.ui.windows.ShaderLibraryWindow;
import de.fatox.meta.ui.windows.ShaderPipelineWindow;

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
    private ShaderLibraryWindow shaderLibraryWindow;
    @Inject
    private ShaderPipelineWindow shaderPipelineWindow;
    @Inject
    private AssetDiscovererWindow assetDiscovererWindow;
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
                ProjectWizardDialog projectWizard = new ProjectWizardDialog(languageBundle.get("newproj_dia_title"));
                projectWizard.show(fileMenu.getStage());
            }
        });
        fileMenu.addItem(menuItemNewProject);
        MenuItem menuItemNewScene = new MenuItem(languageBundle.get("filemenu_new_scene"), new Image(assetProvider.get("ui/appbar.new.png", Texture.class)));
        menuItemNewScene.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(projectManager.getCurrentProject() != null) {
                    SceneWizardDialog newSceneDialog = new SceneWizardDialog(languageBundle.get("newscene_dia_title"));
                    newSceneDialog.show(fileMenu.getStage());
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
                OpenProjectDialog openProjectDialog = new OpenProjectDialog();
                openProjectDialog.show(fileMenu.getStage());
            }
        });
        fileMenu.addItem(menuItemOpen);
        fileMenu.setWidth(200);
        return fileMenu;
    }

    private Menu createWindowsMenu() {
        Menu editMenu = new Menu(languageBundle.get("windowsmenu_title"));
        MenuItem assetMenu = new MenuItem("Asset Discoverer");
        assetMenu.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                assetDiscovererWindow.refresh();
                uiManager.addWindow(assetDiscovererWindow, false);
            }
        });
        MenuItem shaderMenu = new MenuItem("Shader Library");
        shaderMenu.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.addWindow(shaderLibraryWindow, false);
            }
        });
        MenuItem shaderPipeMenu = new MenuItem("Shader Pipeline");
        shaderPipeMenu.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.addWindow(shaderPipelineWindow, false);
            }
        });
        editMenu.addItem(assetMenu);
        editMenu.addItem(shaderMenu);
        editMenu.addItem(shaderPipeMenu);
        return editMenu;
    }

}
