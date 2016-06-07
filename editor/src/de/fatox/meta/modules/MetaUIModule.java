package de.fatox.meta.modules;

import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Provides;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.MetaUIRenderer;
import de.fatox.meta.ui.windows.AssetManagerWindow;

/**
 * Created by Frotty on 07.06.2016.
 */
public class MetaUIModule {
    @Provides
    @Singleton
    public AssetManagerWindow assetManagerWindow() {
        return new AssetManagerWindow();
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
