package de.fatox.meta.ui.tabs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.Meta;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.dao.MetaSceneData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.SceneWidget;

/**
 * Created by Frotty on 13.06.2016.
 */
public class SceneTab extends MetaTab {
    private MetaSceneData sceneData;
    private Table table;
    @Inject
    private PerspectiveCamera perspectiveCamera;
    @Inject
    private UIRenderer uiRenderer;

    public SceneTab(MetaSceneData sceneData) {
        Meta.inject(this);
        this.sceneData = sceneData;
        table = new VisTable();
        table.add(new SceneWidget()).grow();
    }

    @Override
    public String getTabTitle() {
        return sceneData.getName();
    }

    @Override
    public Table getContentTable() {
        return table;
    }

    @Override
    public void onDisplay() {
        Gdx.input.setInputProcessor(new InputMultiplexer(uiRenderer.getStage(), new CameraInputController(perspectiveCamera)));
    }
}
