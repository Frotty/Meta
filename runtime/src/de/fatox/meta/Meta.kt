package de.fatox.meta

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.DummyPosModifier
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
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

inline fun <reified T : Screen> ScreenConfig.register(
	name: String = T::class.qualifiedName ?: "",
	noinline creator: () -> T,
) {
	val gameName: String = MetaInject.inject("gameName")
	Gdx.files.external(".$gameName").child(MetaData.GLOBAL_DATA_FOLDER_NAME).list().forEach { screenId ->
		if (screenId.isDirectory && screenId.name().equals(T::class.qualifiedName, ignoreCase = true)) {
			println("Found legacy screen name: ${screenId.name()}, replacing with $name")
			screenId.moveTo(screenId.sibling(name))
		}
	}

	register(T::class, name, creator)
}

abstract class Meta(protected val modifier: PosModifier = DummyPosModifier) : Game() {
	protected val firstScreen: Screen by lazyInject()
	protected val uiManager: UIManager by lazyInject()
	private val screenConfig: ScreenConfig by lazyInject()

	private var lastChange: Long = 0
	private lateinit var lastScreen: Screen

	protected val lastScreenName: String get() = screenConfig.classToName[lastScreen::class]!!

	init {
		Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler)
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
		uiManager.posModifier = modifier
		MetaInject.global { singleton("default") { ScreenConfig().apply { screens() } } }
		MetaInject.global { singleton("default") { WindowConfig().apply { windows() } } }
		config()
		changeScreen(firstScreen)
	}

	interface ScreenManager {
		fun canChangeScreen(): Boolean
		fun newLastScreen()
		fun changeScreen(newScreen: Screen)
	}

	companion object : ScreenManager {
		@JvmStatic
		lateinit var instance: Meta
			private set

		override fun canChangeScreen(): Boolean = TimeUtils.millis() > instance.lastChange + 150

		override fun newLastScreen() {
			changeScreen(instance.screenConfig.screenCreators[instance.lastScreenName]!!())
		}

		override fun changeScreen(newScreen: Screen) {
			if (!canChangeScreen()) return

			instance.lastChange = TimeUtils.millis()
			val oldScreen: Screen? = instance.getScreen()
			if (oldScreen != null && !oldScreen.javaClass.isInstance(newScreen)) {
				instance.lastScreen = oldScreen
			}
			Gdx.app.postRunnable { instance.setScreen(newScreen) }
		}
	}
}
