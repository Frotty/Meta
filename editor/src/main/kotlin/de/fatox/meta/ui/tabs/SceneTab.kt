package de.fatox.meta.ui.tabs

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.api.MetaInputProcessor

import de.fatox.meta.api.graphics.Renderer
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.camera.ArcCamControl
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.EditorSceneRenderer
import de.fatox.meta.shader.MetaSceneHandle
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.components.MetaTable
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
	private val renderer: Renderer by lazyInject()

	private val camControl = ArcCamControl()
	val sceneHandle: MetaSceneHandle
	private val sceneWidget: SceneWidget
	private val table: Table
	override val tabTitle: String get() = sceneHandle.sceneFile.name()
	override val contentTable: Table get() = table

	override fun onShow() {
		// EditorSceneRenderer is a single app-wide singleton: re-assign its scene on every (re)show, not just at
		// construction, so it renders THIS tab's scene even if another SceneTab was created/shown in between.
		(renderer as EditorSceneRenderer).sceneHandle = sceneHandle
		metaInput.addScreenInputProcessor(camControl)
		editorUI.metaToolbar.clear()
		editorUI.metaToolbar.addAvailableWindow(AssetDiscovererWindow::class)
		editorUI.metaToolbar.addAvailableWindow(ShaderLibraryWindow::class)
		editorUI.metaToolbar.addAvailableWindow(ShaderComposerWindow::class)
		editorUI.metaToolbar.addAvailableWindow(SceneOptionsWindow::class)
		editorUI.metaToolbar.addAvailableWindow(PrimitivesWindow::class)
	}

	override fun onHide() {
		metaInput.removeScreenInputProcessor(camControl)
	}

	override fun dispose() {
		sceneWidget.dispose()
	}

	init {
		this.sceneHandle = sceneHandle
		sceneWidget = SceneWidget(sceneHandle)
		table = MetaTable()
		table.add(sceneWidget).grow()
		table.invalidate()
		//        table.debugAll();
	}
}
