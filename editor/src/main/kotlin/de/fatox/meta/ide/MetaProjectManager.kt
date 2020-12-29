package de.fatox.meta.ide

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.toast.ToastTable
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.model.MetaProjectData
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.ui.MetaEditorUI
import de.fatox.meta.ui.tabs.ProjectHomeTab
import java.nio.file.Paths

/**
 * Created by Frotty on 04.06.2016.
 */
@Singleton
class MetaProjectManager : ProjectManager {
    @Inject
    private val json: Json? = null

    @Inject
    private val uiManager: UIManager? = null

    @Inject
    private val editorUI: MetaEditorUI? = null

    @Inject
    private val metaData: MetaData? = null
    override var currentProject: MetaProjectData? = null
        private set
    override var currentProjectRoot: FileHandle? = null
        private set
    private val onLoadListeners = Array<EventListener>()
    override fun loadProject(projectFile: FileHandle?): MetaProjectData {
        var projectFile = projectFile
        if (!projectFile!!.name().endsWith("metaproject.json")) {
            projectFile = projectFile.child("metaproject.json")
        }
        val metaProjectData = json!!.fromJson(MetaProjectData::class.java, projectFile!!.readString())
        currentProjectRoot = projectFile.parent()
        createFolders(metaProjectData)
        currentProject = metaProjectData
        val lastProjects: Array<String>
        if (metaData!!.has("lastProjects")) {
            lastProjects = metaData.get("lastProjects", Array::class.java)
        } else {
            lastProjects = Array()
        }
        if (!lastProjects.contains(projectFile.path(), false)) {
            lastProjects.add(projectFile.path())
            metaData.save("lastProjects", lastProjects)
        }
        for (listener in onLoadListeners) {
            listener.handle(null)
        }
        editorUI!!.closeTab("home")
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
        child.writeBytes(json!!.toJson(projectData).toByteArray(), false)
        val toastTable = ToastTable()
        toastTable.add(VisLabel("Project created"))
    }

    override fun newProject(location: FileHandle?, projectData: MetaProjectData) {
        currentProjectRoot = location
        saveProject(projectData)
    }

    override fun verifyProjectFile(file: FileHandle?): Boolean {
        try {
            val metaProjectData = json!!.fromJson(MetaProjectData::class.java, file!!.readString())
            if (metaProjectData.isValid) {
                return true
            }
        } catch (e: Exception) {
        }
        return false
    }

    private fun createFolders(projectData: MetaProjectData) {
        currentProjectRoot!!.child("assets").mkdirs()
        currentProjectRoot!!.child("scenes").mkdirs()
    }

    override fun <T> get(key: String?, type: Class<T>?): T? {
        return metaData!![currentProjectRoot!!, key, type]
    }

    override fun save(key: String?, obj: Any?): FileHandle {
        return metaData!!.save(currentProjectRoot!!, key, obj)
    }

    override fun relativize(fh: FileHandle?): String {
        val pathAbsolute = Paths.get(fh!!.file().absolutePath)
        val pathBase = Paths.get(currentProjectRoot!!.file().absolutePath)
        val pathRelative = pathBase.relativize(pathAbsolute)
        return pathRelative.toString()
    }

    override fun addOnLoadListener(listener: EventListener) {
        onLoadListeners.add(listener)
    }

    init {
        inject(this)
    }
}