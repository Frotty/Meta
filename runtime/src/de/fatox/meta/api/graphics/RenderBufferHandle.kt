package de.fatox.meta.api.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.dao.RenderBufferData
import de.fatox.meta.graphics.buffer.MRTFrameBuffer

/**
 * Created by Frotty on 18.04.2017.
 */
class RenderBufferHandle(var data: RenderBufferData, var metaShader: MetaGLShader) {
    private var mrtFrameBuffer: MRTFrameBuffer? = null
    private var frameBuffer: FrameBuffer? = null


    fun rebuild(width: Int, height: Int) {
        println("rebuilt")
        mrtFrameBuffer?.dispose()
        frameBuffer?.dispose()

        val targetsNum = metaShader.shaderHandle.targets.size
        if (targetsNum > 1) {
            // MRT Shader
            mrtFrameBuffer = MRTFrameBuffer(width, height, targetsNum, data.hasDepth)
        } else {
            // Regular Framebuffer
            frameBuffer = FrameBuffer(Pixmap.Format.RGB888, width, height, data.hasDepth)
        }
    }

    val height: Int
        get() {
            if (mrtFrameBuffer != null) {
                return mrtFrameBuffer!!.height
            } else if (frameBuffer != null) {
                return frameBuffer!!.height
            }
            return 0
        }

    val width: Int
        get() {
            if (mrtFrameBuffer != null) {
                return mrtFrameBuffer!!.width
            } else if (frameBuffer != null) {
                return frameBuffer!!.width
            }
            return 0
        }

    private val singleArray = Array<Texture>(1)
    private val emptyArray = Array<Texture>()

    val colorTextures: Array<Texture>
        get() {
            if (mrtFrameBuffer != null) {
                return mrtFrameBuffer!!.colorBufferTextures
            } else if (frameBuffer != null) {
                singleArray.set(0, frameBuffer!!.colorBufferTexture)
                return singleArray
            }
            return emptyArray
        }


    fun begin() {
        if (mrtFrameBuffer != null) {
            mrtFrameBuffer!!.begin()
        } else if (frameBuffer != null) {
            frameBuffer!!.begin()
        }
    }

    fun end(x: Float, y: Float) {
        if (mrtFrameBuffer != null) {
            mrtFrameBuffer!!.end(x.toInt(), y.toInt(), Gdx.graphics.width, Gdx.graphics.height)
        } else if (frameBuffer != null) {
            frameBuffer!!.end()
        }
    }
}
