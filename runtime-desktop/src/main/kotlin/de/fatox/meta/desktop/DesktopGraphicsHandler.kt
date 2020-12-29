package de.fatox.meta.desktop

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import com.sun.org.apache.xpath.internal.operations.Mult
import de.fatox.meta.api.GraphicsHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.graphics.buffer.MultisampleFBO
import org.lwjgl.opengl.GL32
import org.slf4j.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val log: Logger = MetaLoggerFactory.logger {}

class DesktopGraphicsHandler : GraphicsHandler {
	override fun createTexture(fbo: MultisampleFBO, attachmentSpec: MultisampleFBO.FrameBufferTextureAttachmentSpec): Int {
		try {
			val texture = Gdx.gl.glGenTexture()
			Gdx.gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, texture)
			GL32.glTexImage2DMultisample(
				GL32.GL_TEXTURE_2D_MULTISAMPLE,
				2,
				attachmentSpec.internalFormat,
				fbo.getWidth(),
				fbo.getHeight(),
				true
			)
			Gdx.gl.glTexParameterf(texture, GL20.GL_TEXTURE_MIN_FILTER, Texture.TextureFilter.Nearest.glEnum.toFloat())
			Gdx.gl.glTexParameterf(texture, GL20.GL_TEXTURE_MAG_FILTER, Texture.TextureFilter.Nearest.glEnum.toFloat())
			Gdx.gl.glTexParameterf(texture, GL20.GL_TEXTURE_WRAP_S, Texture.TextureWrap.ClampToEdge.glEnum.toFloat())
			Gdx.gl.glTexParameterf(texture, GL20.GL_TEXTURE_WRAP_T, Texture.TextureWrap.ClampToEdge.glEnum.toFloat())
			Gdx.gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, 0)
			return texture
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return -1
	}

	override fun build(fbo: MultisampleFBO) {
		val gl = Gdx.gl20
		fbo.checkValidBuilder()

		// iOS uses a different framebuffer handle! (not necessarily 0)
		if (!MultisampleFBO.defaultFramebufferHandleInitialized) {
			MultisampleFBO.defaultFramebufferHandleInitialized = true
			if (Gdx.app.type == Application.ApplicationType.iOS) {
				val intbuf =
					ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder()).asIntBuffer()
				gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, intbuf)
				MultisampleFBO.defaultFramebufferHandle = intbuf[0]
			} else {
				MultisampleFBO.defaultFramebufferHandle = 0
			}
		}
		fbo.framebufferHandle = gl.glGenFramebuffer()
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, fbo.framebufferHandle)
		val imBuilder = GLFrameBuffer.FrameBufferBuilder(fbo.getWidth(), fbo.getHeight())
		val width = fbo.bufferBuilder.width
		val height = fbo.bufferBuilder.height
		if (fbo.bufferBuilder.hasDepthRenderBuffer) {
			fbo.depthbufferHandle = gl.glGenRenderbuffer()
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, fbo.depthbufferHandle)
			Gdx.gl30.glRenderbufferStorageMultisample(
				GL20.GL_RENDERBUFFER,
				4,
				fbo.bufferBuilder.depthRenderBufferSpec!!.internalFormat,
				width,
				height
			)
		}
		if (fbo.bufferBuilder.hasStencilRenderBuffer) {
			fbo.stencilbufferHandle = gl.glGenRenderbuffer()
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, fbo.stencilbufferHandle)
			Gdx.gl30.glRenderbufferStorageMultisample(
				GL20.GL_RENDERBUFFER,
				4,
				fbo.bufferBuilder.stencilRenderBufferSpec!!.internalFormat,
				width,
				height
			)
		}
		if (fbo.bufferBuilder.hasPackedStencilDepthRenderBuffer) {
			fbo.depthStencilPackedBufferHandle = gl.glGenRenderbuffer()
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, fbo.depthStencilPackedBufferHandle)
			Gdx.gl30.glRenderbufferStorageMultisample(
				GL20.GL_RENDERBUFFER,
				4,
				fbo.bufferBuilder.packedStencilDepthRenderBufferSpec!!.internalFormat,
				width,
				height
			)
		}
		fbo.isMRT = fbo.bufferBuilder.textureAttachmentSpecs.size > 1
		fbo.isMRT = true
		var colorTextureCounter = 0
		if (fbo.isMRT) {
			for (attachmentSpec in fbo.bufferBuilder.textureAttachmentSpecs) {
				val texture = fbo.createTexture(attachmentSpec)
				fbo.textureAttachments++
				if (attachmentSpec.isColorTexture) {
					gl.glFramebufferTexture2D(
						GL20.GL_FRAMEBUFFER,
						GL30.GL_COLOR_ATTACHMENT0 + colorTextureCounter,
						GL32.GL_TEXTURE_2D_MULTISAMPLE,
						texture,
						0
					)
					imBuilder
						.addColorTextureAttachment(
							GL30.GL_RGBA8,
							GL30.GL_RGBA,
							GL30.GL_UNSIGNED_BYTE
						)
					colorTextureCounter++
				} else if (attachmentSpec.isDepth) {
					gl.glFramebufferTexture2D(
						GL20.GL_FRAMEBUFFER,
						GL20.GL_DEPTH_ATTACHMENT,
						GL30.GL_TEXTURE_2D,
						texture,
						0
					)
					imBuilder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL30.GL_FLOAT)
				} else if (attachmentSpec.isStencil) {
					gl.glFramebufferTexture2D(
						GL20.GL_FRAMEBUFFER,
						GL20.GL_STENCIL_ATTACHMENT,
						GL20.GL_TEXTURE_2D,
						texture,
						0
					)
				}
			}
		} else {
			val texture = fbo.createTexture(fbo.bufferBuilder.textureAttachmentSpecs.first())
			fbo.textureAttachments = 1
			imBuilder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE)
			gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, texture)
			Gdx.gl30.glFramebufferTexture2D(
				GL20.GL_FRAMEBUFFER,
				GL20.GL_COLOR_ATTACHMENT0,
				GL32.GL_TEXTURE_2D_MULTISAMPLE,
				texture,
				0
			)
		}
		if (fbo.isMRT) {
			val buffer = BufferUtils.newIntBuffer(colorTextureCounter)
			for (i in 0 until colorTextureCounter) {
				buffer.put(GL30.GL_COLOR_ATTACHMENT0 + i)
			}
			buffer.position(0)
			Gdx.gl30.glDrawBuffers(colorTextureCounter, buffer)
		}
		if (fbo.bufferBuilder.hasDepthRenderBuffer) {
			gl.glFramebufferRenderbuffer(
				GL20.GL_FRAMEBUFFER,
				GL20.GL_DEPTH_ATTACHMENT,
				GL20.GL_RENDERBUFFER,
				fbo.depthbufferHandle
			)
		}
		if (fbo.bufferBuilder.hasStencilRenderBuffer) {
			gl.glFramebufferRenderbuffer(
				GL20.GL_FRAMEBUFFER,
				GL20.GL_STENCIL_ATTACHMENT,
				GL20.GL_RENDERBUFFER,
				fbo.stencilbufferHandle
			)
		}
		if (fbo.bufferBuilder.hasPackedStencilDepthRenderBuffer) {
			gl.glFramebufferRenderbuffer(
				GL20.GL_FRAMEBUFFER,
				GL30.GL_DEPTH_STENCIL_ATTACHMENT,
				GL20.GL_RENDERBUFFER,
				fbo.depthStencilPackedBufferHandle
			)
		}
		gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0)
		gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, 0)
		fbo.assertShit()
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, MultisampleFBO.defaultFramebufferHandle)
		MultisampleFBO.addManagedFrameBuffer(Gdx.app, fbo)
		fbo.nonMultisampledFbo = imBuilder.build()
	}

	private fun MultisampleFBO.checkValidBuilder() {
		val runningGL30 = Gdx.graphics.isGL30Available
		if (!runningGL30) {
			if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
				throw GdxRuntimeException("Packed Stencil/Render render buffers are not available on GLES 2.0")
			}
			if (bufferBuilder.textureAttachmentSpecs.size > 1) {
				throw GdxRuntimeException("Multiple render targets not available on GLES 2.0")
			}
			for (spec in bufferBuilder.textureAttachmentSpecs) {
				if (spec.isDepth) throw GdxRuntimeException("Depth texture FrameBuffer Attachment not available on GLES 2.0")
				if (spec.isStencil) throw GdxRuntimeException("Stencil texture FrameBuffer Attachment not available on GLES 2.0")
				if (spec.isFloat) {
					if (!Gdx.graphics.supportsExtension("OES_texture_float")) {
						throw GdxRuntimeException("Float texture FrameBuffer Attachment not available on GLES 2.0")
					}
				}
			}
		}
	}

	private fun MultisampleFBO.assertShit() {
		val gl = Gdx.gl
		val result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER)
		if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
			if (hasDepthStencilPackedBuffer) {
				gl.glDeleteBuffer(depthStencilPackedBufferHandle)
			} else {
				if (bufferBuilder.hasDepthRenderBuffer) gl.glDeleteRenderbuffer(depthbufferHandle)
				if (bufferBuilder.hasStencilRenderBuffer) gl.glDeleteRenderbuffer(stencilbufferHandle)
			}
			gl.glDeleteFramebuffer(framebufferHandle)
			check(result != GL20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) { "frame buffer couldn't be constructed: incomplete attachment" }
			check(result != GL20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS) { "frame buffer couldn't be constructed: incomplete dimensions" }
			check(result != GL20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) { "frame buffer couldn't be constructed: missing attachment" }
			check(result != GL20.GL_FRAMEBUFFER_UNSUPPORTED) { "frame buffer couldn't be constructed: unsupported combination of formats" }
			throw IllegalStateException("frame buffer couldn't be constructed: unknown error $result")
		}
	}
}
