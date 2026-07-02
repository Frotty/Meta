package de.fatox.meta.ui.windows

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import de.fatox.meta.ide.SceneManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.shader.ShaderComposition
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaSelectBox
import de.fatox.meta.ui.tabs.SceneTab

/**
 * Created by Frotty on 02.06.2016.
 */
class SceneOptionsWindow : MetaWindow("Scene Options", true, true) {
	private val editorUI: MetaEditorUI by lazyInject()
	private val shaderComposer: MetaShaderComposer by lazyInject()
	private val sceneManager: SceneManager by lazyInject()

	private var compositionSelectBox: MetaSelectBox<ShaderComposition?> = MetaSelectBox()
	private fun setup() {
		contentTable.add(MetaLabel("Scene Composition:", 14))
		contentTable.row()
		contentTable.add(compositionSelectBox).growX()
		contentTable.row()
		loadInitial()
		compositionSelectBox.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				val sceneTab = editorUI.currentTab as SceneTab
				sceneTab.sceneHandle.shaderComposition = compositionSelectBox.selected
				sceneManager.saveScene(sceneTab.sceneHandle.data)
			}
		})
	}

	private fun loadInitial() {
		compositionSelectBox.items = shaderComposer.compositions as Array<ShaderComposition?>
		val currentTab = editorUI.currentTab
		if (currentTab != null && currentTab is SceneTab) {
			val sceneHandle = currentTab.sceneHandle
			compositionSelectBox.selected = sceneHandle.shaderComposition
		}
	}

	init {
		setup()
	}
}
