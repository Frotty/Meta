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
import java.nio.ByteBuffer

/**
 * A [SeekableByteChannel] implementation that wraps a byte[].
 *
 *
 * When this channel is used for writing an internal buffer grows to accommodate incoming data. The natural size
 * limit is the value of [Integer.MAX_VALUE] and it is not possible to [set the position][.position] or
 * [truncate][.truncate] to a value bigger than that.  Internal buffer can be accessed via [ ][XPKByteChannel.array].
 *
 * @since 1.13
 * @NotThreadSafe
 */
class XPKByteChannel @JvmOverloads constructor(private var data: ByteArray = ByteArray(0)) : SeekableByteChannel {
    private val closed = AtomicBoolean()
    private var position = 0
    private var size: Int

    /**
     * Constructor taking a size of storage to be allocated.
     *
     *
     * Creates a channel and allocates internal storage of a given size.
     *
     * @param size size of internal buffer to allocate, in bytes.
     */
    constructor(size: Int) : this(ByteArray(size)) {}

    /**
     * Returns this channel's position.
     *
     *
     * This method violates the contract of [SeekableByteChannel.position] as it will not throw any exception
     * when invoked on a closed channel. Instead it will return the position the channel had when close has been
     * called.
     */
    override fun position(): Long {
        return position.toLong()
    }

    @Throws(IOException::class)
    override fun position(newPosition: Long): SeekableByteChannel {
        ensureOpen()
        require(!(newPosition < 0L || newPosition > Int.MAX_VALUE)) { "Position has to be in range 0.. " + Int.MAX_VALUE }
        position = newPosition.toInt()
        return this
    }

    /**
     * Returns the current size of entity to which this channel is connected.
     *
     *
     * This method violates the contract of [SeekableByteChannel.size] as it will not throw any exception when
     * invoked on a closed channel. Instead it will return the size the channel had when close has been called.
     */
    override fun size(): Long {
        return size.toLong()
    }

    /**
     * Truncates the entity, to which this channel is connected, to the given size.
     *
     *
     * This method violates the contract of [SeekableByteChannel.truncate] as it will not throw any exception when
     * invoked on a closed channel.
     */
    override fun truncate(newSize: Long): SeekableByteChannel {
        require(!(newSize < 0L || newSize > Int.MAX_VALUE)) { "Size has to be in range 0.. " + Int.MAX_VALUE }
        if (size > newSize) {
            size = newSize.toInt()
        }
        if (position > newSize) {
            position = newSize.toInt()
        }
        return this
    }

    @Throws(IOException::class)
    override fun read(buf: ByteBuffer): Int {
        ensureOpen()
        var wanted = buf.remaining()
        val possible = size - position
        if (possible <= 0) {
            return -1
        }
        if (wanted > possible) {
            wanted = possible
        }
        buf.put(data, position, wanted)
        position += wanted
        return wanted
    }

    override fun close() {
        closed.set(true)
    }

    override fun isOpen(): Boolean {
        return !closed.get()
    }

    @Throws(IOException::class)
    override fun write(b: ByteBuffer): Int {
        ensureOpen()
        var wanted = b.remaining()
        val possibleWithoutResize = size - position
        if (wanted > possibleWithoutResize) {
            val newSize = position + wanted
            if (newSize < 0) { // overflow
                resize(Int.MAX_VALUE)
                wanted = Int.MAX_VALUE - position
            } else {
                resize(newSize)
            }
        }
        b[data, position, wanted]
        position += wanted
        if (size < position) {
            size = position
        }
        return wanted
    }

    /**
     * Obtains the array backing this channel.
     *
     *
     * NOTE:
     * The returned buffer is not aligned with containing data, use
     * [.size] to obtain the size of data stored in the buffer.
     *
     * @return internal byte array.
     */
    fun array(): ByteArray {
        return data
    }

    private fun resize(newLength: Int) {
        var len = data.size
        if (len <= 0) {
            len = 1
        }
        if (newLength < NAIVE_RESIZE_LIMIT) {
            while (len < newLength) {
                len = len shl 1
            }
        } else { // avoid overflow
            len = newLength
        }
        data = Arrays.copyOf(data, len)
    }

    @Throws(ClosedChannelException::class)
    private fun ensureOpen() {
        if (!isOpen) {
            throw ClosedChannelException()
        }
    }

    companion object {
        private const val NAIVE_RESIZE_LIMIT = Int.MAX_VALUE shr 1
    }
    /**
     * Constructor taking a byte array.
     *
     *
     * This constructor is intended to be used with pre-allocated buffer or when
     * reading from a given byte array.
     *
     * @param data input data or pre-allocated array.
     */
    /**
     * Parameterless constructor - allocates internal buffer by itself.
     */
    init {
        size = data.size - HASH_LENGTH
    }
}