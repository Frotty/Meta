package de.fatox.meta.assets

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
import com.badlogic.gdx.utils.reflect.ReflectionException
import de.fatox.meta.injection.*
import java.io.File
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashSet
import java.util.HashSet

/**
 * Created by Frotty on 10.03.2017.
 * Handles MetaData needs.
 * De-/serializes config classes from a .meta sub folder in the game's data folder inside user home
 * MetaData can be accessed via #.get and will be cached.
 */
class MetaData {
    internal class CacheObj<T>(var obj: T) {
        var created = TimeUtils.millis()
    }

    @Inject
    @Named("gameName")
    private val gameName: String? = null
    private val fileHandleCache = ObjectMap<String?, FileHandle>()
    private val fileCache = ObjectMap<String?, File>()
    private val jsonCache = ObjectMap<String, CacheObj<out Any>>()
    private val dataRoot: FileHandle
    private val json = Json()
    fun save(key: String?, obj: Any?) {
        save(dataRoot, key, obj)
    }

    fun save(target: FileHandle, key: String?, obj: Any?): FileHandle {
        val jsonString = json.toJson(obj)
        val fileHandle = getCachedHandle(target, key)
        fileHandle.writeBytes(jsonString.toByteArray(), false)
        val cacheObj: CacheObj<*>? = jsonCache.get(key)
        if (cacheObj != null) {
            cacheObj.created = TimeUtils.millis()
            cacheObj.obj = obj
        }
        return fileHandle
    }

    /** Loads and caches the filehandle descripted by the path, if it exists  */
    operator fun get(key: String?): FileHandle {
        return getCachedHandle(dataRoot, key)
    }

    /** Caches and returns this object loaded from json at the default location  */
    operator fun <T> get(type: Class<T>): T {
        return getCachedJson(dataRoot, type.javaClass.simpleName, type)
    }

    /** Caches and returns this object loaded from json at the specified location  */
    operator fun <T> get(key: String, type: Class<T>?): T {
        return getCachedJson(dataRoot, key, type)
    }

    private fun <T> getCachedJson(parent: FileHandle, key: String, type: Class<T>?): T {
        val jsonHandle: T
        if (jsonCache.containsKey(key)) {
            val cacheObj = jsonCache.get(key) as CacheObj<T>
            val lastModified = getCachedFile(key)!!.lastModified()
            if (cacheObj.created < lastModified) {
                cacheObj.obj = json.fromJson(type, getCachedHandle(parent, key))
                cacheObj.created = lastModified
            }
            jsonHandle = cacheObj.obj
        } else {
            val cachedHandle = getCachedHandle(parent, key)
            if (!cachedHandle.exists()) {
                try {
                    cachedHandle.writeBytes(json.toJson(ClassReflection.newInstance(type)).toByteArray(), false)
                } catch (e: ReflectionException) {
                    e.printStackTrace()
                }
            }
            jsonHandle = json.fromJson(type, cachedHandle)
            jsonCache.put(key, CacheObj(jsonHandle))
        }
        return jsonHandle
    }

    operator fun <T> get(target: FileHandle, key: String?, type: Class<T>?): T? {
        val fileHandle = getCachedHandle(target, key)
        return if (fileHandle != null && fileHandle.exists()) {
            json.fromJson(type, fileHandle.readString())
        } else null
    }

    fun getCachedHandle(key: String?): FileHandle {
        return getCachedHandle(dataRoot, key)
    }

    fun getCachedFile(key: String?): File? {
        return if (fileCache.containsKey(key)) {
            fileCache.get(key)
        } else null
    }

    fun getCachedHandle(parent: FileHandle, key: String?): FileHandle {
        var fileHandle: FileHandle
        if (fileHandleCache.containsKey(key)) {
            fileHandle = fileHandleCache.get(key)
        } else {
            fileHandle = parent.child(key)
            if (!fileHandle.exists()) {
                val fileHandle2 = Gdx.files.external(GLOBAL_DATA_FOLDER_NAME + key)
                if (fileHandle2.exists()) {
                    fileHandle = fileHandle2
                }
            }
            fileHandleCache.put(key, fileHandle)
            fileCache.put(key, fileHandle.file())
        }
        return fileHandle
    }

    fun has(name: String?): Boolean {
        return has(dataRoot, name)
    }

    fun has(fileHandle: FileHandle, name: String?): Boolean {
        return fileHandleCache.containsKey(name) || fileHandle.child(name).exists()
    }

    companion object {
        const val GLOBAL_DATA_FOLDER_NAME = ".meta"
    }

    init {
        inject(this)
        dataRoot = Gdx.files.external(".$gameName").child(GLOBAL_DATA_FOLDER_NAME)
        dataRoot.mkdirs()
    }
}