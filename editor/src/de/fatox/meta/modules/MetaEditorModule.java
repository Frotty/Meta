package de.fatox.meta.modules;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import de.fatox.meta.Primitives;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.assets.MetaAssetProvider;
import de.fatox.meta.ide.*;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.lang.MetaLanguageBundle;
import de.fatox.meta.screens.MetaEditorScreen;
import de.fatox.meta.shader.EditorSceneRenderer;
import de.fatox.meta.shader.MetaShaderComposer;
import de.fatox.meta.shader.MetaShaderLibrary;

public class MetaEditorModule {

    @Provides
    @Singleton
    public Renderer renderer() {
        return new EditorSceneRenderer();
    }

    @Provides
    @Singleton
    public Primitives primitives() {
        return new Primitives();
    }

    @Provides
    @Singleton
    public ProjectManager projectManager(MetaProjectManager projectManager) {
        return projectManager;
    }

    @Provides
    @Singleton
    @Named("default")
    public MetaShaderComposer shaderComposer() {
        return new MetaShaderComposer();
    }

    @Provides
    @Singleton
    @Named("default")
    public MetaShaderLibrary shaderLibrary() {
        return new MetaShaderLibrary();
    }

    @Provides
    @Singleton
    public SceneManager sceneManager(MetaSceneManager sceneManager) {
        return sceneManager;
    }

    @Provides
    @Singleton
    @Named("default")
    public MetaAssetProvider metaAssetProvider() {
        return new MetaAssetProvider();
    }

    @Provides
    @Singleton
    public AssetDiscoverer assetManager() {
        return new AssetDiscoverer();
    }

    @Provides
    @Singleton
    @Named("default")
    public Screen firstScreen(MetaEditorScreen editorScreen) {
        return editorScreen;
    }

    @Provides
    @Singleton
    @Named("default")
    public LanguageBundle languageBundle(MetaLanguageBundle metaLanguageBundle) {
        return metaLanguageBundle;
    }

    @Provides
    @Singleton
    @Named("open")
    public FileChooser openFileChooser() {
        return new FileChooser(FileChooser.Mode.OPEN);
    }

    @Provides
    @Singleton
    @Named("save")
    public FileChooser saveFileChooser() {
        return new FileChooser(FileChooser.Mode.SAVE);
    }

    @Provides
    @Singleton
    @Named("visuiSkin")
    public String uiSkinPath() {
        return "visui\\uiskin.json";
    }

    @Provides
    @Singleton
    public ShapeRenderer shapeRenderer() {
        return null;
    }

}
