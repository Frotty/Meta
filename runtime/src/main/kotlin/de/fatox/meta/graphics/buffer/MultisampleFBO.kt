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
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.Disposable
import de.fatox.meta.Meta

/**
 *
 *
 * Encapsulates OpenGL ES 2.0 frame buffer objects. This is a simple helper class which should cover most FBO uses. It
 * will automatically create a gltexture for the color attachment and a renderbuffer for the depth buffer. You can get a
 * hold of the gltexture by [MultisampleFBO.colorBufferTexture]. This class will only work with OpenGL ES 2.0.
 *
 * FrameBuffers are managed. In case of an OpenGL context loss, which only happens on Android when a user switches to
 * another application or receives an incoming call, the framebuffer will be automatically recreated.
 *
 * A FrameBuffer must be disposed if it is no longer needed
 *
 *
 * @author mzechner, realitix
 */
class MultisampleFBO(var bufferBuilder: GLFrameBufferBuilder<out MultisampleFBO>) : MetaFrameBuffer, Disposable {
	/**
	 * the color buffer texture
	 */
	var textureAttachments: Int = 0
	/**
	 * @return The OpenGL handle of the framebuffer (see [GL20.glGenFramebuffer])
	 */
	var framebufferHandle: Int = 0

	/**
	 * The OpenGL handle of the (optional) depth buffer (see [GL20.glGenRenderbuffer]). May return 0 even if depth buffer enabled
	 */
	var depthbufferHandle: Int = 0

	/**
	 * The OpenGL handle of the (optional) stencil buffer (see [GL20.glGenRenderbuffer]). May return 0 even if stencil buffer enabled
	 */
	var stencilbufferHandle: Int = 0

	/**
	 * The OpenGL handle of the packed depth & stencil buffer (GL_DEPTH24_STENCIL8_OES) or 0 if not used.
	 */
	var depthStencilPackedBufferHandle: Int = 0

	/**
	 * if has depth stencil packed buffer
	 */
	var hasDepthStencilPackedBuffer: Boolean = false

	/**
	 * if multiple texture attachments are present
	 */
	var isMRT: Boolean = false
	var nonMultisampledFbo: FrameBuffer? = null
	val colorBufferTexture: Texture?
		get() {
			val nonMultisampledFbo = nonMultisampledFbo
			if (nonMultisampledFbo == null) {
				// TODO fix
				return null
			}
			Gdx.gl30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferHandle)
			Gdx.gl30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, nonMultisampledFbo.framebufferHandle)
			Gdx.gl30.glBlitFramebuffer(
				0, 0, width, height, 0, 0, width, height, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST
			)
			Gdx.gl30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0)
			Gdx.gl30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0)
			FrameBuffer.unbind()
			return nonMultisampledFbo.colorBufferTexture
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
				Gdx.gl30.glBlitFramebuffer(
					0, 0, width, height, 0, 0, width, height, GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST
				)
			} else {
				Gdx.gl30.glBlitFramebuffer(
					0, 0, width, height, 0, 0, width, height, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST
				)
			}
		}
		FrameBuffer.unbind()
		return nonMultisampledFbo!!.textureAttachments
	}

	override fun getFBO(): Int {
		return framebufferHandle
	}

	private fun checkError(error: Int) {
		when (error) {
			GL20.GL_INVALID_ENUM -> System.err.println("Invalid enum")
			GL20.GL_INVALID_VALUE -> System.err.println("Invalid val")
			GL20.GL_INVALID_OPERATION -> System.err.println("Invalid op")
			GL20.GL_INVALID_FRAMEBUFFER_OPERATION -> System.err.println("Invalid fbo op")
			GL20.GL_OUT_OF_MEMORY -> System.err.println("Out of memory")
		}
	}

	/**
	 * Override this method in a derived class to set up the backing texture as you like.
	 */
	fun createTexture(attachmentSpec: FrameBufferTextureAttachmentSpec): Int {
		return Meta.instance.graphicsHandler.createTexture(this, attachmentSpec)
	}

	/**
	 * Override this method in a derived class to dispose the backing texture as you like.
	 */
	fun disposeColorTexture(colorTexture: Texture) {
		colorTexture.dispose()
	}

	fun build() {
		Meta.instance.graphicsHandler.build(this)
	}

	/**
	 * Releases all resources associated with the FrameBuffer.
	 */
	override fun dispose() {

		//        Gdx.gl.glDeleteTexture (textureHandle);
		if (hasDepthStencilPackedBuffer) {
			Gdx.gl20.glDeleteRenderbuffer(depthStencilPackedBufferHandle)
		} else {
			if (bufferBuilder.hasDepthRenderBuffer) Gdx.gl20.glDeleteRenderbuffer(depthbufferHandle)
			if (bufferBuilder.hasStencilRenderBuffer) Gdx.gl20.glDeleteRenderbuffer(stencilbufferHandle)
		}
		Gdx.gl20.glDeleteFramebuffer(framebufferHandle)
		buffers[Gdx.app]?.removeValue(this, true)
		nonMultisampledFbo?.dispose()
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
	override fun begin() {
		bind()
		setFrameBufferViewport()
	}

	/**
	 * Sets viewport to the dimensions of framebuffer. Called by [.begin].
	 */
	fun setFrameBufferViewport() {
		Gdx.gl20.glViewport(0, 0, bufferBuilder.width, bufferBuilder.height)
	}

	override fun end() {
		end(0)
	}

	/**
	 * Unbinds the framebuffer and sets viewport sizes, all drawing will be performed to the normal framebuffer from here on.
	 *
	 * @param x      the x-axis position of the viewport in pixels
	 * @param y      the y-asis position of the viewport in pixels
	 * @param width  the width of the viewport in pixels
	 * @param height the height of the viewport in pixels
	 */
	fun end(
		x: Int = 0,
		y: Int = 0,
		width: Int = Gdx.graphics.backBufferWidth,
		height: Int = Gdx.graphics.backBufferHeight
	) {
		unbind()
		Gdx.gl20.glViewport(x, y, width, height)
	}

	/**
	 * @return the height of the framebuffer in pixels
	 */
	override val height: Int
		get() = bufferBuilder.height

	/**
	 * @return the width of the framebuffer in pixels
	 */
	override val width: Int
		get() = bufferBuilder.width

	class FrameBufferTextureAttachmentSpec(var internalFormat: Int, var format: Int, var type: Int) {
		var isFloat: Boolean = false
		var isGpuOnly: Boolean = false
		var isDepth: Boolean = false
		var isStencil: Boolean = false
		val isColorTexture: Boolean
			get() = !isDepth && !isStencil
	}

	class FrameBufferRenderBufferAttachmentSpec(var internalFormat: Int)

	abstract class GLFrameBufferBuilder<U : MultisampleFBO?>(var width: Int, var height: Int) {
		var textureAttachmentSpecs: Array<FrameBufferTextureAttachmentSpec> = Array<FrameBufferTextureAttachmentSpec>()
		var stencilRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec? = null
		var depthRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec? = null
		var packedStencilDepthRenderBufferSpec: FrameBufferRenderBufferAttachmentSpec? = null
		var hasStencilRenderBuffer: Boolean = false
		var hasDepthRenderBuffer: Boolean = false
		var hasPackedStencilDepthRenderBuffer: Boolean = false
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
		const val GL_DEPTH24_STENCIL8_OES: Int = 0x88F0

		/**
		 * the default framebuffer handle, a.k.a screen.
		 */
		var defaultFramebufferHandle: Int = 0

		/**
		 * true if we have polled for the default handle already.
		 */
		var defaultFramebufferHandleInitialized: Boolean = false

		/**
		 * Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.
		 */
		fun unbind() {
			Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle)
		}

		fun addManagedFrameBuffer(app: Application, frameBuffer: MultisampleFBO) {
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