package de.fatox.meta.shader

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.graphics.GLShaderHandle
import de.fatox.meta.api.graphics.MetaGLShader
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.api.model.MetaShaderCompData
import de.fatox.meta.api.model.RenderBufferData
import de.fatox.meta.ide.ProjectManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Created by Frotty on 10.04.2017.
 */
class ShaderComposition(val compositionHandle: FileHandle, var data: MetaShaderCompData) {
	private val shaderLibrary: MetaShaderLibrary by lazyInject()
	private val projectManager: ProjectManager by lazyInject()

	private val log = MetaLoggerFactory.logger {  }

	val bufferHandles = Array<RenderBufferHandle>()

	val outputBuffer: RenderBufferHandle?
		get() = if (bufferHandles.isEmpty) null else bufferHandles.peek()

	init {
		loadExisting()
	}

	private fun loadExisting() {
		for (i in 0 until data.renderBuffers.size) {
			val renderBufferData = data.renderBuffers.get(i)
			bufferHandles.add(RenderBufferHandle(renderBufferData, assignShader(renderBufferData)))
		}
		log.debug("Loaded <" + bufferHandles.size + "> buffers")
	}

	private fun assignShader(renderBufferData: RenderBufferData): MetaGLShader {
		var metaShaderHandle = shaderLibrary.getShaderHandle(renderBufferData.metaShaderPath)
		if (metaShaderHandle == null) {
			metaShaderHandle = shaderLibrary.getFirstShader()
		}
		return when (renderBufferData.inType) {
			RenderBufferData.IN.GEOMETRY -> {
				val metaGeoShader = MetaGeoShader(metaShaderHandle)
				metaGeoShader.init()
				metaGeoShader
			}
			RenderBufferData.IN.FULLSCREEN -> {
				val metaFSShader = MetaFullscreenShader(metaShaderHandle)
				metaFSShader.init()
				metaFSShader
			}
		}
	}

	fun setType(handle: RenderBufferHandle, intype: RenderBufferData.IN) {
		handle.data.inType = intype
		handle.metaShader.dispose()
		handle.metaShader = assignShader(handle.data)
	}

	fun addBufferHandle(bufferHandle: RenderBufferHandle) {
		if (!bufferHandles.contains(bufferHandle, true)) {
			bufferHandles.add(bufferHandle)
			data.renderBuffers.add(bufferHandle.data)
		}
	}

	override fun toString(): String {
		return data.name
	}

	fun removeBufferHandle(handle: RenderBufferHandle) {
		bufferHandles.removeValue(handle, true)
		data.renderBuffers.removeValue(handle.data, true)
	}

	fun setShader(handle: RenderBufferHandle, selected: GLShaderHandle) {
		handle.data.metaShaderPath = projectManager.relativize(selected.shaderHandle)
		handle.metaShader.dispose()
		handle.metaShader = assignShader(handle.data)
	}
}
