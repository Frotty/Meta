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
import kotlin.jvm.Synchronized
import de.fatox.meta.task.TaskListener
import de.fatox.meta.task.MetaTask
import com.badlogic.gdx.Game
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.error.MetaError
import com.badlogic.gdx.InputProcessor
import de.fatox.meta.camera.ArcCamControl
import com.badlogic.gdx.controllers.Controllers
import de.fatox.meta.input.MetaControllerListener
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.PovDirection
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.sound.MetaSoundDefinition
import com.badlogic.gdx.math.MathUtils
import de.fatox.meta.sound.MetaSoundHandle
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.sound.MetaSoundPlayer
import java.lang.NoSuchMethodException
import java.lang.reflect.InvocationTargetException
import de.fatox.meta.assets.MetaData.CacheObj
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
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import de.fatox.meta.graphics.buffer.MultisampleFBO
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import java.nio.IntBuffer
import java.lang.Exception
import com.badlogic.gdx.Application.ApplicationType
import com.badlogic.gdx.audio.Music
import java.nio.ByteOrder
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import java.lang.IllegalStateException
import com.badlogic.gdx.graphics.Pixmap
import java.util.HashMap
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.Qualifier
import de.fatox.meta.injection.MetastasisException
import de.fatox.meta.injection.Metastasis
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashSet
import java.util.HashSet
import de.fatox.meta.injection.Provides

/**
 * Created by Frotty on 09.11.2016.
 */
class MetaMusicPlayer {
    @Inject
    private val metaData: MetaData? = null

    @Inject
    private val assetProvider: AssetProvider? = null
    private var task: Timer.Task? = null
    private val startVolume = 0.01f
    private var musicEnabled = true
    private var currentMusic: Music? = null
    private var nextMusic: Music? = null
    private val allPool = Array<Music?>()
    private val activePool = Array<Music?>()
    private val musicCache = ObjectMap<String, Music?>()
    private val timer = Timer()
    fun start() {
        // Start Timer to update music
        task = timer.scheduleTask(object : Timer.Task() {
            override fun run() {
                updateMusic()
            }
        }, 0f, 0.1f)
    }

    private fun updateMusic() {
        val audioVideoData = metaData!!.get("audioVideoData", MetaAudioVideoData::class.java)
        val volume = audioVideoData!!.masterVolume * audioVideoData.musicVolume
        if (!musicEnabled || volume <= startVolume) {
            if (currentMusic != null) {
                currentMusic!!.volume = 0f
            }
            return
        }
        if (currentMusic == null || !currentMusic!!.isPlaying) {
            if (nextMusic == null) {
                nextFromPool()
            } else {
                startMusic(nextMusic)
            }
        }
        if (currentMusic != null) {
            if (currentMusic!!.volume < startVolume) {
                finishMusic()
            } else {
                fadeInOut(volume)
                if (currentMusic!!.volume > volume) {
                    currentMusic!!.volume = volume
                }
            }
        }
    }

    private fun finishMusic() {
        currentMusic!!.stop()
        // Check if there is a track queued
        if (nextMusic != null) {
            currentMusic = nextMusic
            nextMusic = null
            currentMusic!!.play()
            currentMusic!!.volume = startVolume
        }
    }

    private fun fadeInOut(volume: Float) {
        if (nextMusic != null && currentMusic!!.isPlaying) {
            currentMusic!!.volume = currentMusic!!.volume * 0.4f
        } else if (currentMusic!!.volume >= startVolume && currentMusic!!.volume < volume) {
            currentMusic!!.volume = currentMusic!!.volume * 3f
        }
    }

    fun playMusic(musicPath: String) {
        val music = getMusic(musicPath)
        if (currentMusic == null || !currentMusic!!.isPlaying) {
            startMusic(music)
        } else {
            nextMusic = music
        }
    }

    private fun startMusic(music: Music?) {
        currentMusic = music
        currentMusic!!.play()
        currentMusic!!.volume = startVolume
    }

    private fun getMusic(musicPath: String): Music? {
        if (!musicCache.containsKey(musicPath)) {
            val music = assetProvider!!.get(musicPath, Music::class.java)
            musicCache.put(musicPath, music)
        }
        return musicCache.get(musicPath)
    }

    fun addMusicToPool(musicName: String) {
        val music = getMusic(musicName)
        allPool.add(music)
    }

    fun nextFromPool() {
        if (activePool.size == 0 && allPool.size > 0) {
            activePool.addAll(allPool)
            activePool.shuffle()
        }
        if (activePool.size <= 0) return
        if (currentMusic == null) {
            startMusic(activePool.pop())
        } else {
            nextMusic = activePool.pop()
        }
    }

    fun isMusicEnabled(): Boolean {
        return musicEnabled
    }

    fun setMusicEnabled(musicEnabled: Boolean) {
        this.musicEnabled = musicEnabled
        if (!musicEnabled) {
            nextMusic = null
            currentMusic!!.stop()
            currentMusic = null
        }
    }

    private var vol = 1f
    fun silenceMusic(musicEnabled: Boolean) {
        if (currentMusic != null) {
            if (musicEnabled) {
                currentMusic!!.volume = vol
                if (!task!!.isScheduled) {
                    timer.scheduleTask(task, 0f, 0.1f)
                }
            } else {
                vol = currentMusic!!.volume
                currentMusic!!.volume = 0f
                task!!.cancel()
            }
        }
    }

    val isMusicPlaying: Boolean
        get() = currentMusic != null && currentMusic!!.isPlaying

    init {
        inject(this)
    }
}