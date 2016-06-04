package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
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
    private final SpriteBatch batch;
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
        rebuild();
        batch = new SpriteBatch();
    }

    public void render() {
        renderContext.begin();
        mrtFrameBuffer.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renerablePool.flush();
        renderables.clear();

        modelCache.begin(cam);
        for(Meta3DEntity entity : entityManager.getEntities()) {
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

        ShaderProgram  mrtSceneShader =((FullscreenShader) shaderLibrary.getOutputShader().getShader()).getProgram();
        mrtSceneShader.begin();
//        mrtSceneShader.setUniformi("u_diffuseTexture",
//                renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(0)));
//        mrtSceneShader.setUniformi("u_normalTexture",
//                renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(1)));
//        mrtSceneShader.setUniformi("u_positionTexture",
//                renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(2)));
        mrtSceneShader.setUniformi("s_depthTex", renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(3)));
        mrtSceneShader.setUniformf("u_nearDistance", cam.near);
        mrtSceneShader.setUniformf("u_farDistance", cam.far);
        //        mrtSceneShader.setUniformMatrix("u_inverseProjectionMatrix", cam.invProjectionView);
        fsquad.render(mrtSceneShader);
        mrtSceneShader.end();
        renderContext.end();
        // Render to Screen

        debugAll();
    }

    private void debugAll() {
        batch.disableBlending();
        batch.begin();
        batch.draw(mrtFrameBuffer.getColorBufferTexture(0), 0, 0, Gdx.graphics.getWidth() / 4f,
                Gdx.graphics.getHeight() / 4f, 0f, 0f, 1f, 1f);
        batch.draw(mrtFrameBuffer.getColorBufferTexture(1), Gdx.graphics.getWidth() / 4f, 0,
                Gdx.graphics.getWidth() / 4f, Gdx.graphics.getHeight() / 4f, 0f, 0f, 1f, 1f);
        batch.draw(mrtFrameBuffer.getColorBufferTexture(2), 2 * Gdx.graphics.getWidth() / 4f, 0,
                Gdx.graphics.getWidth() / 4f, Gdx.graphics.getHeight() / 4f, 0f, 0f, 1f, 1f);
        batch.draw(mrtFrameBuffer.getColorBufferTexture(3), 3 * Gdx.graphics.getWidth() / 4f, 0,
                Gdx.graphics.getWidth() / 4f, Gdx.graphics.getHeight() / 4f, 0f, 0f, 1f, 1f);
        batch.end();
    }

    @Override
    public void rebuild() {
        mrtFrameBuffer = new MRTFrameBuffer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 3);
        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN));
    }
    protected static class RenderablePool extends Pool<Renderable> {
		protected Array<Renderable> obtained = new Array<Renderable>();

		@Override
		protected Renderable newObject () {
			return new Renderable();
		}

		@Override
		public Renderable obtain () {
			Renderable renderable = super.obtain();
			renderable.environment = null;
			renderable.material = null;
			renderable.meshPart.set("", null, 0, 0, 0);
			renderable.shader = null;
			obtained.add(renderable);
			return renderable;
		}

		public void flush () {
			super.freeAll(obtained);
			obtained.clear();
		}
	}
}
