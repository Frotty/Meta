package de.fatox.meta.ui;

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

    private boolean first = true;

    public SceneWidget() {
        Meta.inject(this);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.end();
        validate();
        if (first) {
            first = false;
            renderer.rebuild((int) getWidth(), (int) getHeight());
        }
        renderer.render(getX(), getY(), getWidth(), getHeight());
        batch.begin();
    }
}
