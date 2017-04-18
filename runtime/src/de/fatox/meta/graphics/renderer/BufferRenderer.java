//package de.fatox.meta.graphics.renderer;
//
//import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.graphics.GL30;
//import com.badlogic.gdx.graphics.PerspectiveCamera;
//import com.badlogic.gdx.graphics.Pixmap;
//import com.badlogic.gdx.graphics.g2d.SpriteBatch;
//import com.badlogic.gdx.graphics.g3d.ModelBatch;
//import com.badlogic.gdx.graphics.g3d.ModelCache;
//import com.badlogic.gdx.graphics.g3d.Renderable;
//import com.badlogic.gdx.graphics.g3d.Shader;
//import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
//import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
//import com.badlogic.gdx.graphics.glutils.FrameBuffer;
//import com.badlogic.gdx.graphics.glutils.ShaderProgram;
//import com.badlogic.gdx.math.MathUtils;
//import com.badlogic.gdx.math.Vector3;
//import com.badlogic.gdx.utils.Array;
//import de.fatox.meta.Meta;
//import de.fatox.meta.api.entity.EntityManager;
//import de.fatox.meta.api.graphics.Renderer;
//import de.fatox.meta.entity.LightEntity;
//import de.fatox.meta.entity.LightShader;
//import de.fatox.meta.entity.Meta3DEntity;
//import de.fatox.meta.graphics.buffer.MRTFrameBuffer;
//import de.fatox.meta.injection.Inject;
//
//public class BufferRenderer implements Renderer {
//    @Inject
//    private SpriteBatch batch;
//    @Inject
//    private ShaderLibrary shaderLibrary;
//    @Inject
//    private PerspectiveCamera cam;
//    @Inject
//    private EntityManager<Meta3DEntity> entityManager;
//
//    private ModelCache modelCache = new ModelCache();
//    private ModelBatch modelBatch;
//
//    private FullscreenQuad fsquad = new FullscreenQuad(1);
//
//    private MRTFrameBuffer mrtFrameBuffer;
//    private FrameBuffer lightingBuffer;
//    private FrameBuffer shadowBuffer;
//    private RenderContext renderContext;
//
//    private Array<LightEntity> lights = new Array<>();
//    private FullscreenQuad compositeQuad;
//
//    public BufferRenderer() {
//        Meta.inject(this);
//        cam.update();
//        rebuild(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//        for (int i = -1; i < 1; i++) {
//            for (int j = -1; j < 1; j++) {
//                lights.add(new LightEntity(new Vector3(i * 15, 5, j * 15), 10, new Vector3(MathUtils.random(0.1f, 0.9f), MathUtils.random(0.1f, 0.9f),
//                        MathUtils.random(0.1f, 0.9f))));
//            }
//        }
//    }
//
//    @Override
//    public void rebuildCache() {
//        modelCache.begin();
//        for (Meta3DEntity entity : entityManager.getEntities()) {
//            modelCache.add(entity.getActor());
//        }
//        modelCache.end();
//    }
//
//    public void render(float x, float y) {
//        ShaderProgram.pedantic = false;
//
//        mrtFrameBuffer.begin();
//        renderContext.begin();
//        Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
//        Shader shaderInfo = shaderLibrary.getActiveShaders().get(0);
//
//        modelBatch.begin(cam);
//        modelBatch.render(modelCache, shaderInfo);
//        modelBatch.end();
//        mrtFrameBuffer.end();
//        renderContext.end();
//        renderContext.begin();
//
//        Shader shaderInfo2 = shaderLibrary.getActiveShaders().get(1);
//        LightShader shader = (LightShader) shaderInfo2;
//        shader.begin(cam, renderContext);
//        shader.getProgram().setUniformf("u_inverseScreenSize", 1.0f / mrtFrameBuffer.getWidth(), 1.0f / mrtFrameBuffer.getHeight());
//        int depthBind = renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(3));
//        shader.getProgram().setUniformi("s_depth", depthBind);
//        int normalBind = renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(1));
//        shader.getProgram().setUniformi("s_normal", normalBind);
//        for (LightEntity le : lights) {
//            shader.getProgram().setUniformf("u_lightColor", le.color);
//            shader.render(le.volumeSphere.getRenderable(new Renderable()));
//        }
//        shader.end();
//
//        renderContext.end();
//        renderContext.begin();
//        lightingBuffer.begin();
//        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
//        Gdx.gl30.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
//        FullscreenShader mrtSceneShader = (FullscreenShader) shaderLibrary.getOutputShader().getShader();
//        mrtSceneShader.begin(cam, renderContext);
//        int albedoBind = renderContext.textureBinder.bind(mrtFrameBuffer.getColorBufferTexture(0));
//        mrtSceneShader.getProgram().setUniformi("s_albedoTex", albedoBind);
//        mrtSceneShader.getProgram().setUniformi("s_normalTex", normalBind);
//        mrtSceneShader.getProgram().setUniformi("s_depthTex", depthBind);
////        mrtSceneShader.getProgram().setUniformi("s_lightTex", renderContext.textureBinder.bind(lightingBuffer.getColorBufferTexture()));
//        fsquad.render(mrtSceneShader.getProgram());
//        mrtSceneShader.end();
//        lightingBuffer.end();
//        FullscreenShader blurShader = (FullscreenShader) shaderLibrary.getActiveShaders().get(2);
//        blurShader.begin(cam, renderContext);
//        blurShader.getProgram().setUniformi("s_albedoTex", albedoBind);
//        blurShader.getProgram().setUniformi("s_inputTex", renderContext.textureBinder.bind(lightingBuffer.getColorBufferTexture()));
//        compositeQuad.render(blurShader.getProgram());
//        blurShader.end();
//
//        renderContext.end();
//        debugAll(x, y);
//    }
//
//    private void debugAll(float x, float y) {
//        batch.disableBlending();
//        batch.begin();
//        float debugScreens = 4;
//        float width = mrtFrameBuffer.getWidth();
//        float height = mrtFrameBuffer.getHeight();
//        batch.draw(mrtFrameBuffer.getColorBufferTexture(0), x, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
//        batch.draw(mrtFrameBuffer.getColorBufferTexture(1), x + width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
//        batch.draw(mrtFrameBuffer.getColorBufferTexture(3), x + 2 * width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
//        batch.draw(lightingBuffer.getColorBufferTexture(), x + 3 * width / debugScreens, y, width / debugScreens, height / debugScreens, 0f, 0f, 1f, 1f);
//        batch.end();
//    }
//
//    @Override
//    public void rebuild(int width, int height) {
//        if (mrtFrameBuffer != null) {
//            if (width == mrtFrameBuffer.getWidth() && height == mrtFrameBuffer.getHeight()) {
//                return;
//            }
//            mrtFrameBuffer.dispose();
//            lightingBuffer.dispose();
//            shadowBuffer.dispose();
//            compositeQuad.dispose();
//            cam.viewportWidth = width;
//            cam.viewportHeight = height;
//            cam.update();
//        }
//        mrtFrameBuffer = new MRTFrameBuffer(width, height);
//        lightingBuffer = new FrameBuffer(Pixmap.Format.RGB888, width, height, false);
//        shadowBuffer = new FrameBuffer(Pixmap.Format.RGB888, width, height, true);
//        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.ROUNDROBIN, 5));
//        modelBatch = new ModelBatch(renderContext);
//        compositeQuad = new FullscreenQuad(1f - ((1f - ((float) (height) / (float) (Gdx.graphics.getHeight()))) * 2f));
//        batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//    }
//
//}
