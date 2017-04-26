package de.fatox.meta.ui.components;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.shader.EditorSceneRenderer;
import de.fatox.meta.shader.MetaSceneHandle;

/**
 * Created by Frotty on 16.06.2016.
 */
public class SceneWidget extends Widget {
    private Renderer renderer;

    public SceneWidget(MetaSceneHandle sceneHandle) {
        renderer = new EditorSceneRenderer(sceneHandle);
    }

    @Override
    public void layout() {
        invalidate();
        renderer.rebuild((int) getWidth(), (int) getHeight());
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.end();
        validate();
        renderer.render(getX(), getY());
        batch.begin();
    }

}
