/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.fatox.meta.graphics.buffer;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Encapsulates OpenGL ES 2.0 frame buffer objects. This is a simple helper class which should cover most FBO uses. It will
 * automatically create a gltexture for the color attachment and a renderbuffer for the depth buffer. You can get a hold of the
 * gltexture by {@link MultisampleFBO#getColorBufferTexture()}. This class will only work with OpenGL ES 2.0.
 * </p>
 * <p>
 * <p>
 * FrameBuffers are managed. In case of an OpenGL context loss, which only happens on Android when a user switches to another
 * application or receives an incoming call, the framebuffer will be automatically recreated.
 * </p>
 * <p>
 * <p>
 * A FrameBuffer must be disposed if it is no longer needed
 * </p>
 *
 * @author mzechner, realitix
 */
public class MultisampleFBO implements Disposable {
	/**
	 * the frame buffers
	 **/
	protected final static Map<Application, Array<MultisampleFBO>> buffers = new HashMap<Application, Array<MultisampleFBO>>();

	protected final static int GL_DEPTH24_STENCIL8_OES = 0x88F0;

	/**
	 * the color buffer texture
	 **/
	private int textureAttachments = 0;

	/**
	 * the default framebuffer handle, a.k.a screen.
	 */
	protected static int defaultFramebufferHandle;
	/**
	 * true if we have polled for the default handle already.
	 */
	protected static boolean defaultFramebufferHandleInitialized = false;

	/**
	 * the framebuffer handle
	 **/
	protected int framebufferHandle;
	/**
	 * the depthbuffer render object handle
	 **/
	protected int depthbufferHandle;
	/**
	 * the stencilbuffer render object handle
	 **/
	protected int stencilbufferHandle;
	/**
	 * the depth stencil packed render buffer object handle
	 **/
	protected int depthStencilPackedBufferHandle;
	/**
	 * if has depth stencil packed buffer
	 **/
	protected boolean hasDepthStencilPackedBuffer;

	/**
	 * if multiple texture attachments are present
	 **/
	protected boolean isMRT;

	protected FrameBuffer nonMultisampledFbo;

	protected GLFrameBufferBuilder<? extends MultisampleFBO> bufferBuilder;

	/**
	 * Creates a MultisampleFBO from the specifications provided by bufferBuilder
	 **/
	protected MultisampleFBO(GLFrameBufferBuilder<? extends MultisampleFBO> bufferBuilder) {
		this.bufferBuilder = bufferBuilder;
		build();

	}

	/**
	 * Convenience method to return the first Texture attachment present in the fbo
	 **/
	public Texture getColorBufferTexture() {
		if (nonMultisampledFbo == null) {
			// TODO fix
			return null;
		}
		Gdx.gl30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferHandle);
		Gdx.gl30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, nonMultisampledFbo.getFramebufferHandle());
		Gdx.gl30.glBlitFramebuffer(0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
		Gdx.gl30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
		Gdx.gl30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
		FrameBuffer.unbind();
		return nonMultisampledFbo.getColorBufferTexture();
	}

	/**
	 * Return the Texture attachments attached to the fbo
	 **/
	public Array<Texture> getTextureAttachments() {
		checkError(Gdx.gl.glGetError());
		FrameBuffer.unbind();
		Gdx.gl30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebufferHandle);
		Gdx.gl30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, nonMultisampledFbo.getFramebufferHandle());
		IntBuffer intBuffer = BufferUtils.newIntBuffer(1);
		for (int i = 0; i < textureAttachments; i++) {
			if (i == textureAttachments - 1 && textureAttachments > 1) {
				Gdx.gl30.glReadBuffer(GL30.GL_NONE);
				intBuffer.put(GL30.GL_NONE);
				checkError(Gdx.gl.glGetError());
			} else {
				Gdx.gl30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0 + i);
				intBuffer.put(GL30.GL_COLOR_ATTACHMENT0 + i);
			}
			intBuffer.rewind();
			Gdx.gl30.glDrawBuffers(1, intBuffer);
			if (i == textureAttachments - 1 && textureAttachments > 1) {
				Gdx.gl30.glBlitFramebuffer(0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
			} else {
				Gdx.gl30.glBlitFramebuffer(0, 0, getWidth(), getHeight(), 0, 0, getWidth(), getHeight(), GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
			}
		}
		FrameBuffer.unbind();
		return nonMultisampledFbo.getTextureAttachments();
	}

	private void checkError(int error) {
		if (error == GL20.GL_INVALID_ENUM) {
			System.err.println("Invalid enum");
		} else if (error == GL20.GL_INVALID_VALUE) {
			System.err.println("Invalid val");
		} else if (error == GL20.GL_INVALID_OPERATION) {
			System.err.println("Invalid op");
		} else if (error == GL20.GL_INVALID_FRAMEBUFFER_OPERATION) {
			System.err.println("Invalid fbo op");
		} else if (error == GL20.GL_OUT_OF_MEMORY) {
			System.err.println("Out of memory");
		}
	}

	/**
	 * Override this method in a derived class to set up the backing texture as you like.
	 */
	protected int createTexture(FrameBufferTextureAttachmentSpec attachmentSpec) {
		try {
			int texture = Gdx.gl.glGenTexture();
			Gdx.gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, texture);

			GL32.glTexImage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, 2, attachmentSpec.internalFormat, getWidth(), getHeight(), true);
			Gdx.gl.glTexParameterf(texture, GL20.GL_TEXTURE_MIN_FILTER, Texture.TextureFilter.Nearest.getGLEnum());
			Gdx.gl.glTexParameterf(texture, GL20.GL_TEXTURE_MAG_FILTER, Texture.TextureFilter.Nearest.getGLEnum());
			Gdx.gl.glTexParameterf(texture, GL20.GL_TEXTURE_WRAP_S, Texture.TextureWrap.ClampToEdge.getGLEnum());
			Gdx.gl.glTexParameterf(texture, GL20.GL_TEXTURE_WRAP_T, Texture.TextureWrap.ClampToEdge.getGLEnum());
			Gdx.gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, 0);
			return texture;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Override this method in a derived class to dispose the backing texture as you like.
	 */
	protected void disposeColorTexture(Texture colorTexture) {
		colorTexture.dispose();
	}

	protected void build() {
		GL20 gl = Gdx.gl20;

		checkValidBuilder();

		// iOS uses a different framebuffer handle! (not necessarily 0)
		if (!defaultFramebufferHandleInitialized) {
			defaultFramebufferHandleInitialized = true;
			if (Gdx.app.getType() == ApplicationType.iOS) {
				IntBuffer intbuf = ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder()).asIntBuffer();
				gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, intbuf);
				defaultFramebufferHandle = intbuf.get(0);
			} else {
				defaultFramebufferHandle = 0;
			}
		}

		framebufferHandle = gl.glGenFramebuffer();
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
		GLFrameBuffer.FrameBufferBuilder imBuilder = new GLFrameBuffer.FrameBufferBuilder(getWidth(), getHeight());

		int width = bufferBuilder.width;
		int height = bufferBuilder.height;

		if (bufferBuilder.hasDepthRenderBuffer) {
			depthbufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthbufferHandle);
			Gdx.gl30.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, 4, bufferBuilder.depthRenderBufferSpec.internalFormat, width, height);
		}

		if (bufferBuilder.hasStencilRenderBuffer) {
			stencilbufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, stencilbufferHandle);
			Gdx.gl30.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, 4, bufferBuilder.stencilRenderBufferSpec.internalFormat, width, height);
		}

		if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
			depthStencilPackedBufferHandle = gl.glGenRenderbuffer();
			gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle);
			Gdx.gl30.glRenderbufferStorageMultisample(GL20.GL_RENDERBUFFER, 4, bufferBuilder.packedStencilDepthRenderBufferSpec.internalFormat, width, height);
		}

		isMRT = bufferBuilder.textureAttachmentSpecs.size > 1;
		isMRT = true;
		int colorTextureCounter = 0;
		if (isMRT) {
			for (FrameBufferTextureAttachmentSpec attachmentSpec : bufferBuilder.textureAttachmentSpecs) {
				int texture = createTexture(attachmentSpec);
				textureAttachments++;
				if (attachmentSpec.isColorTexture()) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + colorTextureCounter, GL32.GL_TEXTURE_2D_MULTISAMPLE,
						texture, 0);
					imBuilder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE);
					colorTextureCounter++;
				} else if (attachmentSpec.isDepth) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL30.GL_TEXTURE_2D, texture, 0);
					imBuilder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL30.GL_FLOAT);
				} else if (attachmentSpec.isStencil) {
					gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, gl.GL_TEXTURE_2D, texture, 0);
				}
			}
		} else {
			int texture = createTexture(bufferBuilder.textureAttachmentSpecs.first());
			textureAttachments = 1;
			imBuilder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE);
			gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, texture);
			Gdx.gl30.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D_MULTISAMPLE, texture, 0);
		}

		if (isMRT) {
			IntBuffer buffer = BufferUtils.newIntBuffer(colorTextureCounter);
			for (int i = 0; i < colorTextureCounter; i++) {
				buffer.put(GL30.GL_COLOR_ATTACHMENT0 + i);
			}
			buffer.position(0);
			Gdx.gl30.glDrawBuffers(colorTextureCounter, buffer);
		}

		if (bufferBuilder.hasDepthRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_RENDERBUFFER, depthbufferHandle);
		}

		if (bufferBuilder.hasStencilRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL20.GL_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, stencilbufferHandle);
		}

		if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
			gl.glFramebufferRenderbuffer(GL20.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL20.GL_RENDERBUFFER, depthStencilPackedBufferHandle);
		}

		gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);
		gl.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, 0);


		assertShit();
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);

		addManagedFrameBuffer(Gdx.app, this);
		nonMultisampledFbo = imBuilder.build();
	}

	private void assertShit() {
		GL20 gl = Gdx.gl;
		int result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {

			if (hasDepthStencilPackedBuffer) {
				gl.glDeleteBuffer(depthStencilPackedBufferHandle);
			} else {
				if (bufferBuilder.hasDepthRenderBuffer) gl.glDeleteRenderbuffer(depthbufferHandle);
				if (bufferBuilder.hasStencilRenderBuffer) gl.glDeleteRenderbuffer(stencilbufferHandle);
			}

			gl.glDeleteFramebuffer(framebufferHandle);

			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
				throw new IllegalStateException("frame buffer couldn't be constructed: incomplete attachment");
			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS)
				throw new IllegalStateException("frame buffer couldn't be constructed: incomplete dimensions");
			if (result == GL20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)
				throw new IllegalStateException("frame buffer couldn't be constructed: missing attachment");
			if (result == GL20.GL_FRAMEBUFFER_UNSUPPORTED)
				throw new IllegalStateException("frame buffer couldn't be constructed: unsupported combination of formats");
			throw new IllegalStateException("frame buffer couldn't be constructed: unknown error " + result);
		}
	}

	/**
	 * Releases all resources associated with the FrameBuffer.
	 */
	@Override
	public void dispose() {
		GL20 gl = Gdx.gl20;

//        Gdx.gl.glDeleteTexture (textureHandle);

		if (hasDepthStencilPackedBuffer) {
			gl.glDeleteRenderbuffer(depthStencilPackedBufferHandle);
		} else {
			if (bufferBuilder.hasDepthRenderBuffer) gl.glDeleteRenderbuffer(depthbufferHandle);
			if (bufferBuilder.hasStencilRenderBuffer) gl.glDeleteRenderbuffer(stencilbufferHandle);
		}

		gl.glDeleteFramebuffer(framebufferHandle);

		if (buffers.get(Gdx.app) != null) buffers.get(Gdx.app).removeValue(this, true);
		if (nonMultisampledFbo != null) {
			nonMultisampledFbo.dispose();
		}
	}

	/**
	 * Makes the frame buffer current so everything gets drawn to it.
	 */
	public void bind() {
		Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
	}

	/**
	 * Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.
	 */
	public static void unbind() {
		Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);
	}

	/**
	 * Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it.
	 */
	public void begin() {
		bind();
		setFrameBufferViewport();
	}

	/**
	 * Sets viewport to the dimensions of framebuffer. Called by {@link #begin()}.
	 */
	protected void setFrameBufferViewport() {
		Gdx.gl20.glViewport(0, 0, bufferBuilder.width, bufferBuilder.height);
	}

	/**
	 * Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on.
	 */
	public void end() {
		end(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
	}

	/**
	 * Unbinds the framebuffer and sets viewport sizes, all drawing will be performed to the normal framebuffer from here on.
	 *
	 * @param x      the x-axis position of the viewport in pixels
	 * @param y      the y-asis position of the viewport in pixels
	 * @param width  the width of the viewport in pixels
	 * @param height the height of the viewport in pixels
	 */
	public void end(int x, int y, int width, int height) {
		unbind();
		Gdx.gl20.glViewport(x, y, width, height);
	}


	/**
	 * @return The OpenGL handle of the framebuffer (see {@link GL20#glGenFramebuffer()})
	 */
	public int getFramebufferHandle() {
		return framebufferHandle;
	}

	/**
	 * @return The OpenGL handle of the (optional) depth buffer (see {@link GL20#glGenRenderbuffer()}). May return 0 even if depth buffer enabled
	 */
	public int getDepthBufferHandle() {
		return depthbufferHandle;
	}

	/**
	 * @return The OpenGL handle of the (optional) stencil buffer (see {@link GL20#glGenRenderbuffer()}). May return 0 even if stencil buffer enabled
	 */
	public int getStencilBufferHandle() {
		return stencilbufferHandle;
	}

	/**
	 * @return The OpenGL handle of the packed depth & stencil buffer (GL_DEPTH24_STENCIL8_OES) or 0 if not used.
	 **/
	protected int getDepthStencilPackedBuffer() {
		return depthStencilPackedBufferHandle;
	}

	/**
	 * @return the height of the framebuffer in pixels
	 */
	public int getHeight() {
		return bufferBuilder.height;
	}

	/**
	 * @return the width of the framebuffer in pixels
	 */
	public int getWidth() {
		return bufferBuilder.width;
	}

	private static void addManagedFrameBuffer(Application app, MultisampleFBO frameBuffer) {
		Array<MultisampleFBO> managedResources = buffers.get(app);
		if (managedResources == null) managedResources = new Array<MultisampleFBO>();
		managedResources.add(frameBuffer);
		buffers.put(app, managedResources);
	}

	/**
	 * Invalidates all frame buffers. This can be used when the OpenGL context is lost to rebuild all managed frame buffers. This
	 * assumes that the texture attached to this buffer has already been rebuild! Use with care.
	 */
	public static void invalidateAllFrameBuffers(Application app) {
		if (Gdx.gl20 == null) return;

		Array<MultisampleFBO> bufferArray = buffers.get(app);
		if (bufferArray == null) return;
		for (int i = 0; i < bufferArray.size; i++) {
			bufferArray.get(i).build();
		}
	}

	public static void clearAllFrameBuffers(Application app) {
		buffers.remove(app);
	}

	public static StringBuilder getManagedStatus(final StringBuilder builder) {
		builder.append("Managed buffers/app: { ");
		for (Application app : buffers.keySet()) {
			builder.append(buffers.get(app).size);
			builder.append(" ");
		}
		builder.append("}");
		return builder;
	}

	public static String getManagedStatus() {
		return getManagedStatus(new StringBuilder()).toString();
	}


	private void checkValidBuilder() {
		boolean runningGL30 = Gdx.graphics.isGL30Available();

		if (!runningGL30) {
			if (bufferBuilder.hasPackedStencilDepthRenderBuffer) {
				throw new GdxRuntimeException("Packed Stencil/Render render buffers are not available on GLES 2.0");
			}
			if (bufferBuilder.textureAttachmentSpecs.size > 1) {
				throw new GdxRuntimeException("Multiple render targets not available on GLES 2.0");
			}
			for (FrameBufferTextureAttachmentSpec spec : bufferBuilder.textureAttachmentSpecs) {
				if (spec.isDepth)
					throw new GdxRuntimeException("Depth texture FrameBuffer Attachment not available on GLES 2.0");
				if (spec.isStencil)
					throw new GdxRuntimeException("Stencil texture FrameBuffer Attachment not available on GLES 2.0");
				if (spec.isFloat) {
					if (!Gdx.graphics.supportsExtension("OES_texture_float")) {
						throw new GdxRuntimeException("Float texture FrameBuffer Attachment not available on GLES 2.0");
					}
				}
			}
		}

	}

	protected static class FrameBufferTextureAttachmentSpec {
		int internalFormat, format, type;
		boolean isFloat, isGpuOnly;
		boolean isDepth;
		boolean isStencil;

		public FrameBufferTextureAttachmentSpec(int internalformat, int format, int type) {
			this.internalFormat = internalformat;
			this.format = format;
			this.type = type;
		}

		public boolean isColorTexture() {
			return !isDepth && !isStencil;
		}
	}

	protected static class FrameBufferRenderBufferAttachmentSpec {
		int internalFormat;

		public FrameBufferRenderBufferAttachmentSpec(int internalFormat) {
			this.internalFormat = internalFormat;
		}
	}

	public static abstract class GLFrameBufferBuilder<U extends MultisampleFBO> {

		protected int width, height;

		protected Array<FrameBufferTextureAttachmentSpec> textureAttachmentSpecs = new Array<FrameBufferTextureAttachmentSpec>();

		protected FrameBufferRenderBufferAttachmentSpec stencilRenderBufferSpec;
		protected FrameBufferRenderBufferAttachmentSpec depthRenderBufferSpec;
		protected FrameBufferRenderBufferAttachmentSpec packedStencilDepthRenderBufferSpec;

		protected boolean hasStencilRenderBuffer;
		protected boolean hasDepthRenderBuffer;
		protected boolean hasPackedStencilDepthRenderBuffer;

		public GLFrameBufferBuilder(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public GLFrameBufferBuilder<U> addColorTextureAttachment(int internalFormat, int format, int type) {
			textureAttachmentSpecs.add(new FrameBufferTextureAttachmentSpec(internalFormat, format, type));
			return this;
		}

		public GLFrameBufferBuilder<U> addBasicColorTextureAttachment(Pixmap.Format format) {
			int glFormat = Pixmap.Format.toGlFormat(format);
			int glType = Pixmap.Format.toGlType(format);
			return addColorTextureAttachment(glFormat, glFormat, glType);
		}

		public GLFrameBufferBuilder<U> addFloatAttachment(int internalFormat, int format, int type, boolean gpuOnly) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat, format, type);
			spec.isFloat = true;
			spec.isGpuOnly = gpuOnly;
			textureAttachmentSpecs.add(spec);
			return this;
		}

		public GLFrameBufferBuilder<U> addStencilTextureAttachment(int internalFormat, int type) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat, GL30.GL_STENCIL_ATTACHMENT, type);
			spec.isStencil = true;
			textureAttachmentSpecs.add(spec);
			return this;
		}

		public GLFrameBufferBuilder<U> addDepthTextureAttachment(int internalFormat, int type) {
			FrameBufferTextureAttachmentSpec spec = new FrameBufferTextureAttachmentSpec(internalFormat, GL30.GL_DEPTH_COMPONENT, type);
			spec.isDepth = true;
			textureAttachmentSpecs.add(spec);
			return this;
		}

		public GLFrameBufferBuilder<U> addDepthRenderBuffer(int internalFormat) {
			depthRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat);
			hasDepthRenderBuffer = true;
			return this;
		}

		public GLFrameBufferBuilder<U> addStencilRenderBuffer(int internalFormat) {
			stencilRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat);
			hasStencilRenderBuffer = true;
			return this;
		}

		public GLFrameBufferBuilder<U> addStencilDepthPackedRenderBuffer(int internalFormat) {
			packedStencilDepthRenderBufferSpec = new FrameBufferRenderBufferAttachmentSpec(internalFormat);
			hasPackedStencilDepthRenderBuffer = true;
			return this;
		}

		public GLFrameBufferBuilder<U> addBasicDepthRenderBuffer() {
			return addDepthRenderBuffer(GL20.GL_DEPTH_COMPONENT16);
		}

		public GLFrameBufferBuilder<U> addBasicStencilRenderBuffer() {
			return addStencilRenderBuffer(GL20.GL_STENCIL_INDEX8);
		}

		public GLFrameBufferBuilder<U> addBasicStencilDepthPackedRenderBuffer() {
			return addStencilDepthPackedRenderBuffer(GL30.GL_DEPTH24_STENCIL8);
		}

		public abstract U build();
	}

	public static class FrameBufferBuilder extends MultisampleFBO.GLFrameBufferBuilder<MultisampleFBO> {

		public FrameBufferBuilder(int width, int height) {
			super(width, height);
		}

		@Override
		public MultisampleFBO build() {
			return new MultisampleFBO(this);
		}

	}

}
