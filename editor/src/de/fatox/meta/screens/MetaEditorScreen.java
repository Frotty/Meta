package de.fatox.meta.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.dao.MetaAudioVideoData;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.ide.SceneManager;
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
    @Inject
    private MetaData metaData;
    @Inject
    private AssetProvider assetProvider;
    @Inject
    private SceneManager sceneManager;
    private boolean isInited = false;

    private FPSLogger fpsLogger = new FPSLogger();
    @Override
    public void show() {
        if (!isInited) {
            Meta.inject(this);
            uiManager.changeScreen(getClass().getName());
            setupEditorUi();
            isInited = true;
        } else {
            uiManager.changeScreen(getClass().getName());
        }
    }


    @Override
    public void render(float delta) {
        uiRenderer.update();
        clearFrame();
        uiRenderer.draw();
        fpsLogger.log();
    }

    private void clearFrame() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.16862746f, 0.16862746f, 0.16862746f, 1);
        Gdx.gl.glClearDepthf(1.0f);
        Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT |
                (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
    }

    private void setupEditorUi() {
        metaEditorUISetup.setup();
    }

    @Override
    public void resize(int width, int height) {
        if (isInited && width > 120 && height > 0) {
            uiManager.resize(width, height);
            if (!Gdx.graphics.isFullscreen()) {
                MetaAudioVideoData audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData.class);
                audioVideoData.width = width;
                audioVideoData.height = height;
                metaData.save("audioVideoData", audioVideoData);
            }
        }
    }
}
