package de.fatox.meta.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.ui.MetaEditorUI;

public class MetaEditorScreen extends ScreenAdapter {
    private static final String TAG = "EditorScreen";
    @Inject
    @Log
    private Logger log;
    @Inject
    private UIManager uiManager;
    @Inject
    private UIRenderer uiRenderer;

    @Inject
    private SpriteBatch spriteBatch;

    @Inject
    private FontProvider fontProvider;
    @Inject
    private MetaEditorUI metaEditorUISetup;

    @Override
    public void show() {
        Meta.inject(this);
        setupEditorUi();
        Gdx.input.setInputProcessor(new InputMultiplexer(uiRenderer.getStage()));
    }

    private void update() {
        uiRenderer.update();
    }

    @Override
    public void render(float delta) {
        update();
        clearFrame();
        uiRenderer.draw();
    }

    private void clearFrame() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.16862746f, 0.16862746f, 0.16862746f, 1);
        Gdx.gl.glClearDepthf(1.0f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
    }

    private void setupEditorUi() {
        metaEditorUISetup.setup();
    }

    @Override
    public void resize(int width, int height) {
        uiManager.resize(width, height);
//        Gdx.input.setInputProcessor(uiRenderer.getInputProcessor());
    }
}
