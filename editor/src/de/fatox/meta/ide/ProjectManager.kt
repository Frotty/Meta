package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.EventListener
import de.fatox.meta.api.model.MetaProjectData
import kotlin.reflect.KClass

interface ProjectManager {
	val currentProject: MetaProjectData
	fun loadProject(projectFile: FileHandle): MetaProjectData
	fun saveProject(projectData: MetaProjectData)
	fun newProject(location: FileHandle, projectData: MetaProjectData)
	fun verifyProjectFile(file: FileHandle): Boolean
	val currentProjectRoot: FileHandle
	operator fun <T : Any> get(key: String, type: KClass<out T>): T
	fun save(key: String, obj: Any): FileHandle
	fun relativize(fh: FileHandle): String
	fun addOnLoadListener(listener: EventListener)
}