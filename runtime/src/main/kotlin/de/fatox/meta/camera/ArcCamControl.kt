package de.fatox.meta.camera

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
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.PerspectiveCamera
import java.nio.ByteOrder
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import java.lang.IllegalStateException
import com.badlogic.gdx.graphics.Pixmap
import java.util.HashMap
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import de.fatox.meta.injection.Qualifier
import de.fatox.meta.injection.MetastasisException
import de.fatox.meta.injection.Metastasis
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashSet
import java.util.HashSet
import de.fatox.meta.injection.Provides

/**
 * Better camera control because I dislike LibGDX's
 * Very similar to World Editor.
 *
 * @author Frotty
 */
class ArcCamControl : InputProcessor {
    /** The button for moving the target.  */
    var moveCameraButton = Buttons.RIGHT
    var resetCameraButton = Buttons.MIDDLE

    /** The units to translate the camera when moved the full width or height of the screen.  */
    var translateUnits = 0.2f // FIXME auto calculate this based on the target

    /** The key which must be pressed to enter rotation mode.  */
    var rotateMode = Input.Keys.CONTROL_LEFT
    protected var rotateModeOn = false
    protected var fastZoomMode = false

    /** The camera.  */
    @Inject
    var camera: PerspectiveCamera? = null

    @Inject
    var entityManager: EntityManager<Meta3DEntity>? = null

    @Inject
    var modelBuilder: ModelBuilder? = null

    /** Are we in moveMode?  */
    private var moveModeOn = false

    /** The target of the arcball  */
    private val target = Vector3.Zero

    /** The planar (X/Y) rotation of the camera  */
    private var rotationAngle = 0f

    /** The angle in which the camera looks onto the target  */
    private var angleOfAttack = 56f

    /** Distance from target  */
    private var distance = 10f
    fun getDistance(): Float {
        return distance
    }

    fun setDistance(distance: Float) {
        this.distance = distance
        update()
    }

    private var startX = 0
    private var startY = 0
    fun update() {
        target.lerp(temp, 0.5f)
        camera!!.position.x = ppX(target.x, distance, rotationAngle, angleOfAttack)
        camera!!.position.y = ppY(0f, distance, angleOfAttack)
        camera!!.position.z = ppZ(target.z, distance, rotationAngle, angleOfAttack)
        camera!!.direction.x = target.x - camera!!.position.x
        camera!!.direction.y = target.y - camera!!.position.y
        camera!!.direction.z = target.z - camera!!.position.z
        camera!!.up.x = -sin(rotationAngle) * cos(angleOfAttack)
        camera!!.up.y = sin(angleOfAttack)
        camera!!.up.z = -cos(rotationAngle) * cos(angleOfAttack)
        camera!!.update()
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == moveCameraButton) {
            startX = screenX
            startY = screenY
            moveModeOn = true
        }
        if (button == resetCameraButton) {
            rotationAngle = 0f
            angleOfAttack = 56f
            distance = 10f
        }
        update()
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == moveCameraButton) {
            startX = screenX
            startY = screenY
            moveModeOn = false
        }
        update()
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        // Touch drag equals clicking a mouseButton and then moving the mouse
        // In case the right mouse button is clicked, we are in MoveMode
        if (moveModeOn) {
            // Calculate the middle of old and new mousePosition
            var deltaX = (screenX - startX).toFloat()
            var deltaY = (startY - screenY).toFloat()
            startX = screenX
            startY = screenY
            // If CTRL is active, we only rotate
            if (rotateModeOn) {
                angleOfAttack += (deltaY * .25).toFloat()
                rotationAngle += (deltaX * .25).toFloat()
            } else {
                // Otherwise we simple move the target
                if (distance < 150) {
                    deltaX *= distance / 100
                    deltaY *= distance / 100
                }
                temp.set(target).add(
                    sin(rotationAngle) * deltaY + cos(rotationAngle) * -deltaX,
                    0f,
                    cos(rotationAngle) * deltaY + sin(rotationAngle) * deltaX
                )
            }
            update()
        }
        return false
    }

    fun scrolled(amount: Int): Boolean {
        return if (fastZoomMode) {
            zoom(amount * translateUnits * 10)
        } else {
            zoom(amount * translateUnits)
        }
    }

    fun zoom(amount: Float): Boolean {
        distance += amount
        update()
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.CONTROL_LEFT) {
            rotateModeOn = true
        } else if (keycode == Input.Keys.SHIFT_LEFT) {
            fastZoomMode = true
        } else {
            yes = !yes
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        if (keycode == Input.Keys.CONTROL_LEFT) {
            rotateModeOn = false
        } else if (keycode == Input.Keys.SHIFT_LEFT) {
            fastZoomMode = false
        }
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    companion object {
        private val temp = Vector3()
        @kotlin.jvm.JvmField
        var yes = true
        private fun cos(aoa: Float): Float {
            return Math.cos((aoa * DEGTORAD).toDouble()).toFloat()
        }

        private fun sin(ang: Float): Float {
            return Math.sin((ang * DEGTORAD).toDouble()).toFloat()
        }

        /** Polar Projection from Wurst  */
        private fun ppX(x: Float, dist: Float, ang: Float, aoa: Float): Float {
            return (x + dist * Math.sin((ang * DEGTORAD).toDouble()) * Math.sin((aoa * DEGTORAD).toDouble())).toFloat()
        }

        private fun ppY(y: Float, dist: Float, ang: Float): Float {
            return (y + dist * Math.cos((ang * DEGTORAD).toDouble())).toFloat()
        }

        private fun ppZ(z: Float, dist: Float, ang: Float, aoa: Float): Float {
            return (z + dist * Math.cos((ang * DEGTORAD).toDouble()) * Math.sin((aoa * DEGTORAD).toDouble())).toFloat()
        }

        const val DEGTORAD = 0.017453293f
    }

    init {
        inject(this)
        update()
    }
}