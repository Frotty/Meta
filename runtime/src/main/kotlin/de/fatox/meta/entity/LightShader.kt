package de.fatox.meta.entity

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
import com.badlogic.gdx.Input.Buttons
import de.fatox.meta.api.entity.EntityManager
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import de.fatox.meta.entity.LightEntity
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import de.fatox.meta.graphics.buffer.MultisampleFBO
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import java.nio.IntBuffer
import java.lang.Exception
import com.badlogic.gdx.Application.ApplicationType
import com.badlogic.gdx.graphics.*
import java.nio.ByteOrder
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import java.lang.IllegalStateException
import java.util.HashMap
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import de.fatox.meta.injection.Qualifier
import de.fatox.meta.injection.MetastasisException
import de.fatox.meta.injection.Metastasis
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashSet
import java.util.HashSet
import de.fatox.meta.injection.Provides

/**
 * Final composite Shader that turns the gbuffers into a composite texture,
 * which is then rendered to a fullscreen Quad.
 */
class LightShader : Shader {
    var program: ShaderProgram? = null
        private set
    private var u_InverseScreenSize = 0
    private var u_WorldTrans = 0
    private var u_ProjTrans = 0
    private var u_ProjViewTrans = 0
    private var u_ViewTrans = 0
    private var u_MvpTrans = 0
    private var u_FarDistance = 0
    private var u_LightPosition = 0
    private var u_NormalTrans = 0
    private var u_CamPos = 0

    //Light
    private var u_LightRadius = 0
    private var u_LightColor = 0
    private var u_invProjTrans = 0
    private var u_MvTrans = 0
    override fun init() {
        val vert = Gdx.files.internal("shaders/lightpoint.vert").readString()
        val frag = Gdx.files.internal("shaders/lightpoint.frag").readString()
        program = ShaderProgram(vert, frag)
        if (!program!!.isCompiled) {
            throw GdxRuntimeException(program!!.log)
        } else {
            println(
                """
    Shader compiled correctly. Appending log:
    ${program!!.log}
    """.trimIndent()
            )
        }
        u_InverseScreenSize = program!!.getUniformLocation("u_inverseScreenSize")
        u_WorldTrans = program!!.getUniformLocation("u_worldTrans")
        u_MvpTrans = program!!.getUniformLocation("u_mvpTrans")
        u_MvTrans = program!!.getUniformLocation("u_mvTrans")
        u_FarDistance = program!!.getUniformLocation("u_farDistance")
        u_LightPosition = program!!.getUniformLocation("u_lightPosition")
        u_ProjTrans = program!!.getUniformLocation("u_projTrans")
        u_ProjViewTrans = program!!.getUniformLocation("u_projViewTrans")
        u_invProjTrans = program!!.getUniformLocation("u_invProjTrans")
        u_ViewTrans = program!!.getUniformLocation("u_viewTrans")
        u_NormalTrans = program!!.getUniformLocation("u_normalTrans")
        u_LightRadius = program!!.getUniformLocation("u_lightRadius")
        u_LightColor = program!!.getUniformLocation("u_lightColor")
        u_CamPos = program!!.getUniformLocation("u_camPos")
    }

    override fun dispose() {
        program!!.dispose()
    }

    private val tmpM = Matrix3()
    val temp = Matrix4()
    var tempV = Vector3()
    private var camera: Camera? = null
    private var context: RenderContext? = null
    override fun render(renderable: Renderable) {
        context!!.setDepthMask(false)
        context!!.setDepthTest(GL20.GL_GEQUAL)
        context!!.setBlending(true, GL20.GL_ONE, GL20.GL_ONE)
        context!!.setCullFace(GL20.GL_FRONT)
        renderable.worldTransform.getTranslation(tempV)
        program!!.setUniformf(u_LightPosition, tempV)
        program!!.setUniformf(u_LightRadius, 50f)
        program!!.setUniformMatrix(u_WorldTrans, renderable.worldTransform)
        temp.set(camera!!.combined).mul(renderable.worldTransform)
        program!!.setUniformMatrix(u_MvpTrans, temp)
        temp.set(camera!!.view).mul(renderable.worldTransform)
        program!!.setUniformMatrix(u_MvTrans, temp)
        tmpM.set(renderable.worldTransform).inv().transpose()
        program!!.setUniformMatrix(u_NormalTrans, tmpM)
        renderable.meshPart.render(program)
    }

    override fun end() { // program.end is no longer necessary
    }

    override fun compareTo(other: Shader): Int {
        return 0
    }

    override fun canRender(renderable: Renderable): Boolean {
        return true
    }

    override fun begin(camera: Camera, context: RenderContext) {
        this.camera = camera
        this.context = context
        program!!.bind()
        program!!.setUniformMatrix(u_ProjTrans, camera.projection)
        program!!.setUniformMatrix(u_invProjTrans, camera.invProjectionView)
        program!!.setUniformMatrix(u_ViewTrans, camera.view)
        program!!.setUniformMatrix(u_ProjViewTrans, camera.combined)
        //        program.setUniformf("u_nearDistance", camera.near);
        //        program.setUniformf("u_farDistance", camera.far);
        program!!.setUniformf(u_FarDistance, camera.far)
        program!!.setUniformf(u_CamPos, camera.position)
    }
}