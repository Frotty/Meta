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
	/**
	 * The currently active exclusive input owner (top of the exclusive stack), or `null`. While non-null, ALL input
	 * is routed to it and the normal screen/global processors (incl. the scene2d stage) are bypassed.
	 *
	 * Exclusive ownership is a STACK so a temporary grab (e.g. a key-rebind dialog) can be popped to restore the
	 * previous owner instead of clobbering it. Prefer [pushExclusiveProcessor]/[popExclusiveProcessor]; the setter is
	 * kept for convenience (assigning non-null pushes, assigning `null` pops the current top).
	 *
	 * IMPORTANT: every push MUST be paired with a pop on teardown through EVERY exit path. A leaked exclusive
	 * processor swallows all touch input and makes later UI (dialog buttons) silently dead. For dialogs, pop in
	 * [de.fatox.meta.ui.windows.MetaDialog.onHidden] (runs on every close path), not only in your key handler.
	 */
	var exclusiveProcessor: InputProcessor?

	/** Pushes [processor] as the active exclusive input owner. Pair with [popExclusiveProcessor] on teardown. */
	fun pushExclusiveProcessor(processor: InputProcessor)

	/** Removes [processor] from the exclusive stack (wherever it is), restoring the previous owner. Idempotent. */
	fun popExclusiveProcessor(processor: InputProcessor): Boolean

	/** Clears the entire exclusive stack. Use as a hard reset (e.g. before a fresh modal) - prefer disciplined pops. */
	fun clearExclusiveProcessors()
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