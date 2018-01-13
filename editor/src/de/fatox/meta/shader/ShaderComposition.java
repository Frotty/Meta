package de.fatox.meta.shader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaShaderCompData;
import de.fatox.meta.api.graphics.RenderBufferHandle;
import de.fatox.meta.injection.Inject;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Frotty on 10.04.2017.
 */
public class ShaderComposition {
    @Inject
    private MetaShaderLibrary shaderLibrary;

    public MetaShaderCompData data;

    private Array<RenderBufferHandle> bufferHandles = new Array<>();
    private RenderBufferHandle outputBuffer;
    private FileHandle compositionHandle;

    public ShaderComposition(FileHandle compHandle, MetaShaderCompData metaShaderCompData) {
        Meta.inject(this);
        this.compositionHandle = compHandle;
        this.data = metaShaderCompData;
        loadExisting();
    }

    private void loadExisting() {
        for (int i = 0; i < data.getRenderBuffers().size - 1; i++) {
            MetaGeoShader metaGeoShader = new MetaGeoShader(shaderLibrary.getShaderHandle(data.getRenderBuffers().get(i).getMetaShaderPath()));
            metaGeoShader.init();
            bufferHandles.add(new RenderBufferHandle(data.getRenderBuffers().get(i), metaGeoShader));
        }
        MetaGeoShader metaGeoShader = new MetaGeoShader(shaderLibrary.getShaderHandle(data.getRenderBuffers().get(data.getRenderBuffers().size - 1)
                .getMetaShaderPath()));
        metaGeoShader.init();
        outputBuffer = new RenderBufferHandle(data.getRenderBuffers().get(data.getRenderBuffers().size - 1), metaGeoShader);
        System.out.println("Loaded <" + bufferHandles.size + "> buffers");
    }

    public void addBufferHandle(RenderBufferHandle bufferHandle) {
        if (!bufferHandles.contains(bufferHandle, true) && outputBuffer != bufferHandle) {
            if (outputBuffer != null) {
                bufferHandles.add(outputBuffer);
            }
            if (bufferHandle.data == null) {
                throw new GdxRuntimeException("bufferHandle has no data attached");
            }
            outputBuffer = bufferHandle;
            data.getRenderBuffers().add(bufferHandle.data);
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
        return data.getName();
    }

    public RenderBufferHandle getOutputBuffer() {
        return outputBuffer;
    }

    public void removeBufferHandle(@NotNull RenderBufferHandle handle) {
        bufferHandles.removeValue(handle, true);
        data.getRenderBuffers().removeValue(handle.data, true);
        if (outputBuffer == handle) {
            outputBuffer = bufferHandles.size > 0 ? bufferHandles.pop() : null;
        }

    }
}
