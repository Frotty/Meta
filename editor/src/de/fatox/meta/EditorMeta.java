package de.fatox.meta;

import com.badlogic.gdx.Gdx;
import de.fatox.meta.api.dao.MetaAudioVideoData;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.modules.MetaEditorModule;
import de.fatox.meta.modules.MetaUIModule;
import de.fatox.meta.screens.MetaEditorScreen;

public class EditorMeta extends Meta {
    @Inject
    private MetaData metaData;

    public EditorMeta() {
        super();
        addModule(new MetaEditorModule());
        addModule(new MetaUIModule());
    }

    @Override
    public void create() {
        inject(this);
        if(!metaData.has("audioVideoData")) {
            metaData.save("audioVideoData", new MetaAudioVideoData());
        }
        MetaAudioVideoData audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData.class);
        if (audioVideoData.isFullscreen()) {
            if (!Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayModes()[audioVideoData.getDisplayMode()]);
            }
        } else {
            Gdx.graphics.setUndecorated(audioVideoData.isBorderless());
            Gdx.graphics.setWindowedMode(audioVideoData.getWidth(), audioVideoData.getHeight());
        }
        Gdx.graphics.setVSync(audioVideoData.isVsyncEnabled());
        changeScreen(new MetaEditorScreen());
    }

}
