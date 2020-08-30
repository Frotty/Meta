package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.toast.ToastTable
import de.fatox.meta.api.model.MetaProjectData
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.tabs.ProjectHomeTab
import java.nio.file.Paths

/**
 * Created by Frotty on 04.06.2016.
 */
object MetaProjectManager : ProjectManager {
	private val json: Json by lazyInject()
	private val editorUI: MetaEditorUI by lazyInject()
	private val metaData: MetaData by lazyInject()

	private var currentProject: MetaProjectData? = null
	private var currentProjectRoot: FileHandle? = null
	private val onLoadListeners = Array<EventListener>()
	override fun getCurrentProject(): MetaProjectData {
		return currentProject!!
	}

	override fun loadProject(projectFile: FileHandle): MetaProjectData {
		val realFile = if (!projectFile.name().endsWith("metaproject.json")) {
			projectFile.child("metaproject.json")
		} else projectFile

		val metaProjectData = json.fromJson(MetaProjectData::class.java, realFile.readString())
		currentProjectRoot = realFile.parent()
		createFolders(metaProjectData)
		currentProject = metaProjectData
		val lastProjects: Array<String> = if (metaData.has("lastProjects")) {
			metaData["lastProjects"]
		} else {
			Array()
		}
		if (!lastProjects.contains(realFile.path(), false)) {
			lastProjects.add(realFile.path())
			metaData.save("lastProjects", lastProjects)
		}
		for (listener in onLoadListeners) {
			listener.handle(null)
		}
		editorUI.closeTab("home")
		editorUI.addTab(ProjectHomeTab(metaProjectData))
		return metaProjectData
	}

	override fun saveProject(projectData: MetaProjectData) {
		val root = currentProjectRoot
		if (!root!!.exists()) {
			root.mkdirs()
		}
		var child = root.child(projectData.name)
		child.mkdirs()
		currentProjectRoot = child
		createFolders(projectData)
		child = child.child(MetaProjectData.PROJECT_FILE_NAME)
		child.writeBytes(json.toJson(projectData).toByteArray(), false)
		val toastTable = ToastTable()
		toastTable.add(VisLabel("Project created"))
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

	override fun getCurrentProjectRoot(): FileHandle {
		return currentProjectRoot!!
	}

	private fun createFolders(projectData: MetaProjectData) {
		currentProjectRoot!!.child("assets").mkdirs()
		currentProjectRoot!!.child("scenes").mkdirs()
	}

	override fun <T> get(key: String, type: Class<T>): T {
		return metaData[currentProjectRoot!!, key, type] as T
	}

	override fun save(key: String, obj: Any): FileHandle {
		return metaData.save(currentProjectRoot!!, key, obj)
	}

	override fun relativize(fh: FileHandle): String {
		val pathAbsolute = Paths.get(fh.file().absolutePath)
		val pathBase = Paths.get(currentProjectRoot!!.file().absolutePath)
		val pathRelative = pathBase.relativize(pathAbsolute)
		return pathRelative.toString()
	}

	override fun addOnLoadListener(listener: EventListener) {
		onLoadListeners.add(listener)
	}
}