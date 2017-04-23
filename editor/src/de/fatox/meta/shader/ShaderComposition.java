package de.fatox.meta.shader;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import de.fatox.meta.Meta;
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

    public ShaderComposition(MetaRenderData data) {
        Meta.inject(this);
        this.data = data;
        for (int i = 0; i < data.renderBuffers.size; i++) {
            MetaGeoShader metaGeoShader = new MetaGeoShader(shaderLibrary.getShaderHandle(data.renderBuffers.get(i).metaShaderPath));
            bufferHandles.add(new RenderBufferHandle(data.renderBuffers.get(i), metaGeoShader));
        }
    }

    public void addBufferHandle(RenderBufferHandle bufferHandle) {
        if(! bufferHandles.contains(bufferHandle, true)) {
            bufferHandles.add(bufferHandle);
            if(bufferHandle.data == null) {
                throw new GdxRuntimeException("bufferHandle has no data attached");
            }
            data.renderBuffers.add(bufferHandle.data);
        }
    }

    public ShaderComposition(String name) {
        data = new MetaRenderData(name);
    }

    public Array<RenderBufferHandle> getBufferHandles() {
        return bufferHandles;
    }
}
