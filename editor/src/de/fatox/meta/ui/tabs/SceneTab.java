package de.fatox.meta.ui.tabs;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaSceneData;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.camera.ArcCamControl;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.input.MetaInput;
import de.fatox.meta.ui.components.SceneWidget;

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
    private ArcCamControl camControl = new ArcCamControl();
    @Inject
    private MetaInput metaInput;

    public SceneTab(MetaSceneData sceneData) {
        Meta.inject(this);
        this.sceneData = sceneData;
        table = new VisTable();
        table.add(new SceneWidget()).grow();
    }

    @Override
    public String getTabTitle() {
        return sceneData.getName() + " scene";
    }

    @Override
    public Table getContentTable() {
        return table;
    }

    @Override
    public void onDisplay() {
        metaInput.addAdapterForScreen(camControl);
    }
}
