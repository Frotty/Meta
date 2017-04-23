package de.fatox.meta.shader;

import com.badlogic.gdx.utils.Array;
import de.fatox.meta.api.dao.MetaRenderData;
import de.fatox.meta.api.graphics.RenderBufferHandle;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 10.04.2017.
 */
public class ShaderComposition {
    @Inject
    private MetaShaderLibrary shaderLibrary;
    public MetaRenderData data;
    private Array<RenderBufferHandle> bufferHandles = new Array<>();

    public ShaderComposition() {
        for (int i = 0; i < data.renderBuffers.size; i++) {
            shaderLibrary.getShader(data.renderBuffers.items[i].metaShaderPath);
//            bufferHandles.add(new RenderBufferHandle(data.renderBuffers.items[i], ));

        }
    }

    public void addBufferHandle(RenderBufferHandle bufferHandle) {
        if(! bufferHandles.contains(bufferHandle, true)) {
            bufferHandles.add(bufferHandle);
            data.renderBuffers.add(bufferHandle.data);
        }
    }

    public ShaderComposition(MetaRenderData data) {
        this.data = data;
    }

    public ShaderComposition(String name) {
        data = new MetaRenderData(name);
    }

    public Array<RenderBufferHandle> getBufferHandles() {
        return bufferHandles;
    }
}
