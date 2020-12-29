package de.fatox.meta.graphics.renderer

import de.fatox.meta.ui.windows.MetaWindow.contentTable
import de.fatox.meta.ui.windows.MetaWindow.close
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.graphics.FontProvider.getFont
import de.fatox.meta.api.ui.UIRenderer.addActor
import de.fatox.meta.api.PosModifier.modify
import de.fatox.meta.api.ui.UIRenderer.resize
import de.fatox.meta.api.model.MetaWindowData.displayed
import de.fatox.meta.api.model.MetaWindowData.set
import de.fatox.meta.api.model.MetaWindowData.setFrom
import de.fatox.meta.api.lang.LanguageBundle.format
import de.fatox.meta.api.model.MetaAudioVideoData.masterVolume
import de.fatox.meta.api.model.MetaAudioVideoData.musicVolume
import de.fatox.meta.api.AssetProvider.get
import de.fatox.meta.api.model.MetaAudioVideoData.soundVolume
import de.fatox.meta.assets.MetaAssetProvider.get
import de.fatox.meta.api.ui.UIRenderer.getCamera
import de.fatox.meta.ui.windows.MetaWindow
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisLabel
import de.fatox.meta.ui.windows.MetaDialog.DialogListener
import de.fatox.meta.ui.components.MetaClickListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.widget.VisImageButton
import com.badlogic.gdx.utils.Align
import de.fatox.meta.ui.components.MetaTextButton
import kotlin.jvm.JvmOverloads
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import de.fatox.meta.injection.Inject
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.ui.components.MetaLabel
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.Meta
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle
import de.fatox.meta.util.GoldenRatio
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.util.InputValidator
import de.fatox.meta.error.MetaErrorHandler
import com.kotcrab.vis.ui.widget.VisValidatableTextField
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.injection.Singleton
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.input.MetaInput
import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.DummyPosModifier
import de.fatox.meta.api.model.MetaWindowData
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.reflect.ClassReflection
import de.fatox.meta.ui.MetaUiManager
import de.fatox.meta.ui.windows.MetaDialog
import java.lang.InstantiationException
import java.lang.IllegalAccessException
import com.badlogic.gdx.utils.TimeUtils
import kotlin.jvm.Synchronized
import de.fatox.meta.task.TaskListener
import de.fatox.meta.task.MetaTask
import com.badlogic.gdx.Game
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.error.MetaError
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.IntMap
import de.fatox.meta.camera.ArcCamControl
import com.badlogic.gdx.controllers.Controllers
import de.fatox.meta.input.MetaControllerListener
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.PovDirection
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.api.AssetProvider
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.sound.MetaSoundDefinition
import com.badlogic.gdx.math.MathUtils
import de.fatox.meta.sound.MetaSoundHandle
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.sound.MetaSoundPlayer
import java.lang.NoSuchMethodException
import java.lang.reflect.InvocationTargetException
import de.fatox.meta.assets.MetaData.CacheObj
import com.badlogic.gdx.utils.Json
import java.nio.channels.ReadableByteChannel
import de.fatox.meta.assets.HashUtils
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.lang.RuntimeException
import java.io.IOException
import java.math.BigInteger
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Throws
import de.fatox.meta.assets.XPKByteChannel
import java.util.Arrays
import java.nio.channels.ClosedChannelException
import de.fatox.meta.entity.Meta3DEntity
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.Input.Buttons
import de.fatox.meta.api.entity.EntityManager
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import de.fatox.meta.entity.LightEntity
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import de.fatox.meta.graphics.buffer.MultisampleFBO
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import java.nio.IntBuffer
import java.lang.Exception
import com.badlogic.gdx.Application.ApplicationType
import java.nio.ByteOrder
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import java.lang.IllegalStateException
import com.badlogic.gdx.graphics.Pixmap
import java.util.HashMap
import com.badlogic.gdx.graphics.VertexAttribute
import de.fatox.meta.injection.Qualifier
import de.fatox.meta.injection.MetastasisException
import de.fatox.meta.injection.Metastasis
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashSet
import java.util.HashSet
import de.fatox.meta.injection.Provides

class BufferRenderer { //    @Inject
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
}