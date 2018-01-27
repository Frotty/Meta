package de.fatox.meta.shader

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import de.fatox.meta.Meta
import de.fatox.meta.api.dao.MetaShaderCompData
import de.fatox.meta.api.dao.RenderBufferData
import de.fatox.meta.api.graphics.RenderBufferHandle
import de.fatox.meta.injection.Inject

/**
 * Created by Frotty on 10.04.2017.
 */
class ShaderComposition(val compositionHandle: FileHandle, var data: MetaShaderCompData) {
    @Inject
    private val shaderLibrary: MetaShaderLibrary? = null

    val bufferHandles = Array<RenderBufferHandle>()

    val outputBuffer: RenderBufferHandle
        get() = bufferHandles.peek()

    init {
        Meta.inject(this)
        loadExisting()
    }

    private fun loadExisting() {
        for (i in 0 until data.renderBuffers.size) {
            val renderBufferData = data.renderBuffers.get(i)
            when (renderBufferData.inType) {
                RenderBufferData.IN.GEOMETRY -> {
                    val metaGeoShader = MetaGeoShader(shaderLibrary!!.getShaderHandle(renderBufferData.metaShaderPath))
                    metaGeoShader.init()
                    bufferHandles.add(RenderBufferHandle(renderBufferData, metaGeoShader))
                }
                RenderBufferData.IN.FULLSCREEN -> {
                    val metaFSShader = MetaFullscreenShader(shaderLibrary!!.getShaderHandle(renderBufferData.metaShaderPath)!!)
                    metaFSShader.init()
                    bufferHandles.add(RenderBufferHandle(renderBufferData, metaFSShader))
                }
            }// TODO
        }
        println("Loaded <" + bufferHandles.size + "> buffers")
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
}
