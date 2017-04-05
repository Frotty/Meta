package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import de.fatox.meta.Meta;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.entity.LightEntity;
import de.fatox.meta.entity.LightShader;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.graphics.buffer.MRTFrameBuffer;
import de.fatox.meta.injection.Inject;

public class BufferRenderer implements Renderer {
    @Inject
    private SpriteBatch batch;
    @Inject
    private ShaderLibrary shaderLibrary;
    @Inject
    private PerspectiveCamera cam;
    @Inject
    private EntityManager<Meta3DEntity> entityManager;

    private ModelCache modelCache = new ModelCache();
    private ModelBatch modelBatch;

    private FullscreenQuad fsquad = new FullscreenQuad();


    private MRTFrameBuffer mrtFrameBuffer;
    private FrameBuffer lightingBuffer;
    private FrameBuffer shadowBuffer;
    private RenderContext renderContext;

    private Array<LightEntity> lights = new Array<>();

    public BufferRenderer() {
        Meta.inject(this);
        cam.update();
        rebuild(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        for (int i = -5; i < 5; i++) {
            for (int j = -5; j < 5; j++) {
                lights.add(new LightEntity(new Vector3(i* 50, 25, j * 50), 50, new Vector3(MathUtils.random(0.1f, 0.9f), MathUtils.random(0.1f, 0.9f),
                        MathUtils.random(0.1f, 0.9f))));
            }
        }
        lights.add(new LightEntity(new Vector3(0, 25, 0), 150, new Vector3(1, 1,1)));
    }

    @Override
    public void rebuildCache() {
        modelCache.begin();
        for (Meta3DEntity entity : entityManager.getEntities()) {
            modelCache.add(entity.getActor());
        }
        modelCache.end();
    }

    public void render(float x, float y) {
        ShaderProgram.pedantic = false;

        mrtFrameBuffer.begin();
        renderContext.begin();
        Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT |
                (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
        ShaderInfo shaderInfo = shaderLibrary.getActiveShaders().get(0);

        modelBatch.begin(cam);
        modelBatch.render(modelCache, shaderInfo.getShader());
        modelBatch.end();
        mrtFrameBuffer.end();
        renderContext.end();
        renderContext.begin();
        lightingBuffer.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT |
                (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));
        ShaderInfo shaderInfo2 = shaderLibrary.getActiveShaders().get(1);
        LightShader shader = (LightShader) shaderInfo2.getShader();
        shader.begin(cam, renderContext);
        shader.getProgram().setUniformf("u_inverseScreenSize", 1.0f / mrtFrameBuffer.getWidth(), 1.0f / mrtFrameBuffer.getHeight());
        int bind = renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(3));
        shader.getProgram().setUniformi("s_depth", bind);
        int bind1 = renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(1));
        shader.getProgram().setUniformi("s_normal", bind1);
        for (LightEntity le : lights) {
            shader.getProgram().setUniformf("u_lightColor", le.color);
            shader.render(le.volumeSphere.getRenderable(new Renderable()));
        }
        shader.end();
        lightingBuffer.end();
        renderContext.end();
        renderContext.begin();
        FullscreenShader mrtSceneShader = (FullscreenShader) shaderLibrary.getOutputShader().getShader();
        mrtSceneShader.begin(cam, renderContext);
////        mrtFrameBuffer.getColorBufferTexture(0).bind(21);
//        mrtSceneShader.setUniformi("u_normalTexture",
//                renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(1)));
        mrtSceneShader.getProgram().setUniformi("s_albedoTex", renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(0)));
        mrtSceneShader.getProgram().setUniformi("s_lightTex", renderContext.textureBinder.bind(lightingBuffer.getColorBufferTexture()));
        shader.getProgram().setUniformi("s_normalTex", bind1);
        mrtSceneShader.getProgram().setUniformi("s_depthTex", bind);
//        renderContext.bind(mrtFrameBuffer.getColorBufferTexture(0));
////        mrtSceneShader.setUniformf("u_nearDistance", cam.near);
////        mrtSceneShader.setUniformf("u_farDistance", cam.far);
////        //        mrtSceneShader.setUniformMatrix("u_inverseProjectionMatrix", cam.invProjectionView);
        fsquad.render(mrtSceneShader.getProgram());
        mrtSceneShader.end();
        // Render to Screen

        renderContext.end();
        debugAll(x, y);
    }

    private void debugAll(float x, float y) {
        batch.disableBlending();
        batch.begin();
        float debugScreens = 4;
        float width = mrtFrameBuffer.getWidth();
        float height = mrtFrameBuffer.getHeight();
        batch.draw(mrtFrameBuffer.getColorBufferTexture(0), x, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
        batch.draw(mrtFrameBuffer.getColorBufferTexture(1), x + width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
        batch.draw(shadowBuffer.getColorBufferTexture(), x + 2 * width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
        batch.draw(lightingBuffer.getColorBufferTexture(), x + 3 * width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
        batch.end();
    }

    @Override
    public void rebuild(int width, int height) {
        if (mrtFrameBuffer != null) {
            if (width == mrtFrameBuffer.getWidth() && height == mrtFrameBuffer.getHeight()) {
                return;
            }
            mrtFrameBuffer.dispose();
            lightingBuffer.dispose();
            shadowBuffer.dispose();
            cam.viewportWidth = width;
            cam.viewportHeight = height;
            cam.update();
        }
        mrtFrameBuffer = new MRTFrameBuffer(width, height, 4);
        lightingBuffer = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        shadowBuffer = new FrameBuffer(Pixmap.Format.RGB888, width, height, true);
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN, 5));
        modelBatch = new ModelBatch(renderContext);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    protected static class RenderablePool extends Pool<Renderable> {
        protected Array<Renderable> obtained = new Array<Renderable>();

        @Override
        protected Renderable newObject() {
            return new Renderable();
        }

        @Override
        public Renderable obtain() {
            Renderable renderable = super.obtain();
            renderable.environment = null;
            renderable.material = null;
            renderable.meshPart.set("", null, 0, 0, 0);
            renderable.shader = null;
            obtained.add(renderable);
            return renderable;
        }

        public void flush() {
            super.freeAll(obtained);
            obtained.clear();
        }
    }
}
