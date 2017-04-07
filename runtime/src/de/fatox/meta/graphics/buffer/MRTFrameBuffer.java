package de.fatox.meta.graphics.buffer;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class MRTFrameBuffer implements Disposable {
    /** the frame buffers **/
    private final static Map<Application, Array<MRTFrameBuffer>> buffers = new HashMap<Application, Array<MRTFrameBuffer>>();

    /** the color buffer texture **/
    private Array<Texture> colorTextures;

    /** the default framebuffer handle, a.k.a screen. */
    private static int defaultFramebufferHandle;
    /** true if we have polled for the default handle already. */
    private static boolean defaultFramebufferHandleInitialized = false;

    /** the framebuffer handle **/
    private int framebufferHandle;

    /** width **/
    private final int width;

    /** height **/
    private final int height;

    public MRTFrameBuffer(int width, int height, int numColorAttachments) {
        this.width = width;
        this.height = height;
        build();

        addManagedFrameBuffer(Gdx.app, this);
    }

    private Texture createColorTexture(Texture.TextureFilter min, Texture.TextureFilter mag, int internalformat, int format,
                                       int type) {
        GLOnlyTextureData data = new GLOnlyTextureData(width, height, 0, internalformat, format, type);
        Texture result = new Texture(data);
        result.setFilter(min, mag);
        result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return result;
    }

    private Texture createDepthTexture() {
        GLOnlyTextureData data = new GLOnlyTextureData(width, height, 0, GL30.GL_DEPTH_COMPONENT32F, GL30.GL_DEPTH_COMPONENT,
                GL30.GL_FLOAT);
        Texture result = new Texture(data);
        result.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return result;
    }

    private void disposeColorTexture(Texture colorTexture) {
        colorTexture.dispose();
    }

    private void build() {
        GL20 gl = Gdx.gl20;

        // iOS uses a different framebuffer handle! (not necessarily 0)
        if (!defaultFramebufferHandleInitialized) {
            defaultFramebufferHandleInitialized = true;
            if (Gdx.app.getType() == Application.ApplicationType.iOS) {
                IntBuffer intbuf = ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder())
                        .asIntBuffer();
                gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, intbuf);
                defaultFramebufferHandle = intbuf.get(0);
            } else {
                defaultFramebufferHandle = 0;
            }
        }

        colorTextures = new Array<>();

        framebufferHandle = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);

        //rgba
        Texture diffuse = createColorTexture(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, GL30.GL_RGBA8,
                GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE);
        //rgb
        Texture normal = createColorTexture(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, GL30.GL_RGB8,
                GL30.GL_RGB, GL30.GL_UNSIGNED_BYTE);
        //rgb
        Texture material = createColorTexture(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest, GL30.GL_RGB8,
                GL30.GL_RGB, GL30.GL_UNSIGNED_BYTE);
        Texture depth = createDepthTexture();

        colorTextures.add(diffuse);
        colorTextures.add(normal);
        colorTextures.add(material);
        colorTextures.add(depth);

        gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D,
                diffuse.getTextureObjectHandle(), 0);
        gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_TEXTURE_2D,
                normal.getTextureObjectHandle(), 0);
        gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT2, GL30.GL_TEXTURE_2D,
                material.getTextureObjectHandle(), 0);
        gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_TEXTURE_2D,
                depth.getTextureObjectHandle(), 0);

        IntBuffer buffer = BufferUtils.newIntBuffer(3);
        buffer.put(GL30.GL_COLOR_ATTACHMENT0);
        buffer.put(GL30.GL_COLOR_ATTACHMENT1);
        buffer.put(GL30.GL_COLOR_ATTACHMENT2);
        buffer.position(0);
        Gdx.gl30.glDrawBuffers(3, buffer);

        gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);
        gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);

        int result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);

        gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, defaultFramebufferHandle);

        if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
            for (Texture colorTexture : colorTextures)
                disposeColorTexture(colorTexture);

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

    /** Releases all resources associated with the FrameBuffer. */
    @Override
    public void dispose() {
        GL20 gl = Gdx.gl20;

        for (Texture textureAttachment : colorTextures) {
            disposeColorTexture(textureAttachment);
        }

        gl.glDeleteFramebuffer(framebufferHandle);

        if (buffers.get(Gdx.app) != null)
            buffers.get(Gdx.app).removeValue(this, true);
    }

    /** Makes the frame buffer current so everything gets drawn to it. */
    public void bind() {
        Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
    }

    /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on. */
    public static void unbind() {
        Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, 0);
    }

    /** Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it. */
    public void begin() {
        bind();
        setFrameBufferViewport();
    }

    /** Sets viewport to the dimensions of framebuffer. Called by {@link #begin()}. */
    protected void setFrameBufferViewport() {
        Gdx.gl20.glViewport(0, 0, colorTextures.first().getWidth(), colorTextures.first().getHeight());
    }

    /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on. */
    public void end() {
        end(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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

    public Texture getColorBufferTexture(int index) {
        return colorTextures.get(index);
    }

    /** @return the height of the framebuffer in pixels */
    public int getHeight() {
        return colorTextures.first().getHeight();
    }

    /** @return the width of the framebuffer in pixels */
    public int getWidth() {
        return colorTextures.first().getWidth();
    }

    /** @return the depth of the framebuffer in pixels (if applicable) */
    public int getDepth() {
        return colorTextures.first().getDepth();
    }

    private static void addManagedFrameBuffer(Application app, MRTFrameBuffer frameBuffer) {
        Array<MRTFrameBuffer> managedResources = buffers.get(app);
        if (managedResources == null)
            managedResources = new Array<MRTFrameBuffer>();
        managedResources.add(frameBuffer);
        buffers.put(app, managedResources);
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
}