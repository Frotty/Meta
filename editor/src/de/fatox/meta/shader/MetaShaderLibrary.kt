package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.Meta
import de.fatox.meta.api.dao.GLShaderData
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.listener.MetaNotifier

@Singleton
class MetaShaderLibrary : MetaNotifier() {
    @Inject
    private lateinit var projectManager: ProjectManager
    @Inject
    private lateinit var json: Json

    private val loadedShaders = ObjectMap<String, GLShaderHandle>()
    private val metaShaders = Array<GLShaderHandle>()

    val defaultShaderPath: String
        get() = projectManager.relativize(loadedShaders.values().next().shaderHandle)

    init {
        Meta.inject(this)
        projectManager.addOnLoadListener { evt ->
            loadDefaultShader()
            loadProjectShaders()
            false
        }
        Gdx.app.postRunnable { this.loadProjectShaders() }
    }

    private fun loadDefaultShader() {
        if (projectManager.currentProject != null) {
            loadedShaders.clear()
            metaShaders.clear()

            loadShader(Gdx.files.internal("shaders/Default.msh"))
        }
        notifyListeners()
    }

    fun loadShader(shaderHandle: FileHandle): GLShaderHandle? {
        val shaderData = json.fromJson(GLShaderData::class.java, shaderHandle.readString())
        val projRoot = projectManager.currentProjectRoot
        val vertHandle = projRoot.child(shaderData.vertexFilePath)
        val fragHandle = projRoot.child(shaderData.fragmentFilePath)
        if (vertHandle.exists() && !vertHandle.isDirectory && fragHandle.exists() && !fragHandle.isDirectory) {
            val handle = GLShaderHandle(shaderHandle, vertHandle, fragHandle, shaderData)
            metaShaders.add(handle)
            loadedShaders.put(projectManager.relativize(shaderHandle), handle)
            return handle
        }
        return null
    }

    fun newShader(data: GLShaderData): GLShaderHandle? {
        val newShaderHandle = projectManager.save(INTERNAL_SHADER_PATH + data.name + META_SHADER_SUFFIX, data)
        val loadShader = loadShader(newShaderHandle)
        notifyListeners()
        return loadShader
    }


    fun getLoadedShaders(): Array<GLShaderHandle> {
        return metaShaders
    }

    fun getFirstShader(): GLShaderHandle {
        return metaShaders.first()
    }

    fun getShaderHandle(metaShaderPath: String): GLShaderHandle? {
        return if (loadedShaders.containsKey(metaShaderPath)) {
            loadedShaders.get(metaShaderPath)
        } else null
    }

    fun loadProjectShaders() {
        if (projectManager.currentProject != null) {
            loadedShaders.clear()
            metaShaders.clear()

            val shaderFolder = projectManager.currentProjectRoot.child(INTERNAL_SHADER_PATH)
            if (shaderFolder.exists()) {
                for (metaShaderDef in shaderFolder.list { pathname -> pathname.name.endsWith(META_SHADER_SUFFIX) }) {
                    loadShader(metaShaderDef)
                }
            }
        }
        notifyListeners()
    }

    companion object {
        private val META_SHADER_SUFFIX = ".msh"
        private val INTERNAL_SHADER_PATH = "meta/shaders"
    }
}
