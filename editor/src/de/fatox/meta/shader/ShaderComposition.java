package de.fatox.meta.shader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.api.dao.MetaShaderCompData;
import de.fatox.meta.api.dao.RenderBufferData;
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
    private FileHandle compositionHandle;

    public ShaderComposition(FileHandle compHandle, MetaShaderCompData metaShaderCompData) {
        Meta.inject(this);
        this.compositionHandle = compHandle;
        this.data = metaShaderCompData;
        loadExisting();
    }

    private void loadExisting() {
        for (int i = 0; i < data.getRenderBuffers().size; i++) {
            RenderBufferData renderBufferData = data.getRenderBuffers().get(i);
            switch (renderBufferData.getInType()) {
                case GEOMETRY:
                    MetaGeoShader metaGeoShader = new MetaGeoShader(shaderLibrary.getShaderHandle(renderBufferData.getMetaShaderPath()));
                    metaGeoShader.init();
                    bufferHandles.add(new RenderBufferHandle(renderBufferData, metaGeoShader));
                    break;
                case FULLSCREEN:
                    // TODO
                    break;
            }
        }
        System.out.println("Loaded <" + bufferHandles.size + "> buffers");
    }

    public void addBufferHandle(RenderBufferHandle bufferHandle) {
        if (!bufferHandles.contains(bufferHandle, true)) {
            bufferHandles.add(bufferHandle);
            data.getRenderBuffers().add(bufferHandle.getData());
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
        return bufferHandles.peek();
    }

    public void removeBufferHandle(@NotNull RenderBufferHandle handle) {
        bufferHandles.removeValue(handle, true);
        data.getRenderBuffers().removeValue(handle.getData(), true);
    }
}
