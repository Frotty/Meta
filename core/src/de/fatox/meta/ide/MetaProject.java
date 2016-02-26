package de.fatox.meta.ide;

import com.badlogic.gdx.ScreenAdapter;
import de.fatox.meta.Meta;
import de.fatox.meta.api.ide.persist.PersistentValue;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.ide.persist.Persist;

public class MetaProject extends ScreenAdapter {

    @Persist(key = "Project Name", defaultValue = "New Project")
    private PersistentValue<String> projectName;

    @Persist(key = "Project Renderer", defaultValue = "BufferedRenderer")
    private PersistentValue<Renderer> projectRenderer;

    @Override
    public void show() {
        Meta.inject(this);
    }


}
