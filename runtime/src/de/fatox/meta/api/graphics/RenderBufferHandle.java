package de.fatox.meta.api.graphics;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import de.fatox.meta.api.dao.RenderBufferData;
import de.fatox.meta.graphics.buffer.MRTFrameBuffer;

/**
 * Created by Frotty on 18.04.2017.
 */
public class RenderBufferHandle {
    public RenderBufferData data;
    public MetaGLShader metaShader;

    private MRTFrameBuffer mrtFrameBuffer;
    private FrameBuffer frameBuffer;

    public RenderBufferHandle(RenderBufferData data, MetaGLShader metaShader) {
        this.data = data;
        this.metaShader = metaShader;
    }

    public void rebuild(int width, int height) {
        if (mrtFrameBuffer != null) mrtFrameBuffer.dispose();
        if (frameBuffer != null) frameBuffer.dispose();

        int targetsNum = metaShader.shaderHandle.targets.size;
        if (targetsNum > 1) {
            // MRT Shader
            mrtFrameBuffer = new MRTFrameBuffer(width, height, targetsNum, data.hasDpeth);
        } else {
            // Regular Framebuffer
            frameBuffer = new FrameBuffer(Pixmap.Format.RGB888, width, height, data.hasDpeth);
        }
    }

    public void begin() {
        if(mrtFrameBuffer != null) {
            mrtFrameBuffer.begin();
        } else if(frameBuffer != null) {
            frameBuffer.begin();
        }
    }

    public void end() {
        if(mrtFrameBuffer != null) {
            mrtFrameBuffer.end();
        } else if(frameBuffer != null) {
            frameBuffer.end();
        }
    }

}
