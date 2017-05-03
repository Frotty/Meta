package de.fatox.meta.ui.windows;

import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.shader.MetaSceneHandle;
import de.fatox.meta.shader.MetaShaderComposer;
import de.fatox.meta.shader.ShaderComposition;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.tabs.SceneTab;
import com.kotcrab.vis.ui.widget.VisLabel;
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

        add(new VisLabel("Scene Composition:"));
        add(compositionSelectBox).growX();
        row();


        loadInitial();
    }

    private void loadInitial() {
        Tab currentTab = editorUI.getCurrentTab();
        if(currentTab != null && currentTab instanceof SceneTab) {
            MetaSceneHandle sceneHandle = ((SceneTab) currentTab).getSceneHandle();

        }
    }
}
