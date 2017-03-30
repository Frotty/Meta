package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
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
    private RenderContext renderContext;
    private Array<Renderable> renderables = new Array<Renderable>();
    private RenderablePool renerablePool = new RenderablePool();

    private Array<LightEntity> lights = new Array<>();

    public BufferRenderer() {
        Meta.inject(this);
        cam.update();
        rebuild(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        lights.add(new LightEntity(new Vector3(0, 50, 0), 150, Vector3.X));
        lights.add(new LightEntity(new Vector3(50, 0, 0), 150, Vector3.Y));
    }

    public void render(float x, float y, float width, float height) {
        ShaderProgram.pedantic = false;
        mrtFrameBuffer.begin();
        renderContext.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        renerablePool.flush();
        renderables.clear();

        modelCache.begin(cam);
        for (Meta3DEntity entity : entityManager.getEntities()) {
            modelCache.add(entity.getActor());
        }
        modelCache.end();
        modelCache.getRenderables(renderables, renerablePool);

        ShaderInfo shaderInfo = shaderLibrary.getActiveShaders().get(0);
        shaderInfo.getShader().begin(cam, renderContext);

        for (Renderable renderable : renderables) {
            shaderInfo.getShader().render(renderable);
        }
        shaderInfo.getShader().end();
        mrtFrameBuffer.end();
        renderContext.end();
        renderContext.begin();
        lightingBuffer.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        ShaderInfo shaderInfo2 = shaderLibrary.getActiveShaders().get(1);
        LightShader shader = (LightShader) shaderInfo2.getShader();
        shader.begin(cam, renderContext);
        int bind = renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(3));
        shader.getProgram().setUniformi("s_depth", bind);
        shader.getProgram().setUniformi("s_normal", renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(1)));
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
//        mrtSceneShader.getProgram().setUniformi("s_albedoTex", renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(0)));
        mrtSceneShader.getProgram().setUniformi("s_depthTex", bind);
//        renderContext.bind(mrtFrameBuffer.getColorBufferTexture(0));
////        mrtSceneShader.setUniformf("u_nearDistance", cam.near);
////        mrtSceneShader.setUniformf("u_farDistance", cam.far);
////        //        mrtSceneShader.setUniformMatrix("u_inverseProjectionMatrix", cam.invProjectionView);
        fsquad.render(mrtSceneShader.getProgram());
        mrtSceneShader.end();
        // Render to Screen

        renderContext.end();
        debugAll(x, y, width, height);
    }

    private void debugAll(float x, float y, float width, float height) {
        batch.disableBlending();
        batch.begin();
        float debugScreens = 4;
        batch.draw(mrtFrameBuffer.getColorBufferTexture(0), x, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
        batch.draw(mrtFrameBuffer.getColorBufferTexture(1), x + width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
        batch.draw(mrtFrameBuffer.getColorBufferTexture(3), x + 2 * width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
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
        }
        mrtFrameBuffer = new MRTFrameBuffer(width, height, 4);
        lightingBuffer = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN, 5));
        modelBatch = new ModelBatch(renderContext);
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
