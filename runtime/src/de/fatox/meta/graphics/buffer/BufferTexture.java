package de.fatox.meta.graphics.buffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;

/**
 * Wrapper Class for GBuffer Textures to handle Textureformats that LibGDX's pixmap doesn't support yet
 *
 * @author Frotty
 */
public class BufferTexture extends GLTexture {
    /**
     * The OpenGL internal color format, e.g. GL30.GL_RGB16F
     */
    private int format;
    /**
     * Type of the data. Usually GL30.GL_FLOAT
     */
    private int type;
    /**
     * Attachment Address
     */
    private int bufferAddress;
    /**
     * Bind Address
     */
    private int bindAddress;
    /**
     * The Target attachment of the Framebuffer. This value is added to GL30.GL_COLOR_ATTACHMENT0 when attaching the texture
     */
    private boolean depth = false;
    public final String name;

    public BufferTexture(int bufferAddress, String name) {
        this(GL30.GL_RGB16F, GL30.GL_FLOAT, bufferAddress, name);
    }

    public BufferTexture(int format, int bufferAddress, String name) {
        this(format, GL30.GL_FLOAT, bufferAddress, false, name);
    }

    public BufferTexture(int format, int type, int bufferAddress, String name) {
        this(format, type, bufferAddress, false, name);
    }

    public BufferTexture(int format, int type, int bufferAddress, boolean depth, String name) {
        // Super call generates the textureHandle (glHandle)
        super(GL30.GL_TEXTURE_2D);
        this.name = name;
        minFilter = TextureFilter.Nearest;
        magFilter = TextureFilter.Nearest;
        uWrap = TextureWrap.ClampToEdge;
        vWrap = TextureWrap.ClampToEdge;
        this.format = format;
        this.type = type;
        this.depth = depth;
        this.bufferAddress = bufferAddress;
        generate();
    }

    public void generate() {
        // Bind the texture we want to operate on
        bind();
        unsafeSetWrap(uWrap, vWrap);
        unsafeSetFilter(minFilter, magFilter);
        // Set LOD/mipmaps to zero for the texture to be marked as "complete" in
        // opengl
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_BASE_LEVEL, 0);
        Gdx.gl.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAX_LEVEL, 0);

        if (depth) {
            Gdx.gl.glTexImage2D(glTarget, 0, format, getWidth(), getHeight(), 0, GL30.GL_DEPTH_COMPONENT32F, type, null);
            Gdx.gl.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, glTarget, glHandle, 0);
        } else {
            // Create the texture (previously we generated the TextureHandle,
            // bound it and now configure it)
            Gdx.gl.glTexImage2D(glTarget, 0, format, getWidth(), getHeight(), 0, GL30.GL_RGBA, type, null);
            // Attach the created Texture to the Framebuffer
            // The GL_COLOR_ATTACHMENT target defines which output from the
            // shader gets written into which buffer texture
            System.out.println("Attached to: " + (bufferAddress));
            Gdx.gl.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + bufferAddress, glTarget, glHandle, 0);
        }
    }

    @Override
    public int getWidth() {
        return Gdx.graphics.getWidth();
    }

    @Override
    public int getHeight() {
        return Gdx.graphics.getHeight();
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    protected void reload() {
        glHandle = Gdx.gl.glGenTexture();
        generate();
    }

    public void bind(DefaultTextureBinder textureBinder) {
        bindAddress = textureBinder.bind(this);
    }

    public int getBindAddress() {
        return bindAddress;
    }
}
