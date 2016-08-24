package de.fatox.meta.modules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import de.fatox.meta.Meta;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.api.dao.MetaEditorData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.MetaUIRenderer;
import de.fatox.meta.ui.windows.AssetDiscovererWindow;
import de.fatox.meta.ui.windows.ShaderLibraryWindow;

/**
 * Created by Frotty on 07.06.2016.
 */
public class MetaUIModule {
    @Inject
    private Gson gson;

    public MetaUIModule() {
        Meta.inject(this);
    }

    @Provides
    @Singleton
    public ShaderLibraryWindow shaderLibraryWindow() {
        return new ShaderLibraryWindow();
    }

    @Provides
    @Singleton
    public MetaEditorData metaEditorData() {
        FileHandle absolute = Gdx.files.absolute(MetaEditorData.DATA_FILE_NAME);
        MetaEditorData metaEditorData;
        if(absolute.exists()) {
            metaEditorData = gson.fromJson(Gdx.files.absolute(MetaEditorData.DATA_FILE_NAME).readString(), MetaEditorData.class);
            metaEditorData.setFileHandle(absolute);
        } else {
            metaEditorData = new MetaEditorData();
            absolute.writeBytes(gson.toJson(metaEditorData).getBytes(), false);
            metaEditorData.setFileHandle(absolute);
        }
        return metaEditorData;
    }


    @Provides
    @Singleton
    public AssetDiscovererWindow assetManagerWindow() {
        return new AssetDiscovererWindow();
    }

    @Provides
    @Singleton
    public MetaEditorUI metaEditorUI() {
        return new MetaEditorUI();
    }

    @Provides
    @Singleton
    @Named("default")
    public UIRenderer uiRenderer(MetaUIRenderer uiRenderer) {
        return uiRenderer;
    }
}
