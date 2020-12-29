package de.fatox.meta.sound

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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Array
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.Qualifier
import de.fatox.meta.injection.MetastasisException
import de.fatox.meta.injection.Metastasis
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashSet
import java.util.HashSet
import de.fatox.meta.injection.Provides

class MetaSoundPlayer {
    private val soundDefinitions = ObjectMap<String, MetaSoundDefinition>()
    private val playingHandles = ObjectMap<MetaSoundDefinition, Array<MetaSoundHandle>>()
    private val dynamicHandles = Array<MetaSoundHandle>()

    @Inject
    private val metaAssetProvider: MetaAssetProvider? = null

    @Inject
    private val shapeRenderer: ShapeRenderer? = null

    @Inject
    private val spriteBatch: SpriteBatch? = null

    @Inject
    private val uiRenderer: UIRenderer? = null

    @Inject
    private val metaData: MetaData? = null
    @JvmOverloads
    fun playSound(
        soundDefinition: MetaSoundDefinition?,
        listenerPos: Vector2? = null,
        soundPos: Vector2? = null
    ): MetaSoundHandle? {
        if (soundDefinition == null) return null
        val audioVideoData = metaData!!.get("audioVideoData", MetaAudioVideoData::class.java)
        val volume = audioVideoData!!.masterVolume * audioVideoData.soundVolume
        if (volume <= 0) {
            return null
        }
        if (listenerPos != null && !isInAudibleRange(soundDefinition, listenerPos, soundPos)) {
            return null
        }
        if (!playingHandles.containsKey(soundDefinition)) {
            // Create handlelist if sound is played for the first time
            playingHandles.put(soundDefinition, Array(soundDefinition.maxInstances))
        }
        val handleList = playingHandles.get(soundDefinition)
        cleanupHandles(handleList)
        if (handleList.size >= soundDefinition.maxInstances || handleList.size > 0 && handleList.first().startTime + 200 >= TimeUtils.millis()) {
            return null
        }
        if (soundDefinition.sound == null) {
            // Load sound if it is played for the first time
            val sound = Gdx.audio.newSound(metaAssetProvider!!.get(soundDefinition.soundName, FileHandle::class.java))
            soundDefinition.sound = sound
        }
        // Play or loop sound
        val soundHandle = MetaSoundHandle(soundDefinition)
        soundHandle.setSoundPosition(listenerPos, soundPos)
        if (listenerPos != null) {
            val mappedVolume = soundHandle.calcVolume(listenerPos, false)
            val mappedPan = soundHandle.calcPan(listenerPos)
            val id = if (soundDefinition.isLooping) soundDefinition.sound.loop(
                mappedVolume,
                1f,
                mappedPan
            ) else soundDefinition.sound.play(mappedVolume, 1f, mappedPan)
            soundHandle.setHandleId(id)
            dynamicHandles.add(soundHandle)
        } else {
            val id = if (soundDefinition.isLooping) soundDefinition.sound.loop(
                volume,
                1f,
                0f
            ) else soundDefinition.sound.play(volume, 1f, 0f)
            soundHandle.setHandleId(id)
        }
        handleList.add(soundHandle)
        return soundHandle
    }

    private fun cleanupHandles(handleList: Array<MetaSoundHandle>) {
        val iterator: MutableIterator<MetaSoundHandle> = handleList.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.isDone || !next.isPlaying) {
                stopSound(next)
                iterator.remove()
            }
        }
    }

    private fun isInAudibleRange(
        sound: MetaSoundDefinition,
        listenerPosition: Vector2,
        soundPosition: Vector2?
    ): Boolean {
        return listenerPosition.dst2(soundPosition) <= sound.soundRange2 || soundInScreen(soundPosition)
    }

    private fun soundInScreen(soundPosition: Vector2?): Boolean {
        helper[soundPosition!!.x, soundPosition.y] = 0f
        val project = uiRenderer!!.getCamera().project(helper)
        return project.x > 0 && project.x < Gdx.graphics.width && project.y > 0 && project.y < Gdx.graphics.height
    }

    fun playSound(path: String): MetaSoundHandle? {
        if (!soundDefinitions.containsKey(path)) {
            soundDefinitions.put(path, MetaSoundDefinition(path))
        }
        return playSound(soundDefinitions.get(path))
    }

    fun playSound(path: String, listenerPosition: Vector2?, soundPosition: Vector2?): MetaSoundHandle? {
        val metaSoundHandle = playSound(path)
        metaSoundHandle?.setSoundPosition(listenerPosition, soundPosition)
        return metaSoundHandle
    }

    fun updateDynamicSounds(listenerPos: Vector2) {
        val iterator: Iterator<MetaSoundHandle> = dynamicHandles.iterator()
        while (iterator.hasNext()) {
            val soundHandle = iterator.next()
            if (soundHandle.isDone || !soundHandle.isPlaying) {
                stopSound(soundHandle)
            } else {
                soundHandle.calcVolAndPan(listenerPos)
            }
        }
    }

    /**
     * Debug-renders all dynamic sound instances
     */
    fun debugRender() {
        shapeRenderer!!.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.projectionMatrix = spriteBatch!!.projectionMatrix
        shapeRenderer.transformMatrix = spriteBatch.transformMatrix
        for (soundHandle in dynamicHandles) {
            soundHandle.debugRender()
        }
        shapeRenderer.end()
    }

    fun stopSound(soundHandle: MetaSoundHandle?) {
        if (soundHandle != null) {
            soundHandle.stop()
            soundHandle.setDone()
            dynamicHandles.removeValue(soundHandle, true)
        }
    }

    fun stopAllSounds() {
        for (soundHandles in playingHandles.values()) {
            for (soundHandle in soundHandles) {
                soundHandle.stop()
                soundHandle.setDone()
            }
            soundHandles.clear()
        }
        dynamicHandles.clear()
    }

    companion object {
        private val helper = Vector3()
    }

    init {
        inject(this)
    }
}