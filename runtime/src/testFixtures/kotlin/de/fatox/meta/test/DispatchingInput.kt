package de.fatox.meta.test

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.input.KeyListener
import de.fatox.meta.input.ScrollListener

/**
 * A [MetaInputProcessor] that delivers what it is given, so a test can press keys at the UI.
 *
 * The sibling of [LayoutOnlyInput], not a replacement for it. That one accepts registrations and dispatches nothing
 * on purpose — it exists so a widget tree can be *built*, and taking `Gdx.input`'s processor slot and the static
 * `Controllers` registry is process-wide state a layout harness has no business claiming. This one exists for the
 * other job: covering behaviour that only happens when input actually arrives.
 *
 * Without it, a consumer could not test the most interesting logic Meta owns. `UiControlHelper` registers its
 * navigation listener through `addGlobalInputProcessor`, so the spatial arrow-key step — `navigate`, `getNextX`,
 * `getNextY`, and the wrap hooks a game hangs off `MetaUiActionHandler` — was unreachable. So was anything built on
 * `addScreenKeyListener`, and anything a `MetaDialog` does behind an exclusive grab.
 *
 * ```
 * MetaHeadlessUi.install(input = { DispatchingInput() })
 * val input: MetaInputProcessor = MetaInject.inject()
 * input.keyDown(Input.Keys.DOWN)
 * input.keyUp(Input.Keys.DOWN)
 * ```
 *
 * ### What it reproduces, and why each part matters
 *
 * Dispatch follows `MetaInput` exactly, including where it is asymmetric:
 *
 * - An exclusive grab receives the event and nothing else does — for **every** event type, pointer motion and scroll
 *   included. A harness that let those through to the background would make a modal test pass against a build where
 *   the modal did not own input.
 * - `touchCancelled` is a `touchUp`, so an interrupted gesture releases drag state rather than vanishing.
 * - Pointer and scroll events **stop at the first processor that consumes them**; key events do not, and run every
 *   processor unconditionally. Getting that backwards either hides a consumed event or invents a second delivery.
 * - Scroll listeners are notified before processors, so a `ScrollListener` registration is not silently inert.
 * - Return values match, since a caller driving the fixture directly can see them.
 *
 * Held-key state is real, and reaches `Gdx.input`. `UiControlHelper.activateSelectedActor` declines to fire while
 * ctrl or alt is held and it asks **`Gdx.input.isKeyPressed`**, not this interface — so tracking modifiers in private
 * fields, as the first version of this fixture did, left ctrl+confirm activating a control that production would have
 * left alone. [MetaHeadlessUi] points `Gdx.input` at [isKeyHeld] for the lifetime of the install.
 *
 * `KeyListener` semantics are the real ones too: `onDown` then `onUp`, with `onEvent` arriving from `onUp` for a
 * listener registered with no hold duration. A harness that called `onEvent` on the press would quietly disagree with
 * production about *which* edge an action lands on, and that distinction has been a real bug.
 */
class DispatchingInput : MetaInputProcessor {

	private val globalProcessors = ArrayList<InputProcessor>()
	private val screenProcessors = ArrayList<InputProcessor>()
	private val globalScrollListeners = ArrayList<ScrollListener>()
	private val screenScrollListeners = ArrayList<ScrollListener>()
	private val globalKeyListeners = HashMap<Int, ArrayList<KeyListener>>()
	private val screenKeyListeners = HashMap<Int, ArrayList<KeyListener>>()

	/** A LIFO stack, matching `MetaInput`. See [LayoutOnlyInput] for why a single slot would be wrong. */
	private val exclusiveProcessors = ArrayList<InputProcessor>()

	/** How many processors are registered globally, so a test can assert a disposal actually unregistered. */
	val globalProcessorCount: Int get() = globalProcessors.size

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
		removeExclusiveByIdentity(processor)
		exclusiveProcessors.add(processor)
	}

	override fun popExclusiveProcessor(processor: InputProcessor): Boolean = removeExclusiveByIdentity(processor)

	override fun clearExclusiveProcessors() {
		exclusiveProcessors.clear()
	}

	private fun removeExclusiveByIdentity(processor: InputProcessor): Boolean =
		removeByIdentity(exclusiveProcessors, processor)

	/**
	 * Identity, never equality — `ArrayList.remove` would drop the first *equal* element instead of the one handed
	 * over. `MetaInput` uses `removeValue(x, true)` for the same reason, and the key-listener API promises it: two
	 * distinct owners that happen to compare equal are still two owners, and removing the wrong one would leave a
	 * registration live while reporting it gone.
	 */
	private fun <T : Any> removeByIdentity(from: ArrayList<T>, value: T): Boolean {
		for (index in from.indices) {
			if (from[index] === value) {
				from.removeAt(index)
				return true
			}
		}
		return false
	}

	private val heldKeys = HashSet<Int>()

	/** Whether [keycode] is currently down. What [MetaHeadlessUi] points `Gdx.input.isKeyPressed` at. */
	fun isKeyHeld(keycode: Int): Boolean = heldKeys.contains(keycode)

	/** Whether anything is down, for `isKeyPressed(Input.Keys.ANY_KEY)`. */
	fun isAnyKeyHeld(): Boolean = heldKeys.isNotEmpty()

	override val isLeftCtrlDown: Boolean get() = isKeyHeld(Input.Keys.CONTROL_LEFT)
	override val isRightCtrlDown: Boolean get() = isKeyHeld(Input.Keys.CONTROL_RIGHT)
	override val isLeftShiftDown: Boolean get() = isKeyHeld(Input.Keys.SHIFT_LEFT)
	override val isRightShiftDown: Boolean get() = isKeyHeld(Input.Keys.SHIFT_RIGHT)

	override fun changeScreen() {
		screenProcessors.clear()
		screenScrollListeners.clear()
		screenKeyListeners.clear()
	}

	override fun addGlobalInputProcessor(inputProcessor: InputProcessor): InputProcessor =
		inputProcessor.also { globalProcessors.add(it) }

	override fun removeGlobalInputProcessor(inputProcessor: InputProcessor): Boolean =
		removeByIdentity(globalProcessors, inputProcessor)

	override fun addScreenInputProcessor(inputProcessor: InputProcessor): InputProcessor =
		inputProcessor.also { screenProcessors.add(it) }

	override fun removeScreenInputProcessor(inputProcessor: InputProcessor): Boolean =
		removeByIdentity(screenProcessors, inputProcessor)

	override fun addGlobalKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener =
		register(globalKeyListeners, keycode, millisRequired, keyListener)

	override fun removeGlobalKeyListener(keycode: Int, keyListener: KeyListener): Boolean =
		globalKeyListeners[keycode]?.let { removeByIdentity(it, keyListener) } ?: false

	override fun addScreenKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener =
		register(screenKeyListeners, keycode, millisRequired, keyListener)

	override fun removeScreenKeyListener(keycode: Int, keyListener: KeyListener): Boolean =
		screenKeyListeners[keycode]?.let { removeByIdentity(it, keyListener) } ?: false

	private fun register(
		into: HashMap<Int, ArrayList<KeyListener>>,
		keycode: Int,
		millisRequired: Long,
		keyListener: KeyListener,
	): KeyListener {
		keyListener.requiredLengthMillis = millisRequired
		into.getOrPut(keycode) { ArrayList() }.add(keyListener)
		return keyListener
	}

	override fun addGlobalScrollListener(scrollListener: ScrollListener): ScrollListener =
		scrollListener.also { globalScrollListeners.add(it) }

	override fun removeGlobalScrollListener(scrollListener: ScrollListener): Boolean =
		removeByIdentity(globalScrollListeners, scrollListener)

	override fun addScreenScrollListener(scrollListener: ScrollListener): ScrollListener =
		scrollListener.also { screenScrollListeners.add(it) }

	override fun removeScreenScrollListener(scrollListener: ScrollListener): Boolean =
		removeByIdentity(screenScrollListeners, scrollListener)

	// ── Dispatch ──────────────────────────────────────────────────────────────

	override fun keyDown(keycode: Int): Boolean {
		heldKeys.add(keycode)
		grab()?.let {
			it.keyDown(keycode)
			return false
		}
		notifyKeyListeners(keycode) { listener -> listener.onDown() }
		// Keys reach every processor: MetaInput does not short-circuit them.
		eachProcessor { it.keyDown(keycode) }
		return false
	}

	override fun keyUp(keycode: Int): Boolean {
		heldKeys.remove(keycode)
		grab()?.let {
			it.keyUp(keycode)
			return false
		}
		notifyKeyListeners(keycode) { listener -> listener.onUp() }
		eachProcessor { it.keyUp(keycode) }
		return false
	}

	override fun keyTyped(character: Char): Boolean {
		grab()?.let {
			it.keyTyped(character)
			return false
		}
		eachProcessor { it.keyTyped(character) }
		return false
	}

	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		grab()?.let {
			it.touchDown(screenX, screenY, pointer, button)
			return true
		}
		untilConsumed { it.touchDown(screenX, screenY, pointer, button) }
		return true
	}

	override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		grab()?.let {
			it.touchUp(screenX, screenY, pointer, button)
			return false
		}
		return untilConsumed { it.touchUp(screenX, screenY, pointer, button) }
	}

	/** A cancelled gesture is a release, exactly as in `MetaInput`, or drag state is never let go of. */
	override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
		touchUp(screenX, screenY, pointer, button)

	override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
		grab()?.let {
			it.touchDragged(screenX, screenY, pointer)
			return false
		}
		untilConsumed { it.touchDragged(screenX, screenY, pointer) }
		return true
	}

	override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
		grab()?.let {
			it.mouseMoved(screenX, screenY)
			return false
		}
		return untilConsumed { it.mouseMoved(screenX, screenY) }
	}

	override fun scrolled(amountX: Float, amountY: Float): Boolean {
		grab()?.let {
			it.scrolled(amountX, amountY)
			return true
		}
		// Before the processors, as in MetaInput: a ScrollListener registration must not be silently inert.
		val globals = globalScrollListeners.toTypedArray()
		for (index in globals.indices) globals[index].onScroll()
		val screens = screenScrollListeners.toTypedArray()
		for (index in screens.indices) screens[index].onScroll()
		untilConsumed { it.scrolled(amountX, amountY) }
		return true
	}

	private fun grab(): InputProcessor? = exclusiveProcessors.lastOrNull()

	/**
	 * Snapshotted before dispatch, because a listener may register or remove one during it — `UiControlHelper`
	 * disposing mid-press is exactly that — and mutating the list under an index walk drops or repeats a processor.
	 */
	private inline fun eachProcessor(action: (InputProcessor) -> Unit) {
		val globals = globalProcessors.toTypedArray()
		for (index in globals.indices) action(globals[index])
		val screens = screenProcessors.toTypedArray()
		for (index in screens.indices) action(screens[index])
	}

	/** Global then screen, stopping at the first processor that returns `true`. Snapshotted for the same reason. */
	private inline fun untilConsumed(action: (InputProcessor) -> Boolean): Boolean {
		val globals = globalProcessors.toTypedArray()
		for (index in globals.indices) if (action(globals[index])) return true
		val screens = screenProcessors.toTypedArray()
		for (index in screens.indices) if (action(screens[index])) return true
		return false
	}

	private inline fun notifyKeyListeners(keycode: Int, action: (KeyListener) -> Unit) {
		screenKeyListeners[keycode]?.toTypedArray()?.let { for (index in it.indices) action(it[index]) }
		globalKeyListeners[keycode]?.toTypedArray()?.let { for (index in it.indices) action(it[index]) }
	}
}
