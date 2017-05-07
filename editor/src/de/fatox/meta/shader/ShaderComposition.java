package de.fatox.meta.shader;

import com.badlogic.gdx.files.FileHandle;
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
    private RenderBufferHandle outputBuffer;
    private FileHandle compositionHandle;

    public ShaderComposition(FileHandle compHandle, MetaRenderData metaRenderData) {
        Meta.inject(this);
        this.compositionHandle = compHandle;
        this.data = metaRenderData;
        loadExisting();
    }

    private void loadExisting() {
        for (int i = 0; i < data.renderBuffers.size-1; i++) {
            MetaGeoShader metaGeoShader = new MetaGeoShader(shaderLibrary.getShaderHandle(data.renderBuffers.get(i).metaShaderPath));
            metaGeoShader.init();
            bufferHandles.add(new RenderBufferHandle(data.renderBuffers.get(i), metaGeoShader));
        }
        MetaGeoShader metaGeoShader = new MetaGeoShader(shaderLibrary.getShaderHandle(data.renderBuffers.get(data.renderBuffers.size-1).metaShaderPath));
        metaGeoShader.init();
        outputBuffer = new RenderBufferHandle(data.renderBuffers.get(data.renderBuffers.size-1), metaGeoShader);
        System.out.println("Loaded <" + bufferHandles.size + "> buffers");
    }

    public void addBufferHandle(RenderBufferHandle bufferHandle) {
        if(! bufferHandles.contains(bufferHandle, true) && outputBuffer != bufferHandle) {
            if(outputBuffer != null) {
                bufferHandles.add(outputBuffer);
            }
            if(bufferHandle.data == null) {
                throw new GdxRuntimeException("bufferHandle has no data attached");
            }
            outputBuffer = bufferHandle;
            data.renderBuffers.add(bufferHandle.data);
        }
    }

    public Array<RenderBufferHandle> getBufferHandles() {
        return bufferHandles;
    }

    public FileHandle getCompositionHandle() {
        return compositionHandle;
    }

    @Override
    public String toString() {
        return data.name;
    }

    public RenderBufferHandle getOutputBuffer() {
        return outputBuffer;
    }
}
