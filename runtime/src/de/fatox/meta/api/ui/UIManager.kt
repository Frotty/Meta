@file:Suppress("unused")

package de.fatox.meta.api.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.MenuBar
import de.fatox.meta.api.PosModifier
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaWindow
import kotlin.reflect.KClass

/**
 * Created by Frotty on 20.05.2016.
 */
interface UIManager {
	var posModifier: PosModifier
	fun moveWindow(x: Int, y: Int)
	fun resize(width: Int, height: Int)

	/**
	 * Indicates a screen change. This will remove/modify the elements of the current screen
	 * and load the saved elements
	 *
	 * @param screenIdentifier name of the screen for the json persistence
	 */
	fun changeScreen(screenIdentifier: String)

	fun addTable(table: Table?, growX: Boolean, growY: Boolean)

	/**
	 * @param windowClass The window to show
	 */
	fun <T : Window> showWindow(windowClass: KClass<out T>): T

	fun <T : MetaDialog> showDialog(dialogClass: KClass<out T>): T
	fun setMainMenuBar(menuBar: MenuBar?)
	fun <T : Window> getWindow(windowClass: KClass<out T>): T
	fun closeWindow(window: Window)
	fun updateWindow(window: Window)
	fun bringWindowsToFront()
	fun metaHas(name: String): Boolean
	fun <T> metaGet(name: String, c: Class<T>): T?
	fun metaSave(name: String, windowData: Any)
	val currentlyActiveWindows: Array<Window>
	val nameToClass: MutableMap<String, KClass<out Window>>
	val classToName: MutableMap<KClass<out Window>, String>
	val windowCreators: MutableMap<String, () -> Window>
}

inline fun <reified T: Window> UIManager.register(uniqueName: String = T::class.qualifiedName ?: "", noinline creator: () -> T) {
	require(nameToClass[uniqueName] == null) { "Name already registered: $uniqueName"}

	nameToClass[uniqueName] = T::class
	classToName[T::class] = uniqueName
	windowCreators[uniqueName] = creator
}
inline fun <reified T : MetaWindow> UIManager.getWindow(config: T.() -> Unit = {}): T = getWindow(T::class).apply(config)
inline fun <reified T : MetaWindow> UIManager.showWindow(config: T.() -> Unit = {}): T = showWindow(T::class).apply(config)
inline fun <reified T : MetaDialog> UIManager.showDialog(config: T.() -> Unit = {}): T = showDialog(T::class).apply(config)