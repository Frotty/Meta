package de.fatox.meta.api

import com.badlogic.gdx.InputProcessor
import de.fatox.meta.input.KeyListener
import de.fatox.meta.input.ScrollListener

inline fun MetaInputProcessor.addGlobalKeyListener(
	keycode: Int,
	millisRequired: Long = 0,
	crossinline keyListener: KeyListener.() -> Unit,
): KeyListener {
	return addGlobalKeyListener(keycode, millisRequired, object : KeyListener() {
		override fun onEvent() {
			keyListener()
		}
	})
}

inline fun MetaInputProcessor.addScreenKeyListener(
	keycode: Int,
	millisRequired: Long = 0,
	crossinline keyListener: KeyListener.() -> Unit,
): KeyListener {
	return addScreenKeyListener(keycode, millisRequired, object : KeyListener() {
		override fun onEvent() {
			keyListener()
		}
	})
}

interface MetaInputProcessor : InputProcessor {
	var exclusiveProcessor: InputProcessor?
	val isLeftCtrlDown: Boolean
	val isRightCtrlDown: Boolean
	val isLeftShiftDown: Boolean
	val isRightShiftDown: Boolean

	/**
	 * Clears all the screen's [KeyListener], [InputProcessor] and [ScrollListener].
	 *
	 * Call this function when changing screens.
	 */
	fun changeScreen()

	fun addGlobalInputProcessor(inputProcessor: InputProcessor): InputProcessor
	fun removeGlobalInputProcessor(inputProcessor: InputProcessor): Boolean

	fun addScreenInputProcessor(inputProcessor: InputProcessor): InputProcessor
	fun removeScreenInputProcessor(inputProcessor: InputProcessor): Boolean

	fun addGlobalKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener

	/**
	 * Removes a global [KeyListener] by identity for the given [keycode].
	 * @param keycode Int
	 * @param keyListener KeyListener
	 * @return `true` if the listener got found and removed, `false` otherwise.
	 */
	fun removeGlobalKeyListener(keycode: Int, keyListener: KeyListener): Boolean

	fun addScreenKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener

	/**
	 * Removes a screen [KeyListener] by identity for the given [keycode].
	 * @param keycode Int
	 * @param keyListener KeyListener
	 * @return `true` if the listener got found and removed, `false` otherwise.
	 */
	fun removeScreenKeyListener(keycode: Int, keyListener: KeyListener): Boolean

	fun addGlobalScrollListener(scrollListener: ScrollListener): ScrollListener
	fun removeGlobalScrollListener(scrollListener: ScrollListener): Boolean
	fun addScreenScrollListener(scrollListener: ScrollListener): ScrollListener
	fun removeScreenScrollListener(scrollListener: ScrollListener): Boolean
}