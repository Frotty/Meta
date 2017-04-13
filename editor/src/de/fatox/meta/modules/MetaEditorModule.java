package de.fatox.meta.modules;

import com.badlogic.gdx.Screen;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import de.fatox.meta.Primitives;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.ide.*;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.lang.MetaLanguageBundle;
import de.fatox.meta.screens.MetaEditorScreen;
import de.fatox.meta.shader.MetaShaderComposer;
import de.fatox.meta.shader.MetaShaderLibrary;

public class MetaEditorModule {

    @Provides
    @Singleton
    public Primitives primitives() {
        return new Primitives();
    }

    @Provides
    @Singleton
    public SceneManager sceneManager(MetaSceneManager sceneManager) {
        return sceneManager;
    }

    @Provides
    @Singleton
    public AssetDiscoverer assetManager() {
        return new AssetDiscoverer();
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
    @Named("default")
    public ShaderLibrary shaderLibrary(MetaShaderLibrary metaShaderLibrary) {
        return metaShaderLibrary;
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


}
