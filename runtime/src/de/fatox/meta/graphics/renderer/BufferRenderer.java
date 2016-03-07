package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
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

    // For regular model rendering
    private ModelBatch modelBatch = new ModelBatch();
    // For the Depthmap
    private ModelBatch depthBatch;
    // Fullscreen Quad for final composition
    private FullscreenQuad fsquad = new FullscreenQuad();

    private DefaultTextureBinder textureBinder = new DefaultTextureBinder(1, 10);

    private MultiBuffer multiBuffer = new MultiBuffer(0);
    private PerspectiveCamera cam;

    @Inject
    public BufferRenderer(PerspectiveCamera cam) {
        Meta.inject(this);
        this.cam = cam;
//		depthBatch = new ModelBatch(new DepthShaderProvider());
    }

    public void render() {
        // Populate Buffers
        multiBuffer.renderAll(modelBatch, cam);
        // Render to Screen
        ShaderInfo outputShader = shaderLibrary.getOutputShader();
        if (outputShader != null && outputShader.getShader() instanceof FullscreenShader) {
            FullscreenShader shader = (FullscreenShader) outputShader.getShader();
            shader.getProgram().begin();
            fsquad.render(shader.getProgram());
            shader.end();
            multiBuffer.debugAll();
        } else {
            fontProvider.write(300, 300, "Meta", 14);
        }
    }

    public MultiBuffer getMultiBuffer() {
        return multiBuffer;
    }
}
