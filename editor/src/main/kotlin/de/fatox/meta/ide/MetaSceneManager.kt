package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Json

import de.fatox.meta.api.extensions.writeBytesAtomic
import de.fatox.meta.api.model.MetaSceneData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.shader.MetaSceneHandle
import de.fatox.meta.shader.MetaShaderComposer
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.tabs.SceneTab
import java.io.File

private val FOLDER = "scenes" + File.separator
private const val EXTENSION = "metascene"

/**
 * Created by Frotty on 15.06.2016.
 */
class MetaSceneManager : SceneManager {
	private val projectManager: ProjectManager by lazyInject()
	private val metaEditorUI: MetaEditorUI by lazyInject()
	private val assetDiscoverer: AssetDiscoverer by lazyInject()
	private val shaderComposer: MetaShaderComposer by lazyInject()
	private val json: Json by lazyInject()
	private val uiManager: UIManager by lazyInject()

	override fun createNew(name: String): MetaSceneHandle {
		val currentComposition = shaderComposer.currentComposition
		val path = if (currentComposition != null) projectManager.relativize(currentComposition.compositionHandle) else ""
		val metaSceneData = MetaSceneData(name, path, Vector3.Y, true)
		val sceneFile = projectManager.currentProjectRoot.child("$FOLDER$name.$EXTENSION")
		sceneFile.writeBytesAtomic(json.toJson(metaSceneData).toByteArray())
		val metaSceneHandle = MetaSceneHandle(metaSceneData, shaderComposer.currentComposition, sceneFile)
		metaEditorUI.addTab(SceneTab(metaSceneHandle))
		return metaSceneHandle
	}

	override fun loadScene(projectFile: FileHandle) {
		if (metaEditorUI.tryFocusTab(projectFile.name())) return
		val metaSceneData = try {
			json.fromJson(MetaSceneData::class.java, projectFile.readString())
		} catch (e: Exception) {
			uiManager.showToast("Failed to load scene: ${e.message}")
			return
		}
		val composition = shaderComposer.getComposition(metaSceneData.compositionPath)
		metaEditorUI.addTab(SceneTab(MetaSceneHandle(metaSceneData, composition, projectFile)))
	}

	override fun saveScene(sceneHandle: MetaSceneHandle) {
		sceneHandle.sceneFile.writeBytesAtomic(json.toJson(sceneHandle.data).toByteArray())
	}

	init {
		assetDiscoverer.addOpenListener(EXTENSION, ::loadScene)
	}
}