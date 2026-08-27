package de.fatox.meta

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.glutils.ShaderProgram.prependFragmentCode
import com.badlogic.gdx.graphics.glutils.ShaderProgram.prependVertexCode
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.GraphicsHandler
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.MonitorHandler
import de.fatox.meta.api.NoGraphicsHandler
import de.fatox.meta.api.NoMonitorHandler
import de.fatox.meta.api.NoSoundHandler
import de.fatox.meta.api.NoWindowHandler
import de.fatox.meta.api.SoundHandler
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.assets.MetaData
import de.fatox.meta.concurrent.MetaJobs
import de.fatox.meta.concurrent.MetaThreads
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
	val gameName: String = canonicalAppStorageName(MetaInject.inject("gameName"))
	val screenFolders = Gdx.files.external(".$gameName").child(MetaData.GLOBAL_DATA_FOLDER_NAME).list()
	for (index in screenFolders.indices) {
		val screenId = screenFolders[index]
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
	private val assetProvider: AssetProvider by lazyInject()

	private var lastChange: Long = 0
	private lateinit var lastScreen: Screen

	private val lastScreenName: String get() = screenConfig.classToName[lastScreen::class]!!

	init {
		Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler)
		// GLSL 120, not 130: macOS only supports GLSL 120 (OpenGL 2.1) or 150+ (3.2 core) — never 130/140 — so a
		// "#version 130" prepend makes EVERY shader fail to compile on macOS. These shaders are legacy style
		// (attribute/varying/gl_FragColor/texture2D), valid in 120. NOTE: MetaDesktopLauncher actually requests
		// GLEmulation.GL30 with context version 3.2 (not GL20/2.1 as previously stated here) - on Windows/Linux
		// LWJGL3 does not force a core profile for that request, so legacy GLSL 120 keeps working there, but on
		// macOS GLFW always forces a strict 3.2 CORE profile, which this legacy syntax is NOT valid under. Mac
		// support requires either requesting a real 2.1 context or porting these shaders to core-profile GLSL 150.
		prependVertexCode = "#version 120\n"
		prependFragmentCode = "#version 120\n"
	}

	abstract fun config()
	abstract fun MetaInject.injection()
	abstract fun ScreenConfig.screens()
	abstract fun WindowConfig.windows()

	open fun iconified(isIconified: Boolean) = Unit
	open fun maximized(isMaximized: Boolean) = Unit
	open fun onFocusLost() = Unit
	open fun onFocusGained() = Unit

	final override fun create() {
		instance = this
		// Claimed before anything else runs, so every later guard has an owner to compare against. This is the
		// render thread: libGDX calls create() on it and calls render() on it forever after.
		MetaThreads.claimMainThread()
		// Before anything else resolves a service. The platform handlers are constructor arguments, but engine code
		// reaches them through the injection graph rather than through `instance`: a static singleton is not
		// resolvable in a unit test, and reaching for one was why MetaAudioVideoData, MultisampleFBO and the sound
		// system had no coverage. Registered first so MetaModule's No*Handler defaults find these already present
		// and leave them alone, and so a game's own `injection()` can still replace one deliberately.
		MetaInject.global {
			singleton<WindowHandler> { windowHandler }
			singleton<MonitorHandler> { monitorHandler }
			singleton<SoundHandler> { soundHandler }
			singleton<GraphicsHandler> { graphicsHandler }
		}
		MetaInject.injection()
		MetaModule.initialize()
		uiManager.windowHandler = windowHandler
		MetaInject.global { singleton("default") { ScreenConfig().apply { screens() } } }
		MetaInject.global { singleton("default") { WindowConfig().apply { windows() } } }
		config()
		MetaAudioVideoState.initialize(metaData.load(audioVideoDataKey) ?: MetaAudioVideoData())

		metaInput.addGlobalKeyListener(Input.Keys.ENTER, 0, object : KeyListener() {
			override fun onEvent() {
				if (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)) {
					Gdx.app.postRunnable {
						MetaAudioVideoState.update(applyDisplay = true) {
							if (!fullscreen) captureWindowedBounds()
							fullscreen = !fullscreen
							if (!fullscreen) borderless = false
						}
					}
				}
			}
		})
		changeScreen(firstScreen)
	}

	/**
	 * Applies finished background work, then renders the frame.
	 *
	 * Here rather than in `UIRenderer.update()` because that is called by each screen and a screen may not call it
	 * at all - a drain point a consumer can forget is not a drain point. Before `super.render()`, so a result that
	 * rebuilds UI is visible in the frame that follows it rather than the one after.
	 *
	 * An override that does not call `super.render()` stops background results being applied; if you need to take
	 * over the frame, call [MetaJobs.drainCompletions] yourself.
	 */
	override fun render() {
		MetaJobs.drainCompletions()
		super.render()
	}

	override fun dispose() {
		// Before the services background work might still be holding: a job that wakes up after its asset provider
		// is gone would fail in a way nobody can act on.
		MetaJobs.shutdown()
		uiManager.dispose()
		assetProvider.dispose()
		MetaThreads.releaseMainThread()
	}

	@Suppress("unused")
	companion object {
		@JvmStatic
		lateinit var instance: Meta
			private set

		/**
		 * The running application, or null when there is none.
		 *
		 * A consuming game extends this class, so in a running game [instance] is always assigned. This exists for
		 * the case where it is not: a unit test, which never boots an application. Engine code should not reach for
		 * either accessor to find a service - platform handlers live in the injection graph precisely so that a
		 * test can supply them. [canChangeScreen] is the one legitimate caller, because "no application" genuinely
		 * means "nothing to throttle".
		 */
		@JvmStatic
		val instanceOrNull: Meta?
			get() = if (::instance.isInitialized) instance else null

		/**
		 * Whether enough time has passed since the last screen change to make another.
		 *
		 * True when there is no [Meta] application: the throttle exists to stop this class swapping screens twice
		 * in a frame, and an application that manages its own screens has nothing here to throttle.
		 */
		@JvmStatic
		fun canChangeScreen(): Boolean {
			val running = instanceOrNull ?: return true
			return TimeUtils.millis() > running.lastChange + 150
		}

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

