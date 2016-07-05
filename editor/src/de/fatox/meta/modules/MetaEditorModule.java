package de.fatox.meta.modules;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.ide.*;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.lang.MetaLanguageBundle;
import de.fatox.meta.screens.MetaEditorScreen;
import de.fatox.meta.shader.MetaShaderLibrary;
import io.gsonfire.GsonFireBuilder;

public class MetaEditorModule {

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
    public Gson gson() {
        final GsonFireBuilder fireBuilder = new GsonFireBuilder();
        fireBuilder.enableExposeMethodResult();
        fireBuilder.enableExclusionByValue();
//        fireBuilder.registerTypeSelector(GameObject.class, readElement -> {
//            JsonElement t = readElement.getAsJsonObject().get("t");
//            if (t == null) {
//                return EmptyObject.class;
//            }
//            int typeId = Integer.parseInt(t.getAsString());
//            if (typeId > 0) {
//                Class<? extends GameObject> aClass = ObjectIds.get(typeId);
//                return aClass;
//            }
//            return EmptyObject.class;
//        });
        GsonBuilder gsonBuilder = fireBuilder.createGsonBuilder();
        gsonBuilder.excludeFieldsWithoutExposeAnnotation();
        gsonBuilder.setPrettyPrinting();
        return gsonBuilder.create();
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
    @Named("default")
    public DefaultTextureBinder textureBinder() {
        return new DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN, 10);
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
