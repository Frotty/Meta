package de.fatox.meta.api.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.MenuBar
import de.fatox.meta.api.PosModifier
import de.fatox.meta.ui.windows.MetaDialog

/**
 * Created by Frotty on 20.05.2016.
 */
interface UIManager {
	var posModifier: PosModifier
	fun moveWindow(width: Int, height: Int)
	fun resize(width: Int, height: Int)
	/**
	 * Indicates a screen change. This will remove/modify the elements of the current screen
	 * and load the saved elements
	 *
	 * @param screenIdentifier name of the screen for the json persitence
	 */
	fun changeScreen(screenIdentifier: String?)

	fun addTable(table: Table?, gx: Boolean, gy: Boolean)
	/**
	 * @param windowClass The window to show
	 */
	fun <T : Window?> showWindow(windowClass: Class<out T>?): T

	fun <T : MetaDialog?> showDialog(dialogClass: Class<out T>?): T
	fun setMainMenuBar(menuBar: MenuBar?)
	fun <T : Window?> getWindow(windowClass: Class<out T>?): T
	fun closeWindow(window: Window?)
	fun updateWindow(window: Window?)
	fun bringWindowsToFront()
	fun metaHas(name: String?): Boolean
	fun <T> metaGet(name: String?, c: Class<T>?): T
	fun metaSave(name: String?, windowData: Any?)
	val currentlyActiveWindows: Array<Window?>?
}