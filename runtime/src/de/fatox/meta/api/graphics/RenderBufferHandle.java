package de.fatox.meta.api.graphics;

import com.badlogic.gdx.graphics.g3d.Shader;
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

    public RenderBufferHandle(Shader data) {
//        this.data = data;
        setup();
    }

    private void setup() {
    }

}
