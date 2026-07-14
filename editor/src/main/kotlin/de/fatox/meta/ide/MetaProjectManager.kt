package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import de.fatox.meta.api.extensions.writeBytesAtomic
import de.fatox.meta.api.model.MetaProjectData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.lastProjectsKey
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.tabs.ProjectHomeTab
import java.nio.file.Paths
import kotlin.reflect.KClass

/**
 * Created by Frotty on 04.06.2016.
 */
class MetaProjectManager : ProjectManager {
	private val json: Json by lazyInject()
	private val editorUI: MetaEditorUI by lazyInject()
	private val metaData: MetaData by lazyInject()
	private val uiManager: UIManager by lazyInject()

	override lateinit var currentProject: MetaProjectData
		private set
	override val hasCurrentProject: Boolean
		get() = ::currentProject.isInitialized
	override lateinit var currentProjectRoot: FileHandle
		private set
	private val onLoadListeners = Array<EventListener>()

	override fun loadProject(projectFile: FileHandle): MetaProjectData? {
		val realFile = if (!projectFile.name().endsWith("metaproject.json")) {
			projectFile.child("metaproject.json")
		} else projectFile

		val metaProjectData = try {
			json.fromJson(MetaProjectData::class.java, realFile.readString())
		} catch (e: Exception) {
			uiManager.showToast("Failed to load project: ${e.message}")
			return null
		}
		currentProjectRoot = realFile.parent()
		createFolders(metaProjectData)
		currentProject = metaProjectData
		val lastProjects = if (metaData.has(lastProjectsKey)) metaData[lastProjectsKey] else Array()
		if (!lastProjects.contains(realFile.path(), false)) {
			lastProjects.add(realFile.path())
			metaData.save(lastProjectsKey, lastProjects)
		}
		onLoadListeners.forEach { it.handle(null) }
		editorUI.tryCloseTab("home")
		editorUI.addTab(ProjectHomeTab(metaProjectData))
		return metaProjectData
	}

	override fun saveProject(projectData: MetaProjectData) {
		val root = currentProjectRoot
		if (!root.exists()) {
			root.mkdirs()
		}
		// root is already the project's own folder once loadProject() has run; only nest a new child
		// folder the first time (from newProject()), or a re-save after loading would create a duplicate.
		var child = if (root.name() == projectData.name) root else root.child(projectData.name).also { it.mkdirs() }
		currentProjectRoot = child
		createFolders(projectData)
		child = child.child(MetaProjectData.PROJECT_FILE_NAME)
		child.writeBytesAtomic(json.toJson(projectData).toByteArray())
		uiManager.showToast("Project created")
	}

	override fun newProject(location: FileHandle, projectData: MetaProjectData) {
		currentProjectRoot = location
		saveProject(projectData)
	}

	override fun verifyProjectFile(file: FileHandle): Boolean {
		try {
			val metaProjectData = json.fromJson(MetaProjectData::class.java, file.readString())
			if (metaProjectData.isValid) {
				return true
			}
		} catch (e: Exception) {
			return false
		}
		return false
	}

	private fun createFolders(projectData: MetaProjectData) {
		currentProjectRoot.child("assets").mkdirs()
		currentProjectRoot.child("scenes").mkdirs()
	}

	override fun <T : Any> get(key: String, type: KClass<out T>): T {
		return metaData.load(key, type, currentProjectRoot) as T
	}

	override fun save(key: String, obj: Any): FileHandle {
		return metaData.save(key, obj, currentProjectRoot)
	}

	override fun relativize(fh: FileHandle): String {
		val pathAbsolute = Paths.get(fh.file().absolutePath)
		val pathBase = Paths.get(currentProjectRoot.file().absolutePath)
		val pathRelative = pathBase.relativize(pathAbsolute)
		return pathRelative.toString()
	}

	override fun addOnLoadListener(listener: EventListener) {
		onLoadListeners.add(listener)
	}
}
