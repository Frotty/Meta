package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import de.fatox.meta.Meta
import de.fatox.meta.api.dao.MetaShaderCompData
import de.fatox.meta.api.dao.RenderBufferData
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.listener.MetaNotifier
import de.fatox.meta.ui.windows.ShaderComposerWindow
import java.io.File

/**
 * Created by Frotty on 10.04.2017.
 */
@Singleton
class MetaShaderComposer : MetaNotifier() {
    @Inject
    private lateinit var projectManager: ProjectManager
    @Inject
    private lateinit var json: Json
    @Inject
    private lateinit var uiManager: UIManager
    @Inject
    private lateinit var shaderLibrary: MetaShaderLibrary

    val compositions = Array<ShaderComposition>(2)

    var currentComposition: ShaderComposition? = null
        set(value) {
            field = value
            notifyListeners()
        }

    init {
        Meta.inject(this)
        projectManager.addOnLoadListener { evt ->
            loadProjectCompositions()
            false
        }
        Gdx.app.postRunnable { this.loadProjectCompositions() }
    }

    fun loadProjectCompositions() {
        if (projectManager.currentProject != null) {
            compositions.clear()
            val compositionFolder = projectManager.currentProjectRoot.child("meta/compositions/")
            if (compositionFolder.exists()) {
                for (metaComp in compositionFolder.list { pathname -> pathname.name.endsWith(META_COMP_SUFFIX) }) {
                    val compositionData = json.fromJson(MetaShaderCompData::class.java, metaComp.readString())
                    if (compositionData != null) {
                        addComposition(ShaderComposition(metaComp, compositionData))
                    }
                }
            }
        }
    }

    fun addComposition(composition: ShaderComposition?) {
        if (composition != null) {
            compositions.add(composition)
            val window = uiManager.getWindow(ShaderComposerWindow::class.java)
            window?.addComposition(composition)
            currentComposition = composition
        }
    }

    private fun saveComposition(composition: ShaderComposition): FileHandle {
        return projectManager.save("meta/compositions/" + composition.data.name + META_COMP_SUFFIX, composition.data)
    }

    fun getComposition(compositionPath: String): ShaderComposition? {
        var path = compositionPath
        if (!path.contains(File.separator)) {
            path = META_COMP_PATH + path + META_COMP_SUFFIX
        }
        for (comp in compositions) {
            val relativizedPath = projectManager.relativize(comp.compositionHandle)
            if (relativizedPath.equals(path, ignoreCase = true)) {
                return comp
            }
        }
        return null
    }

    fun setType(handle: RenderBufferHandle, intype: RenderBufferData.IN) {
        if (handle.data.inType != intype) {
            currentComposition?.let {
                it.setType(handle, intype)
                saveComposition(it)
            }
        }
    }

    fun addRenderBuffer(data: RenderBufferData): RenderBufferHandle {
        val bufferHandle = RenderBufferHandle(data, MetaGeoShader(shaderLibrary.getFirstShader()))
        currentComposition?.let {
            currentComposition?.addBufferHandle(bufferHandle)
            saveComposition(it)
        }
        notifyListeners()
        return bufferHandle
    }

    fun newShaderComposition(name: String) {
        val fileHandle = projectManager.currentProjectRoot.child(META_COMP_PATH + name + META_COMP_SUFFIX)
        val metaShaderCompData = MetaShaderCompData(name)
        val shaderComposition = ShaderComposition(fileHandle, metaShaderCompData)
        addComposition(shaderComposition)
        saveComposition(shaderComposition)
        notifyListeners()
    }

    companion object {
        val META_COMP_SUFFIX = ".mco"
        val META_COMP_PATH = "meta\\compositions\\"
    }

    fun removeBufferHandle(handle: RenderBufferHandle) {
        currentComposition?.let {
            it.removeBufferHandle(handle)
            saveComposition(it)
            notifyListeners()
        }
    }

    fun changeShader(handle: RenderBufferHandle, selected: GLShaderHandle?) {
        if (shaderLibrary.getShaderHandle(handle.data.metaShaderPath) != selected) {
            currentComposition?.let {
                it.setShader(handle, selected)
                saveComposition(it)
            }
        }
    }


}
