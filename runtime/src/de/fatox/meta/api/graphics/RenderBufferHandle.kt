package de.fatox.meta.api.graphics

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.dao.RenderBufferData
import de.fatox.meta.graphics.buffer.MRTFrameBuffer

/**
 * Created by Frotty on 18.04.2017.
 */
class RenderBufferHandle(var data: RenderBufferData, var metaShader: MetaGLShader?) {

    private var mrtFrameBuffer: MRTFrameBuffer? = null
    private var frameBuffer: FrameBuffer? = null


    fun rebuild(width: Int, height: Int) {
        mrtFrameBuffer?.dispose()
        frameBuffer?.dispose()

        val targetsNum = metaShader?.shaderHandle?.targets?.size
        if (targetsNum!! > 1) {
            // MRT Shader
            mrtFrameBuffer = MRTFrameBuffer(width, height, targetsNum, data.hasDpeth)
        } else {
            // Regular Framebuffer
            frameBuffer = FrameBuffer(Pixmap.Format.RGB888, width, height, data.hasDpeth)
        }
    }

    val height: Float
        get() {
            if (mrtFrameBuffer != null) {
                return mrtFrameBuffer!!.height.toFloat()
            } else if (frameBuffer != null) {
                return frameBuffer!!.height.toFloat()
            }
            return 0f
        }

    val width: Float
        get() {
            if (mrtFrameBuffer != null) {
                return mrtFrameBuffer!!.width.toFloat()
            } else if (frameBuffer != null) {
                return frameBuffer!!.width.toFloat()
            }
            return 0f
        }

    private val singleArray = Array<Texture>(1)

    val colorTextures: Array<Texture>?
        get() {
            if (mrtFrameBuffer != null) {
                return mrtFrameBuffer!!.colorBufferTextures
            } else if (frameBuffer != null) {
                singleArray.set(0, frameBuffer!!.colorBufferTexture)
                return singleArray
            }
            return null
        }


    fun begin() {
        if (mrtFrameBuffer != null) {
            mrtFrameBuffer!!.begin()
        } else if (frameBuffer != null) {
            frameBuffer!!.begin()
        }
    }

    fun end() {
        if (mrtFrameBuffer != null) {
            mrtFrameBuffer!!.end()
        } else if (frameBuffer != null) {
            frameBuffer!!.end()
        }
    }
}
