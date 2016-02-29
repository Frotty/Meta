package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import de.fatox.meta.Meta;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.graphics.buffer.MultiBuffer;
import de.fatox.meta.injection.Inject;

public class BufferRenderer implements Renderer {
    @Inject
    private ShaderLibrary shaderLibrary;
    // For regular model rendering
    private ModelBatch modelBatch = new ModelBatch();
    // For the Depthmap
    private ModelBatch depthBatch;
    // Fullscreen Quad for final composition
    private FullscreenQuad fsquad = new FullscreenQuad();

    private DefaultTextureBinder textureBinder = new DefaultTextureBinder(1, 10);

    private MultiBuffer multiBuffer = new MultiBuffer(0);
    private PerspectiveCamera cam;

    public BufferRenderer(PerspectiveCamera cam) {
        Meta.inject(this);
        this.cam = cam;
//		depthBatch = new ModelBatch(new DepthShaderProvider());
    }

    public void render() {
        // Populate Buffers
        multiBuffer.renderAll(modelBatch, cam);
        // Render to Screen
        Gdx.gl30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        Gdx.gl30.glClearDepthf(1.0f);
        Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
        FullscreenShader outputShader = (FullscreenShader) shaderLibrary.getOutputShader().getShader();
        if (outputShader != null) {
            outputShader.getProgram().begin();
            fsquad.render(outputShader.getProgram());
            outputShader.end();
            multiBuffer.debugAll();
        }
    }

    public MultiBuffer getMultiBuffer() {
        return multiBuffer;
    }
}
