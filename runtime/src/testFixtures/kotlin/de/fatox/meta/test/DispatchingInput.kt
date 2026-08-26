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
 * Dispatch order follows `MetaInput`: an exclusive grab short-circuits everything, then key listeners, then global
 * processors, then screen ones. A harness that reordered those would let a test pass against a build where a modal
 * did not actually own input.
 *
 * Modifier state is real, because `UiControlHelper.activateSelectedActor` declines to fire while ctrl or alt is held
 * and a harness reporting `false` forever would hide that.
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

	private fun removeExclusiveByIdentity(processor: InputProcessor): Boolean {
		for (index in exclusiveProcessors.indices) {
			if (exclusiveProcessors[index] === processor) {
				exclusiveProcessors.removeAt(index)
				return true
			}
		}
		return false
	}

	override var isLeftCtrlDown: Boolean = false
		private set
	override var isRightCtrlDown: Boolean = false
		private set
	override var isLeftShiftDown: Boolean = false
		private set
	override var isRightShiftDown: Boolean = false
		private set

	override fun changeScreen() {
		screenProcessors.clear()
		screenScrollListeners.clear()
		screenKeyListeners.clear()
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
		register(globalKeyListeners, keycode, millisRequired, keyListener)

	override fun removeGlobalKeyListener(keycode: Int, keyListener: KeyListener): Boolean =
		globalKeyListeners[keycode]?.remove(keyListener) ?: false

	override fun addScreenKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener =
		register(screenKeyListeners, keycode, millisRequired, keyListener)

	override fun removeScreenKeyListener(keycode: Int, keyListener: KeyListener): Boolean =
		screenKeyListeners[keycode]?.remove(keyListener) ?: false

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
		globalScrollListeners.remove(scrollListener)

	override fun addScreenScrollListener(scrollListener: ScrollListener): ScrollListener =
		scrollListener.also { screenScrollListeners.add(it) }

	override fun removeScreenScrollListener(scrollListener: ScrollListener): Boolean =
		screenScrollListeners.remove(scrollListener)

	// ── Dispatch ──────────────────────────────────────────────────────────────

	override fun keyDown(keycode: Int): Boolean {
		trackModifier(keycode, down = true)
		exclusiveProcessors.lastOrNull()?.let {
			it.keyDown(keycode)
			return false
		}
		notifyKeyListeners(keycode) { it.onDown() }
		forEachProcessor { it.keyDown(keycode) }
		return false
	}

	override fun keyUp(keycode: Int): Boolean {
		trackModifier(keycode, down = false)
		exclusiveProcessors.lastOrNull()?.let {
			it.keyUp(keycode)
			return false
		}
		notifyKeyListeners(keycode) { it.onUp() }
		forEachProcessor { it.keyUp(keycode) }
		return false
	}

	override fun keyTyped(character: Char): Boolean {
		exclusiveProcessors.lastOrNull()?.let {
			it.keyTyped(character)
			return false
		}
		forEachProcessor { it.keyTyped(character) }
		return false
	}

	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		exclusiveProcessors.lastOrNull()?.let {
			it.touchDown(screenX, screenY, pointer, button)
			return false
		}
		forEachProcessor { it.touchDown(screenX, screenY, pointer, button) }
		return false
	}

	override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		exclusiveProcessors.lastOrNull()?.let {
			it.touchUp(screenX, screenY, pointer, button)
			return false
		}
		forEachProcessor { it.touchUp(screenX, screenY, pointer, button) }
		return false
	}

	override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false

	override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
		forEachProcessor { it.touchDragged(screenX, screenY, pointer) }
		return false
	}

	override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
		forEachProcessor { it.mouseMoved(screenX, screenY) }
		return false
	}

	override fun scrolled(amountX: Float, amountY: Float): Boolean {
		forEachProcessor { it.scrolled(amountX, amountY) }
		return false
	}

	/**
	 * Snapshotted before dispatch, because a listener may register or remove one during it — `UiControlHelper`
	 * disposing mid-press is exactly that — and mutating the list under an index walk drops or repeats a processor.
	 */
	private inline fun forEachProcessor(action: (InputProcessor) -> Unit) {
		val globals = globalProcessors.toTypedArray()
		for (index in globals.indices) action(globals[index])
		val screens = screenProcessors.toTypedArray()
		for (index in screens.indices) action(screens[index])
	}

	private inline fun notifyKeyListeners(keycode: Int, action: (KeyListener) -> Unit) {
		screenKeyListeners[keycode]?.toTypedArray()?.let { for (index in it.indices) action(it[index]) }
		globalKeyListeners[keycode]?.toTypedArray()?.let { for (index in it.indices) action(it[index]) }
	}

	private fun trackModifier(keycode: Int, down: Boolean) {
		when (keycode) {
			Input.Keys.CONTROL_LEFT -> isLeftCtrlDown = down
			Input.Keys.CONTROL_RIGHT -> isRightCtrlDown = down
			Input.Keys.SHIFT_LEFT -> isLeftShiftDown = down
			Input.Keys.SHIFT_RIGHT -> isRightShiftDown = down
		}
	}
}
