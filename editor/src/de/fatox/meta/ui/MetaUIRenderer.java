package de.fatox.meta.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.ide.ui.UIRenderer;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;
import de.fatox.meta.input.MetaInput;

public class MetaUIRenderer implements UIRenderer {
    private Stage stage;

    @Inject
    @Log
    private Logger log;

    @Inject
    private MetaInput metaInput;

    @Inject
    public MetaUIRenderer() {
        Meta.inject(this);
        VisUI.load();
        setupEditorUI();
    }

    private void setupEditorUI() {
        log.info("MetaUIRenderer", "VisUi loaded");
        this.stage = new Stage(new ScreenViewport());
        log.info("MetaUIRenderer", "Stage loaded");
        MetaToolbar metaToolbar = new MetaToolbar();
        log.info("MetaUIRenderer", "Toolbar created");
        VisTable visTable = new VisTable(true);
        metaToolbar.menuBar.getTable().pack();
        visTable.row().height(19);
        visTable.add(metaToolbar.menuBar.getTable()).left();
        visTable.setFillParent(true);
        visTable.top().left();
        visTable.row().height(2);
        visTable.add(new Separator()).expandX().fillX().height(2).padTop(-6).padBottom(-6);
        visTable.row();
        visTable.add().expand().fill();
        visTable.pack();
        visTable.debugAll();
        stage.addActor(visTable);
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
        setupEditorUI();
    }

    @Override
    public InputProcessor getInputProcessor() {
        return stage;
    }
}
