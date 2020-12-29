package de.fatox.meta.input

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
import com.badlogic.gdx.utils.Array
import de.fatox.meta.injection.Qualifier
import de.fatox.meta.injection.MetastasisException
import de.fatox.meta.injection.Metastasis
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashSet
import java.util.HashSet
import de.fatox.meta.injection.Provides

class MetaInput : InputProcessor {
    private val globalKeyListeners = IntMap<Array<KeyListener>>()
    private val screenKeyListeners = IntMap<Array<KeyListener>>()
    private var exclusiveProcessor: InputProcessor? = null
    private val globalProcessors = Array<InputProcessor>()
    private val screenProcessors = Array<InputProcessor>()
    fun changeScreen() {
        screenKeyListeners.clear()
        screenProcessors.clear()
    }

    fun addAdapterForScreen(adapter: InputProcessor) {
        screenProcessors.add(adapter)
    }

    fun registerGlobalKeyListener(keycode: Int, keyListener: KeyListener) {
        registerGlobalKeyListener(keycode, 0, keyListener)
    }

    fun registerGlobalKeyListener(keycode: Int, milisRequired: Long, keyListener: KeyListener) {
        if (!globalKeyListeners.containsKey(keycode)) {
            globalKeyListeners.put(keycode, Array())
        }
        keyListener.setRequiredLengthMillis(milisRequired)
        globalKeyListeners[keycode].add(keyListener)
    }

    fun registerScreenKeyListener(keycode: Int, keyListener: KeyListener) {
        registerScreenKeyListener(keycode, 0, keyListener)
    }

    fun registerScreenKeyListener(keycode: Int, milisRequired: Long, keyListener: KeyListener) {
        if (!screenKeyListeners.containsKey(keycode)) {
            screenKeyListeners.put(keycode, Array())
        }
        keyListener.setRequiredLengthMillis(milisRequired)
        screenKeyListeners[keycode].add(keyListener)
    }

    override fun keyTyped(character: Char): Boolean {
        if (exclusiveProcessor != null) {
            exclusiveProcessor!!.keyTyped(character)
            return false
        }
        for (processor in globalProcessors) {
            processor.keyTyped(character)
        }
        for (processor in screenProcessors) {
            processor.keyTyped(character)
        }
        return false
    }

    override fun keyDown(keycode: Int): Boolean {
        if (exclusiveProcessor != null) {
            exclusiveProcessor!!.keyDown(keycode)
            return false
        }
        if (screenKeyListeners.containsKey(keycode)) {
            for (listener in screenKeyListeners[keycode]) {
                listener.onDown()
            }
        }
        if (globalKeyListeners.containsKey(keycode)) {
            for (listener in globalKeyListeners[keycode]) {
                listener.onDown()
            }
        }
        for (processor in globalProcessors) {
            processor.keyDown(keycode)
        }
        for (processor in screenProcessors) {
            processor.keyDown(keycode)
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        if (exclusiveProcessor != null) {
            exclusiveProcessor!!.keyUp(keycode)
            return false
        }
        if (screenKeyListeners.containsKey(keycode)) {
            for (listener in screenKeyListeners[keycode]) {
                listener.onUp()
            }
        }
        if (globalKeyListeners.containsKey(keycode)) {
            for (listener in globalKeyListeners[keycode]) {
                listener.onUp()
            }
        }
        for (processor in globalProcessors) {
            processor.keyUp(keycode)
        }
        for (processor in screenProcessors) {
            processor.keyUp(keycode)
        }
        return false
    }

    fun scrolled(amount: Int): Boolean {
        if (exclusiveProcessor != null) {
            exclusiveProcessor!!.scrolled(amount.toFloat())
            return true
        }
        for (processor in globalProcessors) {
            if (processor.scrolled(amount.toFloat())) return true
        }
        for (processor in screenProcessors) {
            if (processor.scrolled(amount.toFloat())) return true
        }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (exclusiveProcessor != null) {
            exclusiveProcessor!!.touchDown(screenX, screenY, pointer, button)
            return true
        }
        for (processor in globalProcessors) {
            if (processor.touchDown(screenX, screenY, pointer, button)) return true
        }
        for (processor in screenProcessors) {
            if (processor.touchDown(screenX, screenY, pointer, button)) return true
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (exclusiveProcessor != null) {
            exclusiveProcessor!!.touchUp(screenX, screenY, pointer, button)
            return false
        }
        for (processor in globalProcessors) {
            if (processor.touchUp(screenX, screenY, pointer, button)) return true
        }
        for (processor in screenProcessors) {
            if (processor.touchUp(screenX, screenY, pointer, button)) return true
        }
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (exclusiveProcessor != null) {
            exclusiveProcessor!!.touchDragged(screenX, screenY, pointer)
            return false
        }
        for (processor in globalProcessors) {
            if (processor.touchDragged(screenX, screenY, pointer)) return true
        }
        for (processor in screenProcessors) {
            if (processor.touchDragged(screenX, screenY, pointer)) return true
        }
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        if (exclusiveProcessor != null) {
            exclusiveProcessor!!.mouseMoved(screenX, screenY)
            return false
        }
        for (processor in globalProcessors) {
            if (processor.mouseMoved(screenX, screenY)) return true
        }
        for (processor in screenProcessors) {
            if (processor.mouseMoved(screenX, screenY)) return true
        }
        return false
    }

    fun registerClick(simpleName: String?, metaClickListener: MetaClickListener?) {}
    fun setExclusiveProcessor(exclusiveProcessor: InputProcessor?) {
        this.exclusiveProcessor = exclusiveProcessor
    }

    fun addGlobalAdapter(processor: InputProcessor) {
        globalProcessors.add(processor)
    }

    fun removeAdapterFromScreen(camControl: ArcCamControl) {
        screenProcessors.removeValue(camControl, true)
    }

    init {
        Gdx.input.inputProcessor = this
        Controllers.addListener(MetaControllerListener(this))
    }
}