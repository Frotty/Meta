package de.fatox.meta.graphics.buffer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable

class MRTFrameBuffer(
        /** width  */
        val width: Int,
        /** height  */
        val height: Int, textureCount: Int, hasDepth: Boolean) : Disposable {
    private var frameBuffer: MultisampleFBO? = null

    val colorBufferTextures: Array<Texture>
        get() = frameBuffer!!.textureAttachments

    init {
        build(textureCount, hasDepth)
    }

    private fun build(numTextures: Int, hasDepth: Boolean) {
        val frameBufferBuilder = MultisampleFBO.FrameBufferBuilder(width, height)
        for (i in 0 until numTextures) {
            frameBufferBuilder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE)
        }
        if (hasDepth) {
            frameBufferBuilder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL30.GL_FLOAT)
			frameBufferBuilder.addBasicDepthRenderBuffer()
		}

        frameBuffer = frameBufferBuilder.build()
    }

    /** Releases all resources associated with the FrameBuffer.  */
    override fun dispose() {
        frameBuffer!!.dispose()
    }

    /** Makes the frame buffer current so everything gets drawn to it.  */
    fun bind() {
        frameBuffer!!.bind()
    }

    /** Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it.  */
    fun begin() {
        bind()
        setFrameBufferViewport()
    }

    /** Sets viewport to the dimensions of framebuffer. Called by [.begin].  */
    protected fun setFrameBufferViewport() {
        Gdx.gl20.glViewport(0, 0, frameBuffer!!.width, frameBuffer!!.height)
    }

    /**
     * Unbinds the framebuffer and sets viewport sizes, all drawing will be performed to the normal framebuffer from here on.
     *
     * @param x      the x-axis position of the viewport in pixels
     * @param y      the y-asis position of the viewport in pixels
     * @param width  the width of the viewport in pixels
     * @param height the height of the viewport in pixels
     */
    @JvmOverloads
    fun end(x: Int = 0, y: Int = 0, width: Int = Gdx.graphics.width, height: Int = Gdx.graphics.height) {
        unbind()
        Gdx.gl20.glViewport(x, y, width, height)
    }

    fun getFBO(): Int {
        return frameBuffer!!.framebufferHandle
    }

    companion object {

        /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.  */
        fun unbind() {
            FrameBuffer.unbind()
        }
    }
}
/** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.  */
