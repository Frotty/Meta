package de.fatox.meta.modules;

import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.MetaEditorUI;

/**
 * Created by Frotty on 07.06.2016.
 */
public class MetaUIModule {
    public MetaUIModule() {
        Meta.inject(this);
    }

    @Provides
    @Singleton
    public MetaData metaData() {
        return new MetaData();
    }

    @Provides
    @Singleton
    public MetaEditorUI metaEditorUI() {
        return new MetaEditorUI();
    }

}
