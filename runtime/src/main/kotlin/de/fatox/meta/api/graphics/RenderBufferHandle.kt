package de.fatox.meta.api.graphics

import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.model.RenderBufferData
import de.fatox.meta.graphics.buffer.MRTFrameBuffer
import de.fatox.meta.graphics.buffer.MetaFrameBuffer
import de.fatox.meta.graphics.buffer.MultisampleFBO
import de.fatox.meta.graphics.buffer.NoMetaFrameBuffer

private val log = MetaLoggerFactory.logger {}
private val emptyArray: Array<out Texture> = Array(0)

/**
 * Created by Frotty on 18.04.2017.
 */
class RenderBufferHandle(var data: RenderBufferData, var metaShader: MetaGLShader) {

	private var metaFrameBuffer: MetaFrameBuffer = NoMetaFrameBuffer
	var colorTextures: Array<out Texture> = emptyArray
		private set

	fun rebuild(width: Int, height: Int) {
		log.debug { "rebuilt width=$width height=$height" }
		metaFrameBuffer.dispose()

		val targetsNum = metaShader.shaderHandle.targets.size
		if (targetsNum > 1) {
			// MRT Shader
			metaFrameBuffer =
				MRTFrameBuffer(width, height, targetsNum, data.hasDepth).also { colorTextures = it.colorBufferTextures }
		} else {
			// Regular Framebuffer
			val builder = MultisampleFBO.FrameBufferBuilder(width, height)
			builder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE)
			if (data.hasDepth) {
				builder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL30.GL_FLOAT)
			}
			metaFrameBuffer =
				builder.build().also { colorTextures = Array<Texture>(1).apply { add(it.colorBufferTexture) } }
		}
	}

	val height: Int
		get() = metaFrameBuffer.height

	val width: Int
		get() = metaFrameBuffer.width


	fun begin() {
		metaFrameBuffer.begin()
	}

	fun end(x: Float, y: Float) {
		// TODO use x and y parameters?
		metaFrameBuffer.end()
	}

	fun getFBO(): Int = metaFrameBuffer.getFBO()
}
