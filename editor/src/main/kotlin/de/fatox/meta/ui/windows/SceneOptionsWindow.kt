package de.fatox.meta.ui.windows

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisSelectBox
import de.fatox.meta.ide.SceneManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.shader.ShaderComposition
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.tabs.SceneTab

/**
 * Created by Frotty on 02.06.2016.
 */
@Singleton
class SceneOptionsWindow : MetaWindow("Scene Options", true, true) {
    @Inject
    private val editorUI: MetaEditorUI? = null

    @Inject
    private val shaderComposer: MetaShaderComposer? = null

    @Inject
    private val sceneManager: SceneManager? = null
    private var compositionSelectBox: VisSelectBox<ShaderComposition?>? = null
    private fun setup() {
        compositionSelectBox = VisSelectBox()
        contentTable.add(VisLabel("Scene Composition:"))
        contentTable.row()
        contentTable.add(compositionSelectBox).growX()
        contentTable.row()
        loadInitial()
        compositionSelectBox!!.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                val sceneTab = editorUI.getCurrentTab() as SceneTab
                sceneTab.sceneHandle.shaderComposition = compositionSelectBox!!.selected
                sceneManager!!.saveScene(sceneTab.sceneHandle.data)
            }
        })
    }

    private fun loadInitial() {
        compositionSelectBox.setItems(shaderComposer!!.compositions)
        val currentTab = editorUI.getCurrentTab()
        if (currentTab != null && currentTab is SceneTab) {
            val sceneHandle = (currentTab as SceneTab).sceneHandle
            compositionSelectBox!!.setSelected(sceneHandle!!.shaderComposition)
        }
    }

    init {
        setup()
    }
}