package de.fatox.meta.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.camera.ArcCamControl;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.ui.MetaEditorMenuBar;
import de.fatox.meta.ui.windows.PrimitivesWindow;

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
    private Renderer renderer;

    @Inject
    private SpriteBatch spriteBatch;


    @Override
    public void show() {
        Meta.inject(this);
        setupEdiotrUi();
        Gdx.input.setInputProcessor(new InputMultiplexer(uiRenderer.getStage(), new ArcCamControl()));
    }

    private void update() {
        uiManager.update();
    }

    @Override
    public void render(float delta) {
        update();
        clearFrame();
//        spriteBatch.begin();
//        spriteBatch.end();
        renderer.render();
        uiManager.draw();
    }

    private void clearFrame() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.16862746f, 0.16862746f, 0.16862746f, 1);
        Gdx.gl.glClearDepthf(1.0f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
    }

    private void setupEdiotrUi() {
        log.info(TAG, "VisUi loaded");
        log.info(TAG, "Stage loaded");
        MetaEditorMenuBar metaToolbar = new MetaEditorMenuBar();
        log.info(TAG, "Toolbar created");
        uiManager.addMenuBar(metaToolbar.menuBar);
        uiManager.addWindow(new PrimitivesWindow());
    }

    @Override
    public void resize(int width, int height) {
        uiManager.resize(width, height);
        renderer.rebuild();
//        Gdx.input.setInputProcessor(uiRenderer.getInputProcessor());
    }
}
