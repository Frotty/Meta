package de.fatox.meta.test

import com.badlogic.gdx.InputProcessor
import de.fatox.meta.input.KeyListener
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.input.ScrollListener

/**
 * A [MetaInputProcessor] that accepts registrations and dispatches nothing.
 *
 * Needed because widget construction reaches it transitively and unavoidably: `MetaSelectBox` resolves a
 * [de.fatox.meta.ui.UiControlHelper] eagerly, and that helper's `init` registers a global input processor. So a screen
 * containing a select box cannot be built at all without something answering for this interface.
 *
 * Deliberately *not* the real [de.fatox.meta.input.MetaInput]. That constructor claims `Gdx.input.inputProcessor` and
 * adds a listener to the static `Controllers` registry — process-wide state a layout harness has no business taking,
 * and the same class of leak that made teardown order matter in the first place. Registrations are kept so
 * add/remove pairs behave, and nothing is ever dispatched, because there is no input in a unit test to dispatch.
 *
 * A test that wants to drive real input should build the real processor itself and own its teardown.
 */
internal class LayoutOnlyInput : MetaInputProcessor {

	private val globalProcessors = ArrayList<InputProcessor>()
	private val screenProcessors = ArrayList<InputProcessor>()
	private val globalScrollListeners = ArrayList<ScrollListener>()
	private val screenScrollListeners = ArrayList<ScrollListener>()

	/**
	 * A LIFO stack, matching [de.fatox.meta.input.MetaInput] rather than approximating it.
	 *
	 * A single slot would have been simpler and wrong in a way that matters here: nested grabs are the reason the real
	 * one is a stack, so `push(A); push(B); pop(B)` must leave A owning input, and popping A while B is active must
	 * remove A from underneath it. A stub that clobbers instead of stacking would let a test asserting on
	 * leaked-owner lifecycle pass when the production path would have failed — the harness lying about the thing the
	 * test is checking.
	 *
	 * Identity, not equality, for the same reason `MetaInput` uses `removeValue(processor, true)`: two distinct
	 * processors that happen to compare equal are still two owners.
	 */
	private val exclusiveProcessors = ArrayList<InputProcessor>()

	override var exclusiveProcessor: InputProcessor?
		get() = exclusiveProcessors.lastOrNull()
		set(value) {
			if (value == null) {
				if (exclusiveProcessors.isNotEmpty()) exclusiveProcessors.removeAt(exclusiveProcessors.lastIndex)
			} else {
				pushExclusiveProcessor(value)
			}
		}

	override fun pushExclusiveProcessor(processor: InputProcessor) {
		removeByIdentity(processor) // avoid duplicates; (re)push to the top
		exclusiveProcessors.add(processor)
	}

	override fun popExclusiveProcessor(processor: InputProcessor): Boolean = removeByIdentity(processor)

	override fun clearExclusiveProcessors() {
		exclusiveProcessors.clear()
	}

	private fun removeByIdentity(processor: InputProcessor): Boolean {
		for (i in exclusiveProcessors.indices) {
			if (exclusiveProcessors[i] === processor) {
				exclusiveProcessors.removeAt(i)
				return true
			}
		}
		return false
	}

	override val isLeftCtrlDown: Boolean = false
	override val isRightCtrlDown: Boolean = false
	override val isLeftShiftDown: Boolean = false
	override val isRightShiftDown: Boolean = false

	override fun changeScreen() {
		screenProcessors.clear()
		screenScrollListeners.clear()
	}

	override fun addGlobalInputProcessor(inputProcessor: InputProcessor): InputProcessor =
		inputProcessor.also { globalProcessors.add(it) }

	override fun removeGlobalInputProcessor(inputProcessor: InputProcessor): Boolean =
		globalProcessors.remove(inputProcessor)

	override fun addScreenInputProcessor(inputProcessor: InputProcessor): InputProcessor =
		inputProcessor.also { screenProcessors.add(it) }

	override fun removeScreenInputProcessor(inputProcessor: InputProcessor): Boolean =
		screenProcessors.remove(inputProcessor)

	override fun addGlobalKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener =
		keyListener

	override fun removeGlobalKeyListener(keycode: Int, keyListener: KeyListener): Boolean = true

	override fun addScreenKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener =
		keyListener

	override fun removeScreenKeyListener(keycode: Int, keyListener: KeyListener): Boolean = true

	override fun addGlobalScrollListener(scrollListener: ScrollListener): ScrollListener =
		scrollListener.also { globalScrollListeners.add(it) }

	override fun removeGlobalScrollListener(scrollListener: ScrollListener): Boolean =
		globalScrollListeners.remove(scrollListener)

	override fun addScreenScrollListener(scrollListener: ScrollListener): ScrollListener =
		scrollListener.also { screenScrollListeners.add(it) }

	override fun removeScreenScrollListener(scrollListener: ScrollListener): Boolean =
		screenScrollListeners.remove(scrollListener)

	// InputProcessor: nothing arrives in a unit test, so nothing is handled.
	override fun keyDown(keycode: Int): Boolean = false
	override fun keyUp(keycode: Int): Boolean = false
	override fun keyTyped(character: Char): Boolean = false
	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
	override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
	override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false
	override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
	override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false
	override fun scrolled(amountX: Float, amountY: Float): Boolean = false
}
