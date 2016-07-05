package de.fatox.meta;

import com.badlogic.gdx.Gdx;
import de.fatox.meta.modules.MetaEditorModule;
import de.fatox.meta.modules.MetaUIModule;

public class EditorMeta extends Meta {

    public EditorMeta() {
        super();
        addModule(new MetaEditorModule());
        addModule(new MetaUIModule());
        System.out.println("fuck u biotch");
    }

    public void setWindowData(int width, int height) {
        Gdx.graphics.setWindowedMode(width, height);
    }

}
