package de.fatox.meta

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.glutils.ShaderProgram.prependFragmentCode
import com.badlogic.gdx.graphics.glutils.ShaderProgram.prependVertexCode
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.*
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.load
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.input.KeyListener
import kotlin.reflect.KClass

class ScreenConfig {
	internal val nameToClass: MutableMap<String, KClass<out Screen>> = mutableMapOf()
	internal val classToName: MutableMap<KClass<out Screen>, String> = mutableMapOf()
	internal val screenCreators: MutableMap<String, () -> Screen> = mutableMapOf()

	@PublishedApi
	internal fun <T : Screen> register(screenClass: KClass<T>, name: String, creator: () -> T) {
		require(nameToClass[name] == null) { "Name already registered: $name" }

		nameToClass[name] = screenClass
		classToName[screenClass] = name
		screenCreators[name] = creator
	}
}

val logger = MetaLoggerFactory.logger {}

inline fun <reified T : Screen> ScreenConfig.register(
	name: String = T::class.qualifiedName ?: "",
	noinline creator: () -> T,
) {
	val gameName: String = MetaInject.inject("gameName")
	Gdx.files.external(".$gameName").child(MetaData.GLOBAL_DATA_FOLDER_NAME).list().forEach { screenId ->
		if (screenId.isDirectory && screenId.name().equals(T::class.qualifiedName, ignoreCase = true)) {
			logger.debug("Found legacy screen name: ${screenId.name()}, replacing with $name")
			screenId.moveTo(screenId.sibling(name))
		}
	}

	register(T::class, name, creator)
}

abstract class Meta(
	val windowHandler: WindowHandler = NoWindowHandler,
	val monitorHandler: MonitorHandler = NoMonitorHandler,
	val soundHandler: SoundHandler = NoSoundHandler,
	val graphicsHandler: GraphicsHandler = NoGraphicsHandler,
) : Game() {
	protected val firstScreen: Screen by lazyInject()
	protected val uiManager: UIManager by lazyInject()
	private val screenConfig: ScreenConfig by lazyInject()
	private val metaInput: MetaInputProcessor by lazyInject()
	private val metaData: MetaData by lazyInject()

	private var lastChange: Long = 0
	private lateinit var lastScreen: Screen

	private val lastScreenName: String get() = screenConfig.classToName[lastScreen::class]!!

	init {
		Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler)
		if (Gdx.app.type == Application.ApplicationType.Desktop) {
			prependVertexCode = "#version 130\n"
			prependFragmentCode = "#version 130\n"
		} else {
			prependVertexCode = "#version 300 es\n"
			prependFragmentCode = "#version 300 es\n"
		}
	}

	abstract fun config()
	abstract fun MetaInject.injection()
	abstract fun ScreenConfig.screens()
	abstract fun WindowConfig.windows()

	open fun iconified(isIconified: Boolean): Unit = Unit
	open fun maximized(isMaximized: Boolean): Unit = Unit
	open fun onFocusLost(): Unit = Unit
	open fun onFocusGained(): Unit = Unit

	final override fun create() {
		instance = this
		MetaInject.injection()
		MetaModule
		uiManager.windowHandler = windowHandler
		MetaInject.global { singleton("default") { ScreenConfig().apply { screens() } } }
		MetaInject.global { singleton("default") { WindowConfig().apply { windows() } } }
		config()

		metaInput.addGlobalKeyListener(Input.Keys.ENTER, 0, object : KeyListener() {
			override fun onEvent() {
				if (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)) {
					Gdx.app.postRunnable {
						val audioVideoData = metaData.load(audioVideoDataKey)
						audioVideoData?.let {
							audioVideoData.fullscreen = !audioVideoData.fullscreen
							metaData.save(audioVideoDataKey, audioVideoData)
							audioVideoData.apply()
						}
					}
				}
			}
		})
		changeScreen(firstScreen)

	}

	@Suppress("unused")
	companion object {
		@JvmStatic
		lateinit var instance: Meta
			private set

		@JvmStatic
		fun canChangeScreen(): Boolean = TimeUtils.millis() > instance.lastChange + 150

		@JvmStatic
		fun newLastScreen() {
			changeScreen(instance.screenConfig.screenCreators[instance.lastScreenName]!!())
		}

		@JvmStatic
		fun changeScreen(newScreen: Screen) {
			if (!canChangeScreen()) return

			instance.lastChange = TimeUtils.millis()
			val oldScreen: Screen? = instance.getScreen()
			if (oldScreen != null && oldScreen::class != newScreen::class) {
				instance.lastScreen = oldScreen
			}
			Gdx.app.postRunnable { instance.setScreen(newScreen) }
		}

		@JvmStatic
		fun isTypeOfLastScreen(type: KClass<out Screen>): Boolean = instance.lastScreen::class == type
	}
}
