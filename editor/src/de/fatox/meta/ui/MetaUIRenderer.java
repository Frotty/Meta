package de.fatox.meta.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
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

    @Inject
    public MetaUIRenderer() {
        Meta.inject(this);
        log.debug(TAG, "Injected MetaUi");
        VisUI.load();

        FileChooser.setDefaultPrefsName("de.fatox.meta");
//        skin.add("default-font", fontProvider.getFont(14));
//        skin.add("default", fontProvider.getFont(14));
        log.debug(TAG, "Loaded VisUi");
        VisUI.setDefaultTitleAlign(Align.center);
        stage = new Stage(new ScreenViewport());
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
    public Stage getStage() {
        return stage;
    }
}
