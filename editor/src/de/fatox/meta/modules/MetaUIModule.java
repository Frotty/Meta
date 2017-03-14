package de.fatox.meta.modules;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.Gson;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.api.dao.MetaData2;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.MetaEditorUI;

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
    public MetaData metaEditorData() {
        FileHandle absolute = Gdx.files.absolute(MetaData.DATA_FILE_NAME);
        MetaData metaEditorData;
        if(absolute.exists()) {
            metaEditorData = gson.fromJson(Gdx.files.absolute(MetaData.DATA_FILE_NAME).readString(), MetaData.class);
            metaEditorData.setFileHandle(absolute);
        } else {
            metaEditorData = new MetaData();
            absolute.writeBytes(gson.toJson(metaEditorData).getBytes(), false);
            metaEditorData.setFileHandle(absolute);
        }
        return metaEditorData;
    }

    @Provides
    @Singleton
    public MetaData2 metaData() {
        return new MetaData2();
    }

    @Provides
    @Singleton
    public MetaEditorUI metaEditorUI() {
        return new MetaEditorUI();
    }

}
