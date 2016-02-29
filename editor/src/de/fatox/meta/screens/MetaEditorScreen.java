package de.fatox.meta.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import de.fatox.meta.Meta;
import de.fatox.meta.api.ide.ui.UIRenderer;
import de.fatox.meta.injection.Inject;

public class MetaEditorScreen extends ScreenAdapter {

    @Inject
    private UIRenderer uiRenderer;

    @Override
    public void show() {
        Meta.inject(this);
    }

    @Override
    public void render(float delta) {

        update();
        uiRenderer.draw();
    }

    private void update() {
        uiRenderer.update();
    }

    @Override
    public void resize(int width, int height) {
        uiRenderer.resize(width, height);
        Gdx.input.setInputProcessor(uiRenderer.getInputProcessor());
    }
}
