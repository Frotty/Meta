package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import de.fatox.meta.ide.SceneManager;
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
    @Inject
    private SceneManager sceneManager;

    private VisSelectBox<ShaderComposition> compositionSelectBox;

    public SceneOptionsWindow() {
        super("Scene Options", true, true);
        setup();
    }

    private void setup() {
        compositionSelectBox = new VisSelectBox<>();
        getContentTable().add(new VisLabel("Scene Composition:"));
        getContentTable().row();
        getContentTable().add(compositionSelectBox).growX();
        getContentTable().row();

        loadInitial();

        compositionSelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SceneTab sceneTab = (SceneTab) editorUI.getCurrentTab();
                sceneTab.getSceneHandle().setShaderComposition(compositionSelectBox.getSelected());
                sceneManager.saveScene(sceneTab.getSceneHandle().getData());
            }
        });
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
