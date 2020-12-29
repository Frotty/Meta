package de.fatox.meta.ui

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
import de.fatox.meta.ui.windows.MetaDialog.DialogListener
import de.fatox.meta.ui.components.MetaClickListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
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
import com.kotcrab.vis.ui.util.InputValidator
import de.fatox.meta.error.MetaErrorHandler
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
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.reflect.ReflectionException
import com.kotcrab.vis.ui.widget.*
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.Qualifier
import de.fatox.meta.injection.MetastasisException
import de.fatox.meta.injection.Metastasis
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashSet
import java.util.HashSet
import de.fatox.meta.injection.Provides
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Created by Frotty on 20.05.2016.
 */
@Singleton
class MetaUiManager : UIManager {
    @Inject
    private val uiRenderer: UIRenderer? = null

    @Inject
    private val metaData: MetaData? = null

    @Inject
    private val metaInput: MetaInput? = null
    private val displayedWindows = Array<Window?>()
    private val cachedWindows = Array<Window?>()
    private var mainMenuBar: MenuBar? = null
    private val contentTable = Table()
    private var currentScreenId: String? = "(none)"
    override var posModifier: PosModifier = DummyPosModifier()
    override fun moveWindow(x: Int, y: Int) {
        posModifier.modify(x, y)
    }

    override fun resize(width: Int, height: Int) {
        uiRenderer!!.resize(width, height)
    }

    override fun changeScreen(screenIdentifier: String?) {
        currentScreenId = screenIdentifier
        metaInput!!.changeScreen()

        // Close or move currently shown windows
        for (window in displayedWindows) {
            val name = window!!.javaClass.name
            if (metaHas(name)) {
                val metaWindowData = metaGet(name, MetaWindowData::class.java)
                if (metaWindowData.displayed) {
                    // There exists saved window metadata
                    metaWindowData.set(window)
                } else {
                    cacheWindow(window, true)
                }
            } else {
                cacheWindow(window, true)
            }
        }
        displayedWindows.removeAll(cachedWindows, true)
        contentTable.remove()
        contentTable.clear()
        if (mainMenuBar != null) {
            contentTable.row().height(26f)
            contentTable.add(mainMenuBar!!.table).growX().top()
            mainMenuBar!!.table.toFront()
        }
        uiRenderer!!.addActor(contentTable)
        restoreWindows()
    }

    private fun restoreWindows() {
        val list = metaData!!.getCachedHandle(currentScreenId).list()
        outer@ for (fh in list) {
            if (fh.name().endsWith("Window")) {
                try {
                    val windowclass = ClassReflection.forName(fh.name())
                    val metaWindowData = metaGet(windowclass.name, MetaWindowData::class.java)
                    for (displayedWindow in displayedWindows) {
                        if (displayedWindow!!.javaClass == windowclass) {
                            if (!metaWindowData.displayed) {
                                cacheWindow(displayedWindow, true)
                            }
                            continue@outer
                        }
                    }
                    if (metaWindowData.displayed) {
                        metaWindowData.set(showWindow<Window>(windowclass))
                    }
                } catch (e: ReflectionException) {
                    fh.delete()
                    e.printStackTrace()
                }
            }
        }
    }

    override fun addTable(table: Table?, growX: Boolean, growY: Boolean) {
        contentTable.row()
        val add = contentTable.add(table)
        if (growX) add.growX()
        if (growY) add.growY()
        contentTable.invalidate()
    }

    /**
     * Shows an instance of the given class on the current screen.
     * If metadata exists for the window, it will be loaded.
     *
     * @param windowClass The window to show
     */
    override fun <T : Window?> showWindow(windowClass: Class<out T>?): T {
        log.debug("show window: " + windowClass!!.name)
        val window: T = displayWindow(windowClass)
        window!!.isVisible = true
        if (metaHas(windowClass.name)) {
            // There exists metadata for this window.
            val windowData = metaGet(windowClass.name, MetaWindowData::class.java)
            windowData.set(window)
            if (!windowData.displayed) {
                windowData.displayed = true
                metaSave(windowClass.name, windowData)
            }
        } else {
            // First time the window has been shown on this screen
            metaSave(windowClass.name, MetaWindowData(window))
        }
        return window
    }

    override fun <T : MetaDialog?> showDialog(dialogClass: Class<out T>?): T {
        log.debug("show dialog: " + dialogClass!!.name)
        // Dialogs are just Window subtypes so we show it as usual
        val dialog = showWindow(dialogClass)
        dialog!!.show()
        return dialog
    }

    override fun setMainMenuBar(menuBar: MenuBar?) {
        if (menuBar != null) {
            contentTable.row().height(26f)
            contentTable.add(menuBar.table).growX().top()
        } else if (mainMenuBar != null) {
            contentTable.removeActor(mainMenuBar!!.table)
        }
        mainMenuBar = menuBar
    }

    override fun <T : Window?> getWindow(windowClass: Class<out T>?): T {
        // TODO avoid NPE
        return getDisplayedClass(windowClass)
    }

    override fun closeWindow(window: Window) {
        val displayedWindow = getDisplayedInstance(window)
        if (displayedWindow != null) {
            displayedWindows.removeValue(window, true)
            val metaWindowData = metaGet(window.javaClass.name, MetaWindowData::class.java)
            if (metaWindowData != null) {
                metaWindowData.displayed = false
                metaSave(displayedWindow.javaClass.name, metaWindowData)
            }
            cacheWindow(window, false)
        }
    }

    override fun updateWindow(window: Window) {
        val name = window.javaClass.name
        if (metaHas(name)) {
            val metaWindowData = metaGet(name, MetaWindowData::class.java)
            metaWindowData.setFrom(window)
            metaSave(name, metaWindowData)
        }
    }

    override fun bringWindowsToFront() {
        for (window in displayedWindows) {
            window!!.toFront()
        }
        mainMenuBar!!.table.toFront()
    }

    fun <T : Window?> displayWindow(windowClass: Class<out T>?): T? {
        // Check if this window is a singleton. If it is and it is displayed, return displayed instance
        var theWindow = checkSingleton(windowClass)
        if (theWindow != null) {
            log.debug("singleton already displaying")
            return theWindow
        }
        // Check for a cached instance
        for (cachedWindow in cachedWindows) {
            if (cachedWindow!!.javaClass == windowClass) {
                log.debug("found cached")
                theWindow = cachedWindow as T?
                break
            }
        }
        // If there was no cached instance we create a new one
        if (theWindow != null) {
            cachedWindows.removeValue(theWindow, true)
        } else {
            try {
                log.debug("try instance")
                theWindow = windowClass!!.newInstance()
            } catch (e: InstantiationException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        uiRenderer!!.addActor(theWindow)
        displayedWindows.add(theWindow)
        return theWindow
    }

    private fun <T : Window?> checkSingleton(windowClass: Class<out T>?): T? {
        if (windowClass!!.isAnnotationPresent(Singleton::class.java)) {
            val displayedWindow: T = getDisplayedClass(windowClass)
            if (displayedWindow != null) {
                return displayedWindow
            }
        }
        return null
    }

    private fun <T : Window?> getDisplayedClass(windowClass: Class<out Window>?): T? {
        for (displayedWindow in displayedWindows) {
            if (displayedWindow!!.javaClass == windowClass) {
                return displayedWindow as T?
            }
        }
        return null
    }

    private fun getDisplayedInstance(window: Window): Window? {
        for (displayedWindow in displayedWindows) {
            if (displayedWindow === window) {
                return displayedWindow
            }
        }
        return null
    }

    private fun cacheWindow(window: Window?, forceClose: Boolean) {
        cachedWindows.add(window)
        window!!.isVisible = false
        if (forceClose) {
            window.remove()
        }
    }

    override fun metaHas(name: String?): Boolean {
        return metaData!!.has(currentScreenId + File.separator + name)
    }

    override fun <T> metaGet(name: String?, c: Class<T>?): T {
        return metaData!![currentScreenId + File.separator + name, c]
    }

    override fun metaSave(name: String?, windowData: Any?) {
        val id = currentScreenId + File.separator + name
        if (TimeUtils.timeSinceMillis(metaData!!.getCachedHandle(id).lastModified()) > 200) {
            metaData.save(id, windowData)
        }
    }

    private val copy = Array<Window?>()
    override val currentlyActiveWindows: Array<Window?>
        get() {
            copy.clear()
            copy.addAll(displayedWindows)
            return copy
        }

    companion object {
        private val log = LoggerFactory.getLogger(MetaUiManager::class.java)
    }

    init {
        inject(this)
        contentTable.top().left()
        contentTable.setPosition(0f, 0f)
        contentTable.setFillParent(true)
        uiRenderer!!.addActor(contentTable)
    }
}