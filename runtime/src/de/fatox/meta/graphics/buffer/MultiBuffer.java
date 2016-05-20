package de.fatox.meta.graphics.buffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

/**
 * Stores all rendertargets from all buffers
 */
public class MultiBuffer {
    @Inject
    @Log
    private Logger log;
    @Inject
    private ShaderLibrary shaderLibrary;
    @Inject
    private EntityManager<Meta3DEntity> entityManager;

    private int width, height;
    // The opengl-handle of the FrameBufferObject
    public int deferredFBO;
    public Array<Buffer> buffers = new Array<>(1);
    // Addresscounter for adding buffers
    private int bufferAddress = 0;
    private int chosenBuffer = 0;

    public MultiBuffer(int addressStart) {
        Meta.inject(this);
        bufferAddress = addressStart;
        rebuild();
    }

    /**
     * Must be called before using the MultiBuffer
     *
     * @return true if the FBO got initiated successfully
     */
    public boolean rebuild() {
        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        int address = bufferAddress;
        for (final ShaderInfo shaderInfo : shaderLibrary.getActiveShaders()) {
            Buffer buf = new Buffer(address, shaderInfo.getRenderTargets()) {

                @Override
                public void draw(ModelBatch mbatch, PerspectiveCamera cam) {
                    mbatch.begin(cam);
                    for (Meta3DEntity e : entityManager.getEntities()) {
                        // TODO culling
                        mbatch.render(e.getActor(), shaderInfo.getShader());
                    }
                    mbatch.end();
                }
            };
            address += shaderInfo.getRenderTargets().length;
            buffers.add(buf);
        }


        boolean success = setupFBO();
        System.out.println("Success? " + success);
        return success;
    }

    private boolean setupFBO() {
        // delete existing fbo, and render buffer in case we are regenerating at new size
        Gdx.gl.glDeleteFramebuffer(deferredFBO);
        // Create and bind the FBO
        deferredFBO = Gdx.gl.glGenFramebuffer();
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, deferredFBO);

        for(Buffer buffer : buffers) {
            buffer.setupTextures(bufferAddress);
            bufferAddress += buffer.getSize();
        }
        FrameBuffer.unbind();
        Gdx.gl.glActiveTexture(GL30.GL_TEXTURE0);
        Gdx.gl.glBindTexture(GL30.GL_TEXTURE_2D, 0);

        if (!checkStatus()) {
            Gdx.app.error("setupFbo()", "Could not create framebuffer");
            return false;
        }
        Gdx.app.log("setupFbo()", "FBO setup successful");
        return true;
    }

    /**
     * Checks the status of the FBO
     *
     * @return true if the FBO is complete
     */
    private boolean checkStatus() {
        System.out.print("FBO status: ");
        int status = Gdx.gl.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        switch (status) {
            case (GL30.GL_FRAMEBUFFER_COMPLETE):
                System.out.println("complete");
                return true;
            case (GL30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE):
                System.out.println("incomplete multisample");
                break;
            case (GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT):
                System.out.println("incomplete attachment");
                break;
            case (GL30.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS):
                System.out.println("incomplete dimensions");
                break;
            case (GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT):
                System.out.println("incomplete_missing attachment");
                break;
            default:
                System.out.println("unknown error");
        }
        return false;
    }

    public void renderAll(ModelBatch modelBatch, ModelBatch depthBatch, PerspectiveCamera cam) {
        Gdx.gl.glBindFramebuffer(GL30.GL_FRAMEBUFFER, deferredFBO);
        for(Buffer buffer : buffers) {
            buffer.bind();
            buffer.draw(modelBatch, cam);
        }
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        FrameBuffer.unbind();
    }

    public void debugAll() {
        Gdx.gl.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, deferredFBO);
        int buffs = 0;
        for (Buffer buffer : buffers) {
            buffs += buffer.getSize();
        }
        int quarterW = width / Math.max(4, buffs);
        int quarterH = height / Math.max(4, buffs);

        for (int i = 0; i < buffs; i++) {
            Gdx.gl30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0+i);
            Gdx.gl30.glBlitFramebuffer(0, 0, width, height, quarterW*i, 0, quarterW*(i+1), quarterH, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);
        }
        FrameBuffer.unbind();
    }
}
