package de.fatox.meta.api.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import de.fatox.meta.ScreenConfig
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaWindow
import kotlin.reflect.KClass

class WindowConfig {
	internal val nameToClass: MutableMap<String, KClass<out Window>> = mutableMapOf()
	internal val classToName: MutableMap<KClass<out Window>, String> = mutableMapOf()
	internal val creators: MutableMap<String, () -> Window> = mutableMapOf()
	internal val singletons: MutableMap<String, () -> Window> = mutableMapOf()
	internal val singletonCache: MutableMap<String, Window> = mutableMapOf()

	internal inline fun nameOf(windowClass: KClass<out Window>): String = classToName.getValue(windowClass)

	internal fun <T : Window> create(name: String): T =
		singletonCache[name] as T?
			?: singletons[name]?.invoke()?.also { singletonCache[name] = it } as T?
			?: creators.getValue(name)() as T

	internal fun <T : Window> create(windowClass: KClass<out T>): T = create(nameOf(windowClass))

	@PublishedApi
	internal fun <T : MetaWindow> register(windowClass: KClass<T>, name: String, creator: () -> T) =
		registerInternal(windowClass, name, creators, creator)

	@PublishedApi
	internal fun <T : MetaWindow> registerSingleton(windowClass: KClass<T>, name: String, creator: () -> T) =
		registerInternal(windowClass, name, singletons, creator)

	private inline fun <T : MetaWindow> registerInternal(
		windowClass: KClass<T>,
		name: String,
		map: MutableMap<String, () -> Window>,
		noinline creator: () -> T
	) {
		require(nameToClass[name] == null) { "Name already registered: $name" }

		nameToClass[name] = windowClass
		classToName[windowClass] = name
		map[name] = creator
	}
}

inline fun <reified T : MetaWindow> WindowConfig.register(
	name: String = T::class.qualifiedName ?: "",
	noinline creator: () -> T,
) {
	handleLegacyName<T>(name)
	register(T::class, name, creator)
}

inline fun <reified T : MetaWindow> WindowConfig.registerSingleton(
	name: String = T::class.qualifiedName ?: "",
	noinline creator: () -> T,
) {
	handleLegacyName<T>(name)
	registerSingleton(T::class, name, creator)
}

val logger = MetaLoggerFactory.logger {}

inline fun <reified T : MetaWindow> handleLegacyName(name: String) {
	val gameName: String = MetaInject.inject("gameName")
	Gdx.files.external(".$gameName").child(MetaData.GLOBAL_DATA_FOLDER_NAME).list().forEach { screenId ->
		if (screenId.isDirectory)
			screenId.list().firstOrNull { it.name() == T::class.qualifiedName }?.let { windowId ->
				logger.debug("Found legacy window name: ${windowId.name()}, replacing with $name")
				windowId.moveTo(windowId.sibling(name))
			}
	}
}

/**
 * Created by Frotty on 20.05.2016.
 */
interface UIManager {
	var windowHandler: WindowHandler
	fun moveWindow(x: Int, y: Int)
	fun resize(width: Int, height: Int)

	fun <T : Screen> changeScreen(screenClass: KClass<T>)

	fun <T : Tab> changeTab(tabClass: KClass<T>)

	fun addTable(table: Table?, growX: Boolean, growY: Boolean)

	fun <T : Window> showWindow(windowClass: KClass<out T>): T

	fun hideOtherWindowsAndPreventNew(window: Window)
	fun restoreOtherWindowsAndAllowNew()
	fun <T : MetaDialog> showDialog(dialogClass: KClass<out T>, showBackdrop: Boolean): T
	fun setMainMenuBar(menuBar: MenuBar?)
	fun <T : Window> getWindow(windowClass: KClass<out T>): T
	fun closeWindow(window: Window)
	fun updateWindow(window: Window)
	fun bringWindowsToFront()
	fun metaHas(name: String): Boolean
	fun <T : Any> metaGet(name: String, c: KClass<out T>): T?
	fun metaSave(name: String, windowData: Any)
	fun closeDialog(metaDialog: MetaDialog)

	val currentlyActiveWindows: Array<Window>
	val windowConfig: WindowConfig
	val preventShowWindowObservers: Array<(Boolean) -> Unit>
	val preventShowWindow: Boolean

	val screenConfig: ScreenConfig
}

inline fun <reified T : Any> UIManager.metaGet(name: String): T? = metaGet(name, T::class)

inline fun <reified T : MetaWindow> UIManager.getWindow(config: T.() -> Unit = {}): T =
	getWindow(T::class).apply(config)

inline fun <reified T : MetaWindow> UIManager.showWindow(config: T.() -> Unit = {}): T =
	showWindow(T::class).apply(config)

inline fun <reified T : MetaDialog> UIManager.showDialog(config: T.() -> Unit = {}): T =
	showDialog(T::class, true).apply(config)

inline fun <reified T : MetaDialog> UIManager.showDialog(config: T.() -> Unit = {}, showBackdrop: Boolean): T =
	showDialog(T::class, showBackdrop).apply(config)

inline fun <reified T : Screen> UIManager.changeScreen(): Unit = changeScreen(T::class)

inline fun <reified W : MetaWindow> UIManager.showWindowOnClick(
	actor: Actor,
	button: Int = Input.Buttons.LEFT,
	crossinline config: W.() -> Unit = {},
): Actor {
	if (actor is Disableable) preventShowWindowObservers.add { actor.isDisabled = it }
	actor.addListener(object : ClickListener(button) {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			showWindow(config)
		}
	})
	return actor
}

inline fun <reified D : MetaDialog> UIManager.showDialogOnClick(
	actor: Actor,
	button: Int = Input.Buttons.LEFT,
	crossinline config: D.() -> Unit = {},
): Actor {
	if (actor is Disableable) preventShowWindowObservers.add { actor.isDisabled = it }
	actor.addListener(object : ClickListener(button) {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			showDialog(config)
		}
	})
	return actor
}