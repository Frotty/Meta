@file:Suppress("unused")

package de.fatox.meta.api.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import de.fatox.meta.ScreenConfig
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.ui.components.MetaClickListener
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaWindow
import kotlin.reflect.KClass

class WindowConfig {
	internal val nameToClass: MutableMap<String, KClass<out Window>> = mutableMapOf()
	internal val classToName: MutableMap<KClass<out Window>, String> = mutableMapOf()
	internal val creators: MutableMap<String, () -> Window> = mutableMapOf()

	internal fun nameOf(windowClass: KClass<out Window>): String = classToName.getValue(windowClass)

	internal inline fun <T : Window> create(name: String): T = creators.getValue(name)() as T
	internal inline fun <T : Window> create(windowClass: KClass<out T>): T = create(nameOf(windowClass))

	@PublishedApi
	internal fun <T : MetaWindow> register(
		windowClass: KClass<T>,
		name: String,
		creator: () -> T
	) {
		require(nameToClass[name] == null) { "Name already registered: $name" }

		nameToClass[name] = windowClass
		classToName[windowClass] = name
		creators[name] = creator
	}
}

inline fun <reified T : MetaWindow> WindowConfig.register(
	name: String = T::class.qualifiedName ?: "",
	noinline creator: () -> T,
) {
	val gameName: String = MetaInject.inject("gameName")
	Gdx.files.external(".$gameName").child(MetaData.GLOBAL_DATA_FOLDER_NAME).list().forEach { screenId ->
		if (screenId.isDirectory)
			screenId.list().firstOrNull { it.name() == T::class.qualifiedName }?.let { windowId ->
				println("Found legacy window name: ${windowId.name()}, replacing with $name")
				windowId.moveTo(windowId.sibling(name))
			}
	}
	register(T::class, name, creator)
}

/**
 * Created by Frotty on 20.05.2016.
 */
interface UIManager {
	var windowHandler: WindowHandler
	fun moveWindow(x: Int, y: Int)
	fun resize(width: Int, height: Int)

	/**
	 * Indicates a screen change. This will remove/modify the elements of the current screen
	 * and load the saved elements
	 *
	 * @param screenIdentifier name of the screen for the json persistence
	 */
	fun <T : Screen> changeScreen(screenClass: KClass<T>)

	fun <T : Tab> changeTab(tabClass: KClass<T>)

	fun addTable(table: Table?, growX: Boolean, growY: Boolean)

	/**
	 * @param windowClass The window to show
	 */
	fun <T : Window> showWindow(windowClass: KClass<out T>): T

	fun hideOtherWindowsAndPreventNew(window: Window)
	fun restoreOtherWindowsAndAllowNew()
	fun <T : MetaDialog> showDialog(dialogClass: KClass<out T>): T
	fun setMainMenuBar(menuBar: MenuBar?)
	fun <T : Window> getWindow(windowClass: KClass<out T>): T
	fun closeWindow(window: Window)
	fun updateWindow(window: Window)
	fun bringWindowsToFront()
	fun metaHas(name: String): Boolean
	fun <T : Any> metaGet(name: String, c: KClass<out T>): T?
	fun metaSave(name: String, windowData: Any)
	val currentlyActiveWindows: Array<Window>
	val windowConfig: WindowConfig
	val preventShowWindowObservers: Array<(Boolean) -> Unit>
	val preventShowWindow: Boolean

	val screenConfig: ScreenConfig
}

inline fun <reified T : Any> UIManager.metaGet(name: String) = metaGet(name, T::class)

inline fun <reified T : MetaWindow> UIManager.getWindow(config: T.() -> Unit = {}): T =
	getWindow(T::class).apply(config)

inline fun <reified T : MetaWindow> UIManager.showWindow(config: T.() -> Unit = {}): T =
	showWindow(T::class).apply(config)

inline fun <reified T : MetaDialog> UIManager.showDialog(config: T.() -> Unit = {}): T =
	showDialog(T::class).apply(config)

inline fun <reified T : Screen> UIManager.changeScreen() = changeScreen(T::class)

inline fun <reified W : MetaWindow> UIManager.showWindowOnClick(
	actor: Actor,
	button: Int = Input.Buttons.LEFT,
	crossinline config: W.() -> Unit = {},
): Actor {
	if (actor is Disableable) preventShowWindowObservers.add { actor.isDisabled = it }
	actor.addListener(object : MetaClickListener(button) {
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
	actor.addListener(object : MetaClickListener(button) {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			showDialog(config)
		}
	})
	return actor
}