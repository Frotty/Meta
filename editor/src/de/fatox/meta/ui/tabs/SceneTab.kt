package de.fatox.meta.ui.tabs

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.VisTable
import de.fatox.meta.api.MetaInputProcessor

import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.camera.ArcCamControl
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.input.MetaInput
import de.fatox.meta.shader.MetaSceneHandle
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.components.SceneWidget
import de.fatox.meta.ui.windows.*

/**
 * Created by Frotty on 13.06.2016.
 */
class SceneTab(sceneHandle: MetaSceneHandle) : MetaTab() {
	private val perspectiveCamera: PerspectiveCamera by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()
	private val metaInput: MetaInputProcessor by lazyInject()
	private val editorUI: MetaEditorUI by lazyInject()

	private val camControl = ArcCamControl()
	val sceneHandle: MetaSceneHandle
	private val table: Table
	override fun getTabTitle(): String {
		return sceneHandle.sceneFile.name()
	}

	override fun getContentTable(): Table {
		return table
	}

	override fun onShow() {
		metaInput.addAdapterForScreen(camControl)
		editorUI.metaToolbar.clear()
		editorUI.metaToolbar.addAvailableWindow(AssetDiscovererWindow::class, null)
		editorUI.metaToolbar.addAvailableWindow(ShaderLibraryWindow::class, null)
		editorUI.metaToolbar.addAvailableWindow(ShaderComposerWindow::class, null)
		editorUI.metaToolbar.addAvailableWindow(SceneOptionsWindow::class, null)
		editorUI.metaToolbar.addAvailableWindow(PrimitivesWindow::class, null)
	}

	override fun onHide() {
		metaInput.removeAdapterFromScreen(camControl)
	}

	init {

		this.sceneHandle = sceneHandle
		table = VisTable()
		table.add(SceneWidget(this.sceneHandle)).grow()
		table.invalidate()
		//        table.debugAll();
	}
}