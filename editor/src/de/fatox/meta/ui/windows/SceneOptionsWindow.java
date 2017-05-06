package de.fatox.meta.ui.windows;

import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.shader.MetaSceneHandle;
import de.fatox.meta.shader.MetaShaderComposer;
import de.fatox.meta.shader.ShaderComposition;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.tabs.SceneTab;

/**
 * Created by Frotty on 02.06.2016.
 */
@Singleton
public class SceneOptionsWindow extends MetaWindow {
    @Inject
    private MetaEditorUI editorUI;
    @Inject
    private MetaShaderComposer shaderComposer;

    private VisSelectBox<ShaderComposition> compositionSelectBox;

    public SceneOptionsWindow() {
        super("Scene Options", true, true);
        setup();
    }

    private void setup() {
        compositionSelectBox = new VisSelectBox<>();
        contentTable.add(new VisLabel("Scene Composition:"));
        contentTable.row();
        contentTable.add(compositionSelectBox).growX();
        contentTable.row();


        loadInitial();
    }

    private void loadInitial() {
        compositionSelectBox.setItems(shaderComposer.getCompositions());

        Tab currentTab = editorUI.getCurrentTab();
        if (currentTab != null && currentTab instanceof SceneTab) {
            MetaSceneHandle sceneHandle = ((SceneTab) currentTab).getSceneHandle();
            compositionSelectBox.setSelected(sceneHandle.getShaderComposition());
        }
    }
}
