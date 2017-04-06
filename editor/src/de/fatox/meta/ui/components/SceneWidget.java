package de.fatox.meta.ui.components;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import de.fatox.meta.Meta;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 16.06.2016.
 */
public class SceneWidget extends Widget {
    @Inject
    private Renderer renderer;

    public SceneWidget() {
        Meta.inject(this);
    }

    @Override
    public void layout() {
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
