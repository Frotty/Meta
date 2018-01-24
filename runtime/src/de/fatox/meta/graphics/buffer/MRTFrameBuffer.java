package de.fatox.meta.graphics.buffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;

import java.nio.IntBuffer;

public class MRTFrameBuffer implements Disposable {
    private FrameBuffer frameBuffer;
    /** width **/
    private final int width;

    /** height **/
    private final int height;

    public MRTFrameBuffer(int width, int height, int textureCount, boolean hasDepth) {
        this.width = width;
        this.height = height;
        build(textureCount, hasDepth);
    }

    private void build(int numTextures, boolean hasDepth) {
        GL20 gl = Gdx.gl20;

        GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(width, height);
        for (int i = 0; i < numTextures; i++) {
            frameBufferBuilder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE);
        }
        if (hasDepth) {
            frameBufferBuilder.addDepthTextureAttachment(GL30.GL_DEPTH_COMPONENT32F, GL30.GL_FLOAT);
        }
        frameBuffer = frameBufferBuilder.build();
        IntBuffer buffer = BufferUtils.newIntBuffer(numTextures);
        for (int i = 0; i < numTextures; i++) {
            buffer.put(GL30.GL_COLOR_ATTACHMENT0 + i);
        }
        buffer.position(0);
        Gdx.gl30.glDrawBuffers(numTextures, buffer);

        gl.glBindRenderbuffer(GL20.GL_RENDERBUFFER, 0);
        gl.glBindTexture(GL20.GL_TEXTURE_2D, 0);

    }

    /** Releases all resources associated with the FrameBuffer. */
    @Override
    public void dispose() {
        frameBuffer.dispose();
    }

    /** Makes the frame buffer current so everything gets drawn to it. */
    public void bind() {
        frameBuffer.bind();
    }

    /** Unbinds the framebuffer, all drawing will be performed to the normal framebuffer from here on. */
    public static void unbind() {
        FrameBuffer.unbind();
    }

    /** Binds the frame buffer and sets the viewport accordingly, so everything gets drawn to it. */
    public void begin() {
        bind();
        setFrameBufferViewport();
    }

    /** Sets viewport to the dimensions of framebuffer. Called by {@link #begin()}. */
    protected void setFrameBufferViewport() {
        Gdx.gl20.glViewport(0, 0, frameBuffer.getWidth(), frameBuffer.getHeight());
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

    /** @return the height of the framebuffer in pixels */
    public int getHeight() {
        return frameBuffer.getHeight();
    }

    /** @return the width of the framebuffer in pixels */
    public int getWidth() {
        return frameBuffer.getWidth();
    }

    public Array<Texture> getColorBufferTextures() {
        return frameBuffer.getTextureAttachments();
    }
}