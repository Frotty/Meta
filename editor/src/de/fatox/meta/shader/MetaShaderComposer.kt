package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import de.fatox.meta.Meta
import de.fatox.meta.api.dao.MetaShaderCompData
import de.fatox.meta.api.dao.RenderBufferData
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
    private val projectManager: ProjectManager? = null
    @Inject
    private val json: Json? = null
    @Inject
    private val uiManager: UIManager? = null
    @Inject
    private val shaderLibrary: MetaShaderLibrary? = null

    val compositions = Array<ShaderComposition>(2)

    var currentComposition: ShaderComposition? = null
        set(value) {
            field = value
            notifyListeners()
        }

    init {
        Meta.inject(this)
        projectManager!!.addOnLoadListener { evt ->
            loadProjectCompositions()
            false
        }
        Gdx.app.postRunnable { this.loadProjectCompositions() }
    }

    fun loadProjectCompositions() {
        if (projectManager!!.currentProject != null) {
            val compositionFolder = projectManager.currentProjectRoot.child("meta/compositions/")
            if (compositionFolder.exists()) {
                for (metaComp in compositionFolder.list { pathname -> pathname.name.endsWith(META_COMP_SUFFIX) }) {
                    val compositionData = json!!.fromJson(MetaShaderCompData::class.java, metaComp.readString())
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
            saveComposition(composition)
            val window = uiManager!!.getWindow(ShaderComposerWindow::class.java)
            window?.addComposition(composition)
            currentComposition = composition
        }
    }

    private fun saveComposition(composition: ShaderComposition): FileHandle {
        return projectManager!!.save("meta/compositions/" + composition.data.name + META_COMP_SUFFIX, composition.data)
    }

    fun getComposition(compositionPath: String): ShaderComposition? {
        var compositionPath = compositionPath
        if (!compositionPath.contains(File.separator)) {
            compositionPath = META_COMP_PATH + compositionPath + META_COMP_SUFFIX
        }
        for (comp in compositions) {
            val relativizedPath = projectManager!!.relativize(comp.compositionHandle)
            if (relativizedPath.equals(compositionPath, ignoreCase = true)) {
                return comp
            }
        }
        return null
    }

    fun addRenderBuffer(data: RenderBufferData): RenderBufferHandle {
        val bufferHandle = RenderBufferHandle(data, null)
        currentComposition!!.addBufferHandle(bufferHandle)
        saveComposition(currentComposition!!)
        return bufferHandle
    }

    fun newShaderComposition(name: String) {
        val fileHandle = projectManager!!.currentProjectRoot.child(META_COMP_PATH + name + META_COMP_SUFFIX)
        val metaShaderCompData = MetaShaderCompData(name)
        val shaderComposition = ShaderComposition(fileHandle, metaShaderCompData)
        addComposition(shaderComposition)
    }

    companion object {
        val META_COMP_SUFFIX = ".mco"
        val META_COMP_PATH = "meta\\compositions\\"
    }

}
