package de.fatox.meta;

import com.badlogic.gdx.Gdx;
import de.fatox.meta.api.model.MetaAudioVideoData;
import de.fatox.meta.assets.MetaData;
import de.fatox.meta.graphics.model.MDXConverter;
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
        audioVideoData.apply();
        changeScreen(new MetaEditorScreen());

        String convert = MDXConverter.INSTANCE.convert(Gdx.files.internal("models/tcBox.mdx"));
        System.out.println(convert);
    }

}
