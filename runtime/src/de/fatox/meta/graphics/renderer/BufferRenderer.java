package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import de.fatox.meta.Meta;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.api.graphics.ShaderInfo;
import de.fatox.meta.api.graphics.ShaderLibrary;
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

    private FullscreenQuad fsquad = new FullscreenQuad();


    private MRTFrameBuffer mrtFrameBuffer;
    private RenderContext renderContext;
    private Array<Renderable> renderables = new Array<Renderable>();
    private RenderablePool renerablePool = new RenderablePool();

    public BufferRenderer() {
        Meta.inject(this);
        cam.update();
        rebuild(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    }

    public void render(float x, float y, float width, float height) {
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

        FullscreenShader mrtSceneShader = (FullscreenShader) shaderLibrary.getOutputShader().getShader();
        mrtSceneShader.begin(cam, renderContext);
////        mrtFrameBuffer.getColorBufferTexture(0).bind(21);
//        mrtSceneShader.setUniformi("u_normalTexture",
//                renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(1)));
        mrtSceneShader.getProgram().setUniformi("s_albedoTex", renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(0)));
        mrtSceneShader.getProgram().setUniformi("s_depthTex", renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(3)));
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
        batch.draw(mrtFrameBuffer.getColorBufferTexture(0), x, y, width / 4f, height / 4f, 0f, 0f, 1f, 1f);
        batch.draw(mrtFrameBuffer.getColorBufferTexture(1), x + width / 4f, y, width / 4f, height / 4f, 0f, 0f, 1f, 1f);
        batch.draw(mrtFrameBuffer.getColorBufferTexture(2), x + 2 * width / 4f, y, width / 4f, height / 4f, 0f, 0f, 1f, 1f);
        batch.draw(mrtFrameBuffer.getColorBufferTexture(3), x + 3 * width / 4f, y, width / 4f, height / 4f, 0f, 0f, 1f, 1f);
        batch.end();
    }

    @Override
    public void rebuild(int width, int height) {
        mrtFrameBuffer = new MRTFrameBuffer(width, height, 3);
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN));
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
