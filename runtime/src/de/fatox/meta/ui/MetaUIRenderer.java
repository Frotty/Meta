package de.fatox.meta.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.input.MetaInput;

public class MetaUIRenderer implements UIRenderer {
    private static final String TAG = "MetaUiRenderer";
    @Inject
    @Log
    private Logger log;
    @Inject
    private MetaInput metaInput;
    @Inject
    private FontProvider fontProvider;

    private Stage stage;

    public MetaUIRenderer() {
        Meta.inject(this);
        log.debug(TAG, "Injected MetaUi");
        VisUI.load();

        FileChooser.setDefaultPrefsName("de.fatox.meta");
        log.debug(TAG, "Loaded VisUi");
        VisUI.setDefaultTitleAlign(Align.center);
        stage = new Stage(new ScreenViewport());
        stage.getRoot().addCaptureListener(new InputListener() {
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                if (!((event.getTarget() instanceof TextField) || (event.getTarget() instanceof ScrollPane))) stage.setScrollFocus(null);
                return false;
            }});
        metaInput.addGlobalAdapter(stage);
    }


    @Override
    public void addActor(Actor actor) {
        try {
            stage.addActor(actor);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        stage.act(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void draw() {
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public Camera getCamera() {
        return stage.getCamera();
    }

}
