package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Json

import de.fatox.meta.api.model.MetaSceneData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.MetaSceneHandle
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.tabs.SceneTab
import java.io.File

/**
 * Created by Frotty on 15.06.2016.
 */
class MetaSceneManager : SceneManager {
	private val projectManager: ProjectManager by lazyInject()
	private val metaEditorUI: MetaEditorUI by lazyInject()
	private val assetDiscoverer: AssetDiscoverer by lazyInject()
	private val shaderComposer: MetaShaderComposer by lazyInject()
	private val json: Json by lazyInject()

	override fun createNew(name: String): MetaSceneHandle {
		val currentComposition = shaderComposer.currentComposition
		val path = if (currentComposition != null) projectManager.relativize(currentComposition.compositionHandle) else ""
		val metaSceneData = MetaSceneData(name, path, Vector3.Y, true)
		val sceneFile = projectManager.currentProjectRoot.child("$FOLDER$name.$EXTENSION")
		sceneFile.writeBytes(json.toJson(metaSceneData).toByteArray(), false)
		val metaSceneHandle = MetaSceneHandle(metaSceneData, shaderComposer.currentComposition, sceneFile)
		metaEditorUI.addTab(SceneTab(metaSceneHandle))
		return metaSceneHandle
	}

	override fun loadScene(sceneFile: FileHandle) {
		if (metaEditorUI.hasTab(sceneFile.name())) {
			metaEditorUI.focusTab(sceneFile.name())
			return
		}
		val metaSceneData = json.fromJson(MetaSceneData::class.java, sceneFile.readString())
		val composition = shaderComposer.getComposition(metaSceneData.compositionPath)
		metaEditorUI.addTab(SceneTab(MetaSceneHandle(metaSceneData, composition, sceneFile)))
	}

	override fun saveScene(sceneData: MetaSceneData) {}

	companion object {
		private val FOLDER = "scenes" + File.separator
		private const val EXTENSION = "metascene"
	}

	init {

		assetDiscoverer.addOpenListener(EXTENSION) { sceneFile: FileHandle -> loadScene(sceneFile) }
	}
}