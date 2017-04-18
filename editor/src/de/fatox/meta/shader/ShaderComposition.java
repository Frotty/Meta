package de.fatox.meta.shader;

import com.badlogic.gdx.utils.Array;
import de.fatox.meta.api.dao.MetaRenderData;
import de.fatox.meta.api.graphics.RenderBufferHandle;

/**
 * Created by Frotty on 10.04.2017.
 */
public class ShaderComposition {
    public MetaRenderData data;
    private Array<RenderBufferHandle> bufferHandles = new Array<>();

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
