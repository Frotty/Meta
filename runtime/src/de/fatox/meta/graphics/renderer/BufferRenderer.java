package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import de.fatox.meta.Meta;
import de.fatox.meta.api.graphics.FontProvider;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.graphics.buffer.MultiBuffer;
import de.fatox.meta.injection.Inject;

public class BufferRenderer implements Renderer {
    @Inject
    private ShaderLibrary shaderLibrary;
    @Inject
    private FontProvider fontProvider;
    @Inject
    private PerspectiveCamera cam;

    // For regular model rendering
    private ModelBatch modelBatch = new ModelBatch();
    // For the Depthmap
    private ModelBatch depthBatch = new ModelBatch(new DepthShaderProvider());

    // Fullscreen Quad for final composition
    private FullscreenQuad fsquad = new FullscreenQuad();

    private DefaultTextureBinder textureBinder = new DefaultTextureBinder(1, 10);
    private MultiBuffer multiBuffer = new MultiBuffer(0);

    public BufferRenderer() {
        Meta.inject(this);
//		depthBatch = new ModelBatch(new DepthShaderProvider());
    }

    public void render() {
        // Populate Buffers
        multiBuffer.renderAll(modelBatch, depthBatch, cam);
        // Render to Screen
        ShaderInfo outputShader = shaderLibrary.getOutputShader();
        if (outputShader != null && outputShader.getShader() instanceof FullscreenShader) {
            FullscreenShader shader = (FullscreenShader) outputShader.getShader();
            shader.begin(null, null);
            fsquad.render(shader.getProgram());
            shader.end();
        } else {
//            fontProvider.write(300, 300, "Meta", 14);
        }
        multiBuffer.debugAll();
    }

    public MultiBuffer getMultiBuffer() {
        return multiBuffer;
    }
}
