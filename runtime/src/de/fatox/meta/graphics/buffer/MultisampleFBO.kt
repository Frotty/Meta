/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fatox.meta.graphics.buffer

import com.badlogic.gdx.Application
import com.badlogic.gdx.Application.ApplicationType
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import org.lwjgl.opengl.GL32
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 *
 *
 * Encapsulates OpenGL ES 2.0 frame buffer objects. This is a simple helper class which should cover most FBO uses. It
 * will automatically create a gltexture for the color attachment and a renderbuffer for the depth buffer. You can get a
 * hold of the gltexture by [MultisampleFBO.getColorBufferTexture]. This class will only work with OpenGL ES 2.0.
 *
 * FrameBuffers are managed. In case of an OpenGL context loss, which only happens on Android when a user switches to
 * another application or receives an incoming call, the framebuffer will be automatically recreated.
 *
 * A FrameBuffer must be disposed if it is no longer needed
 *
 *
 * @author mzechner, realitix
 */
class MultisampleFBO(var bufferBuilder: GLFrameBufferBuilder<out MultisampleFBO>) : Disposable {
	/**
	 * the color buffer texture
	 */
	private var textureAttachments = 0
	/**
	 * @return The OpenGL handle of the framebuffer (see [GL20.glGenFramebuffer])
	 */
	/**
	 * the framebuffer handle
	 */
	var framebufferHandle = 0

	/**
	 * the depthbuffer render object handle
	 */
	var depthbufferHandle = 0

	/**
	 * the stencilbuffer render object handle
	 */
	var stencilbufferHandle = 0

	/**
	 * the depth stencil packed render buffer object handle
	 */
	var depthStencilPackedBufferHandle = 0

	/**
	 * if has depth stencil packed buffer
	 */
	var hasDepthStencilPackedBuffer = false

	/**
	 * if multiple texture attachments are present
	 */
	var isMRT = false
	var nonMultisampledFbo: FrameBuffer? = null
	val colorBufferTexture: Texture?
		get() {
			if (nonMultisampledFbo == null) {
				// TODO fix
				return null
			}
			Gdx.gl30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferHandle)
			Gdx.gl30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, nonMultisampledFbo!!.framebufferHandle)
			Gdx.gl30.glBlitFramebuffer(0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST)
			Gdx.gl30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0)
			Gdx.gl30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0)
			FrameBuffer.unbind()
			return nonMultisampledFbo!!.colorBufferTexture
		}

	/**
	 * Return the Texture attachments attached to the fbo
	 */
	fun getTextureAttachments(): Array<Texture> {
		checkError(Gdx.gl.glGetError())
		FrameBuffer.unbind()
		Gdx.gl30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferHandle)
		Gdx.gl30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, nonMultisampledFbo!!.framebufferHandle)
		val intBuffer = BufferUtils.newIntBuffer(1)
		for (i in 0 until textureAttachments) {
			if (i == textureAttachments - 1 && textureAttachments > 1) {
				Gdx.gl30.glReadBuffer(GL30.GL_NONE)
				intBuffer.put(GL30.GL_NONE)
				checkError(Gdx.gl.glGetError())
			} else {
				Gdx.gl30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0 + i)
				intBuffer.put(GL30.GL_COLOR_ATTACHMENT0 + i)
			}
			intBuffer.rewind()
			Gdx.gl30.glDrawBuffers(1, intBuffer)
			if (i == textureAttachments - 1 && textureAttachments > 1) {
				Gdx.gl30.glBlitFramebuffer(0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST)
			} else {
				Gdx.gl30.glBlitFramebuffer(0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST)
			}
		}
		FrameBuffer.unbind()
		return nonMultisampledFbo!!.textureAttachments
	}

	private fun checkError(error: Int) {
		if (error == GL20.GL_INVALID_ENUM) {
			System.err.println("Invalid enum")
		} else if (error == GL20.GL_INVALID_VALUE) {
			System.err.println("Invalid val")
		} else if (error == GL20.GL_INVALID_OPERATION) {
			System.err.println("Invalid op")
		} else if (error == GL20.GL_INVALID_FRAMEBUFFER_OPERATION) {
			System.err.println("Invalid fbo op")
		} else if (error == GL20.GL_OUT_OF_MEMORY) {
			System.err.println("Out of memory")
		}
	}

	/**
	 * Override this method in a derived class to set up the backing texture as you like.
	 */
	fun createTexture(attachmentSpec: FrameBufferTextureAttachmentSpec): Int {
		try {
			val texture = Gdx.gl.glGenTexture()
			Gdx.gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, texture)
			GL32.glTexImage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, 2, attachmentSpec.internalFormat, getWidth(), getHeight(), true)
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

	/**
	 * Override this method in a derived class to dispose the backing texture as you like.
	 */
	fun disposeColorTexture(colorTexture: Texture) {
		colorTexture.dispose()
	}

	fun build() {
		val gl = Gdx.gl20
		checkValidBuilder()

		// iOS uses a different framebuffer handle! (not necessarily 0)
		if (!defaultFramebufferHandleInitialized) {
			defaultFramebufferHandleInitialized = true
			if (Gdx.app.type == ApplicationType.iOS) {
				val intbuf = ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder()).asIntBuffer()
				gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, intbuf)
				defaultFramebufferHandle = intbuf[0]
			} else {
				defaultFramebufferHandle = 0
			}
		}
		framebufferHandle = gl.glGenFramebuffer()
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle)
		val imBuilder = GLFrameBuffer.FrameBufferBuilder(getWidth(), getHeight())
		val width = bufferBuilder.width
		val height = bufferBuilder.height
		if (bufferBuilder.hasDepthRenderBuffer) {
			depthbufferHandle = gl.glGenRenderbuffer()
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthbufferHandle)
			Gdx.gl30.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, 4, bufferBuilder.depthRenderBufferSpec!!.internalFormat, width, height)
		}
		if (bufferBuilder.hasStencilRenderBuffer) {
			stencilbufferHandle = gl.glGenRenderbuffer()
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, stencilbufferHandle)
			Gdx.gl30.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, 4, bufferBuilder.stencilRenderBufferSpec!!.internalFormat, width, height)
		}
		if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
			depthStencilPackedBufferHandle = gl.glGenRenderbuffer()
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle)
			Gdx.gl30.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, 4, bufferBuilder.packedStencilDepthRenderBufferSpec!!.internalFormat, width, height)
		}
		isMRT = bufferBuilder.textureAttachmentSpecs.size > 1
		isMRT = true
		var colorTextureCounter = 0
		if (isMRT) {
			for (attachmentSpec in bufferBuilder.textureAttachmentSpecs) {
				val texture = createTexture(attachmentSpec)
				textureAttachments++
				if (attachmentSpec.isColorTexture) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + colorTextureCounter, GL32.GL_TEXTURE_2D_MULTISAMPLE,
						texture, 0)
					imBuilder
						.addColorTextureAttachment(
							GL30.GL_RGBA8,
							GL30.GL_RGBA,
							GL30.GL_UNSIGNED_BYTE
						)
					colorTextureCounter++
				} else if (attachmentSpec.isDepth) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL30.GL_TEXTURE_2D, texture, 0)
					imBuilder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL30.GL_FLOAT)
				} else if (attachmentSpec.isStencil) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_TEXTURE_2D, texture, 0)
				}
			}
		} else {
			val texture = createTexture(bufferBuilder.textureAttachmentSpecs.first())
			textureAttachments = 1
			imBuilder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE)
			gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, texture)
			Gdx.gl30.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D_MULTISAMPLE, texture, 0)
		}
		if (isMRT) {
			val buffer = BufferUtils.newIntBuffer(colorTextureCounter)
			for (i in 0 until colorTextureCounter) {
				buffer.put(GL30.GL_COLOR_ATTACHMENT0 + i)
			}
			buffer.position(0)
			Gdx.gl30.glDrawBuffers(colorTextureCounter, buffer)
		}
		if (bufferBuilder.hasDepthRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_RENDERBUFFER, depthbufferHandle)
		}
		if (bufferBuilder.hasStencilRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, stencilbufferHandle)
		}
		if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle)
		}
		gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0)
		gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, 0)
		assertShit()
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle)
		addManagedFrameBuffer(Gdx.app, this)
		nonMultisampledFbo = imBuilder.build()
	}

	private fun assertShit() {
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

	/**
	 * Releases all resources associated with the FrameBuffer.
	 */
	override fun dispose() {
		val gl = Gdx.gl20

//        Gdx.gl.glDeleteTexture (textureHandle);
		if (hasDepthStencilPackedBuffer) {
			gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle)
		} else {
			if (bufferBuilder.hasDepthRenderBuffer) gl.glDeleteRenderbuffer(depthbufferHandle)
			if (bufferBuilder.hasStencilRenderBuffer) gl.glDeleteRenderbuffer(stencilbufferHandle)
		}
		gl.glDeleteFramebuffer(framebufferHandle)
		if (buffers[Gdx.app] != null) buffers[Gdx.app]!!.removeValue(this, true)
		if (nonMultisampledFbo != null) {
			nonMultisampledFbo!!.dispose()
		}
	}

	/**
	 * Makes the frame buffer current so everything gets drawn to it.
	 */
	fun bind() {
		Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle)
	}

	/**
	 * Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it.
	 */
	fun begin() {
		bind()
		setFrameBufferViewport()
	}

	/**
	 * Sets viewport to the dimensions of framebuffer. Called by [.begin].
	 */
	fun setFrameBufferViewport() {
		Gdx.gl20.glViewport(0, 0, bufferBuilder.width, bufferBuilder.height)
	}
	/**
	 * Unbinds the framebuffer and sets viewport sizes, all drawing will be performed to the normal framebuffer from here on.
	 *
	 * @param x      the x-axis position of the viewport in pixels
	 * @param y      the y-asis position of the viewport in pixels
	 * @param width  the width of the viewport in pixels
	 * @param height the height of the viewport in pixels
	 */
	/**
	 * Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.
	 */
	@JvmOverloads
	fun end(x: Int = 0, y: Int = 0, width: Int = Gdx.graphics.backBufferWidth, height: Int = Gdx.graphics.backBufferHeight) {
		unbind()
		Gdx.gl20.glViewport(x, y, width, height)
	}

	/**
	 * @return The OpenGL handle of the (optional) depth buffer (see [GL20.glGenRenderbuffer]). May return 0 even if depth buffer enabled
	 */
	fun getDepthBufferHandle(): Int {
		return depthbufferHandle
	}

	/**
	 * @return The OpenGL handle of the (optional) stencil buffer (see [GL20.glGenRenderbuffer]). May return 0 even if stencil buffer enabled
	 */
	fun getStencilBufferHandle(): Int {
		return stencilbufferHandle
	}

	/**
	 * @return The OpenGL handle of the packed depth & stencil buffer (GL_DEPTH24_STENCIL8_OES) or 0 if not used.
	 */
	fun getDepthStencilPackedBuffer(): Int {
		return depthStencilPackedBufferHandle
	}

	/**
	 * @return the height of the framebuffer in pixels
	 */
	fun getHeight(): Int {
		return bufferBuilder.height
	}

	/**
	 * @return the width of the framebuffer in pixels
	 */
	fun getWidth(): Int {
		return bufferBuilder.width
	}

	private fun checkValidBuilder() {
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

	class FrameBufferTextureAttachmentSpec(var internalFormat: Int, var format: Int, var type: Int) {
		var isFloat = false
		var isGpuOnly = false
		var isDepth = false
		var isStencil = false
		val isColorTexture: Boolean
			get() = !isDepth && !isStencil
	}

	class FrameBufferRenderBufferAttachmentSpec(var internalFormat: Int)

	abstract class GLFrameBufferBuilder<U : MultisampleFBO?>(var width: Int, var height: Int) {
		var textureAttachmentSpecs = Array<FrameBufferTextureAttachmentSpec>()
		var stencilRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec? = null
		var depthRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec? = null
		var packedStencilDepthRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec? = null
		var hasStencilRenderBuffer = false
		var hasDepthRenderBuffer = false
		var hasPackedStencilDepthRenderBuffer = false
		fun addColorTextureAttachment(internalFormat: Int, format: Int, type: Int): GLFrameBufferBuilder<U> {
			textureAttachmentSpecs.add(FrameBufferTextureAttachmentSpec(internalFormat, format, type))
			return this
		}

		fun addBasicColorTextureAttachment(format: Pixmap.Format?): GLFrameBufferBuilder<U> {
			val glFormat = Pixmap.Format.toGlFormat(format)
			val glType = Pixmap.Format.toGlType(format)
			return addColorTextureAttachment(glFormat, glFormat, glType)
		}

		fun addFloatAttachment(internalFormat: Int, format: Int, type: Int, gpuOnly: Boolean): GLFrameBufferBuilder<U> {
			val spec = FrameBufferTextureAttachmentSpec(internalFormat, format, type)
			spec.isFloat = true
			spec.isGpuOnly = gpuOnly
			textureAttachmentSpecs.add(spec)
			return this
		}

		fun addStencilTextureAttachment(internalFormat: Int, type: Int): GLFrameBufferBuilder<U> {
			val spec = FrameBufferTextureAttachmentSpec(internalFormat, GL30.GL_STENCIL_ATTACHMENT, type)
			spec.isStencil = true
			textureAttachmentSpecs.add(spec)
			return this
		}

		fun addDepthTextureAttachment(internalFormat: Int, type: Int): GLFrameBufferBuilder<U> {
			val spec = FrameBufferTextureAttachmentSpec(internalFormat, GL30.GL_DEPTH_COMPONENT, type)
			spec.isDepth = true
			textureAttachmentSpecs.add(spec)
			return this
		}

		fun addDepthRenderBuffer(internalFormat: Int): GLFrameBufferBuilder<U> {
			depthRenderBufferSpec = FrameBufferRenderBufferAttachmentSpec(internalFormat)
			hasDepthRenderBuffer = true
			return this
		}

		fun addStencilRenderBuffer(internalFormat: Int): GLFrameBufferBuilder<U> {
			stencilRenderBufferSpec = FrameBufferRenderBufferAttachmentSpec(internalFormat)
			hasStencilRenderBuffer = true
			return this
		}

		fun addStencilDepthPackedRenderBuffer(internalFormat: Int): GLFrameBufferBuilder<U> {
			packedStencilDepthRenderBufferSpec = FrameBufferRenderBufferAttachmentSpec(internalFormat)
			hasPackedStencilDepthRenderBuffer = true
			return this
		}

		fun addBasicDepthRenderBuffer(): GLFrameBufferBuilder<U> {
			return addDepthRenderBuffer(GL20.GL_DEPTH_COMPONENT16)
		}

		fun addBasicStencilRenderBuffer(): GLFrameBufferBuilder<U> {
			return addStencilRenderBuffer(GL20.GL_STENCIL_INDEX8)
		}

		fun addBasicStencilDepthPackedRenderBuffer(): GLFrameBufferBuilder<U> {
			return addStencilDepthPackedRenderBuffer(GL30.GL_DEPTH24_STENCIL8)
		}

		abstract fun build(): U
	}

	class FrameBufferBuilder(width: Int, height: Int) : GLFrameBufferBuilder<MultisampleFBO>(width, height) {
		override fun build(): MultisampleFBO {
			return MultisampleFBO(this)
		}
	}

	companion object {
		/**
		 * the frame buffers
		 */
		val buffers: MutableMap<Application, Array<MultisampleFBO>?> = HashMap()
		const val GL_DEPTH24_STENCIL8_OES = 0x88F0

		/**
		 * the default framebuffer handle, a.k.a screen.
		 */
		var defaultFramebufferHandle = 0

		/**
		 * true if we have polled for the default handle already.
		 */
		var defaultFramebufferHandleInitialized = false

		/**
		 * Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.
		 */
		fun unbind() {
			Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle)
		}

		private fun addManagedFrameBuffer(app: Application, frameBuffer: MultisampleFBO) {
			var managedResources = buffers[app]
			if (managedResources == null) managedResources = Array()
			managedResources.add(frameBuffer)
			buffers[app] = managedResources
		}

		/**
		 * Invalidates all frame buffers. This can be used when the OpenGL context is lost to rebuild all managed frame buffers. This
		 * assumes that the texture attached to this buffer has already been rebuild! Use with care.
		 */
		fun invalidateAllFrameBuffers(app: Application) {
			if (Gdx.gl20 == null) return
			val bufferArray = buffers[app] ?: return
			for (i in 0 until bufferArray.size) {
				bufferArray[i].build()
			}
		}

		fun clearAllFrameBuffers(app: Application) {
			buffers.remove(app)
		}

		fun getManagedStatus(builder: StringBuilder): StringBuilder {
			builder.append("Managed buffers/app: { ")
			for (app in buffers.keys) {
				builder.append(buffers[app]!!.size)
				builder.append(" ")
			}
			builder.append("}")
			return builder
		}

		val managedStatus: String
			get() = getManagedStatus(StringBuilder()).toString()
	}

	/**
	 * Creates a MultisampleFBO from the specifications provided by bufferBuilder
	 */
	init {
		build()
	}
}