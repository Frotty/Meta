package de.fatox.meta.api.graphics

import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.model.RenderBufferData
import de.fatox.meta.graphics.buffer.MRTFrameBuffer
import de.fatox.meta.graphics.buffer.MultisampleFBO

/**
 * Created by Frotty on 18.04.2017.
 */
class RenderBufferHandle(var data: RenderBufferData, var metaShader: MetaGLShader) {
    private var mrtFrameBuffer: MRTFrameBuffer? = null
    private var frameBuffer: MultisampleFBO? = null


    fun rebuild(width: Int, height: Int) {
        println("rebuilt width=$width height=$height")
        mrtFrameBuffer?.dispose()
        frameBuffer?.dispose()

        val targetsNum = metaShader.shaderHandle.targets.size
        if (targetsNum > 1) {
            // MRT Shader
            mrtFrameBuffer = MRTFrameBuffer(width, height, targetsNum, data.hasDepth)
        } else {
            // Regular Framebuffer
            val builder = MultisampleFBO.FrameBufferBuilder(width, height)
            builder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE)
            if(data.hasDepth) {
                builder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL30.GL_FLOAT)
            }
            frameBuffer = builder.build()
        }
    }

    val height: Int
        get() = when {
			mrtFrameBuffer != null -> mrtFrameBuffer!!.height
			frameBuffer != null -> frameBuffer!!.height
			else -> 0
		}

    val width: Int
        get() = when {
			mrtFrameBuffer != null -> mrtFrameBuffer!!.width
			frameBuffer != null -> frameBuffer!!.width
			else -> 0
		}

    private val singleArray = Array<Texture>(1)

    init {
        singleArray.size = 1
    }

    private val emptyArray: Array<out Texture> = Array()

    val colorTextures: Array<out Texture>
        get() = when {
			mrtFrameBuffer != null -> mrtFrameBuffer!!.colorBufferTextures
			frameBuffer != null -> singleArray.apply { set(0, frameBuffer!!.colorBufferTexture) }
			else -> emptyArray
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
            mrtFrameBuffer!!.end()
        } else if (frameBuffer != null) {
            frameBuffer!!.end()
        }
    }

    fun getFBO(): Int = when {
		mrtFrameBuffer != null -> mrtFrameBuffer!!.getFBO()
		frameBuffer != null -> frameBuffer!!.framebufferHandle
		else -> -1
	}
}
