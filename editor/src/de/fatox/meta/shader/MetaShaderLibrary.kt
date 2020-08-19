package de.fatox.meta.shader

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.model.GLShaderData
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.injection.Singleton
import de.fatox.meta.listener.MetaNotifier

@Singleton
class MetaShaderLibrary : MetaNotifier() {
	private val projectManager: ProjectManager by lazyInject()
	private val json: Json by lazyInject()

	private val loadedShaders = ObjectMap<String, GLShaderHandle>()
	private val metaShaders = Array<GLShaderHandle>()

	val defaultShaderPath: String
		get() = projectManager.relativize(loadedShaders.values().next().shaderHandle)

	init {

		projectManager.addOnLoadListener {
			loadedShaders.clear()
			metaShaders.clear()
			loadDefaultShader()
			loadProjectShaders()
			false
		}
		Gdx.app.postRunnable { this.loadProjectShaders() }
	}

	private fun loadDefaultShader() {
		val internal = Gdx.files.internal("shaders/Default.msh")
		val loadShader = loadShader(internal, false)
		loadedShaders.put("shaders/Default.msh", loadShader)
		notifyListeners()
	}

	fun loadShader(shaderHandle: FileHandle, projectFile: Boolean): GLShaderHandle? {
		val shaderData = json.fromJson(GLShaderData::class.java, shaderHandle.readString())
		val projRoot = projectManager.currentProjectRoot
		val vertHandle = projRoot.child(shaderData.vertexFilePath)
		val fragHandle = projRoot.child(shaderData.fragmentFilePath)
		if (vertHandle.exists() && !vertHandle.isDirectory && fragHandle.exists() && !fragHandle.isDirectory) {
			val handle = GLShaderHandle(shaderHandle, vertHandle, fragHandle, shaderData)
			metaShaders.add(handle)
			loadedShaders.put(if (projectFile) projectManager.relativize(shaderHandle) else shaderHandle.path(), handle)
			return handle
		}
		return null
	}

	fun newShader(data: GLShaderData): GLShaderHandle? {
		val newShaderHandle = projectManager.save(INTERNAL_SHADER_PATH + "/" + data.name + META_SHADER_SUFFIX, data)
		val loadShader = loadShader(newShaderHandle, true)
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
			val shaderFolder = projectManager.currentProjectRoot.child(INTERNAL_SHADER_PATH)
			if (shaderFolder.exists()) {
				for (metaShaderDef in shaderFolder.list { pathname -> pathname.name.endsWith(META_SHADER_SUFFIX) }) {
					loadShader(metaShaderDef, true)
				}
			}
		}
		notifyListeners()
	}

	companion object {
		private const val META_SHADER_SUFFIX = ".msh"
		private const val INTERNAL_SHADER_PATH = "meta/shaders"
	}
}
