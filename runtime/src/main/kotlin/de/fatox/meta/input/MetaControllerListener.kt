package de.fatox.meta.input

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.extensions.forEachEntryReentrant
import de.fatox.meta.api.extensions.forEachIntReentrant
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.absoluteValue

private val log = MetaLoggerFactory.logger {}

object MetaControllerListener : ControllerListener {
	/**
	 * Where synthesized key events go. The interface, not [MetaInput]: this only ever calls `keyDown`/`keyUp`, both
	 * of which the interface declares, and depending on the concrete class made the translation untestable from
	 * outside - there was no way to observe what a button press emitted without standing up the real processor.
	 */
	var metaInput: MetaInputProcessor? = null
	private val uiBindings: MetaUiInputBindings by lazyInject()
	private var currentHorDownKey = -1
	private var currentVertDownKey = -1

	/** Scratch for the paths that release several keys at once and must emit each at most once. */
	private val scratchKeys = IntSet()
	var deadzone = 0.39f

	private const val NO_KEY = -1

	/**
	 * While set, raw button presses are reported here and are **not** translated into canonical UI keys.
	 *
	 * This is what a rebinding screen needs, and it needs both halves. It needs the raw code, because that is the
	 * thing being bound. And it needs the translation suppressed, because otherwise pressing the pad's A button
	 * while a rebind dialog is capturing a *keyboard* key emits ENTER into the exclusive processor, and the dialog
	 * dutifully binds ENTER -- a button press captured as a keystroke.
	 *
	 * Stick pushes are suppressed with it, for the same reason: a nudged stick would otherwise emit arrow keys into
	 * the capture. Held keys are released first, so nothing is left stuck down across the handover.
	 *
	 * Set through [captureButtons] and released through [releaseCapture], which pops by owner the way
	 * `MetaInputProcessor`'s exclusive stack does -- a capture nested on top of another must not bury it.
	 */
	private val captureTargets = Array<(Controller, Int) -> Unit>()

	/** The capture currently in force: the newest one still registered. */
	private val captureTarget: ((Controller, Int) -> Unit)?
		get() = if (captureTargets.isEmpty) null else captureTargets.peek()

	/**
	 * Buttons whose press a capture consumed, so their release is consumed too - **including after the capture has
	 * ended**.
	 *
	 * Doubles as the "not translating" marker that [anyButtonHolds] subtracts from [heldRawButtons]: every entry got
	 * here because its press did not become a canonical key-down, either because a capture took it or because a
	 * starting capture took the key away underneath it.
	 *
	 * A rebind dialog closes on the press, and closing releases the capture, so the physical release lands after
	 * teardown with translation switched back on. Without this it becomes a canonical key-up for a key that was never
	 * pressed down, and `UiControlHelper` reads a CONFIRM key-up as an activation - so letting go of the button you
	 * just bound presses whatever is focused behind the dialog that closed.
	 *
	 * Keyed by device as well as code. Clearing every pad's entries when any one of them disconnects would defeat
	 * the guard in the case it exists for: pad A captures and closes the dialog, pad B is unplugged before A's
	 * release, and A's release is then translated after all.
	 */
	private val capturedDownButtons = ObjectMap<Controller, IntSet>()

	/**
	 * Raw button codes currently held, per device.
	 *
	 * Kept unconditionally, because the interesting moment is the one *before* a capture exists: a capture starting
	 * needs to know which releases are still outstanding, and [downButtonKeys] cannot say - it holds canonical keys,
	 * not the raw codes a release arrives with.
	 */
	private val heldRawButtons = ObjectMap<Controller, IntSet>()

	/** Whether raw button capture is active. */
	val capturing: Boolean get() = captureTarget != null

	/**
	 * Routes raw button presses to [target] until [releaseCapture] is called with the same target.
	 *
	 * Releases whatever keys the pad is currently holding down first: those were emitted as canonical keys, and the
	 * matching key-ups will never arrive once translation is off.
	 */
	fun captureButtons(target: (Controller, Int) -> Unit) {
		releaseAxisKeys()
		// Every device's, not one's: a capture suppresses translation for all of them.
		releaseAllButtonKeys()
		// A release is still owed for anything held right now: its press was translated and its release will not be,
		// so without this the button the player happened to be holding when the dialog opened emits an unmatched
		// canonical key-up once the capture ends - and for a CONFIRM binding that presses whatever is focused behind.
		heldRawButtons.forEachEntryReentrant { controller, held -> capturedPressesOf(controller).addAll(held) }
		captureTargets.removeValue(target, true)
		captureTargets.add(target)
	}

	/**
	 * Stops routing to [target]. A no-op if some other capture has since taken over, so a dialog closing late
	 * cannot switch off a capture it does not own.
	 */
	/**
	 * Drops every capture, whoever owns them.
	 *
	 * The counterpart to [MetaInputProcessor.clearExclusiveProcessors]: a recovery hatch for a teardown that cannot
	 * name the owners, not a substitute for [releaseCapture]. Leaving a capture registered makes the pad look dead,
	 * because translation stays off.
	 */
	fun clearCaptures() {
		captureTargets.clear()
	}

	fun releaseCapture(target: (Controller, Int) -> Unit) {
		// By identity and from anywhere in the stack, the way `popExclusiveProcessor` pops by owner: releasing the
		// top restores the one beneath it, and releasing a buried one leaves the top in force. A single slot lost
		// the outer capture the moment an inner one let go, which is not the lifetime this API advertises.
		captureTargets.removeValue(target, true)
	}

	override fun connected(controller: Controller) {
		log.debug { "Controller connected." }
	}

	override fun disconnected(controller: Controller) {
		log.debug { "Controller disconnected." }
		releaseAxisKeys()
		// No releases are coming for a pad that is gone. Only its own entries: another pad may still be mid-press.
		capturedDownButtons.remove(controller)
		// Removed first, so "is anything still holding this key" does not count the pad that just left.
		val orphaned = heldRawButtons.remove(controller)
		if (orphaned != null) releaseKeysOf(controller, orphaned)
	}

	override fun buttonDown(controller: Controller, buttonCode: Int): Boolean {
		val key = keyFor(controller, buttonCode)
		// Asked before this press is recorded, so the question is whether anything *else* was already holding it.
		val alreadyDown = key != NO_KEY && anyButtonHolds(key)
		heldOf(controller).add(buttonCode)
		captureTarget?.let {
			// Recorded before the callback, which usually closes the dialog and releases the capture synchronously.
			capturedPressesOf(controller).add(buttonCode)
			it(controller, buttonCode)
			return true
		}
		if (key == NO_KEY) return false
		if (!alreadyDown) emitKeyDown(key)
		return true
	}

	override fun buttonUp(controller: Controller, buttonCode: Int): Boolean {
		// Consumed rather than translated: the matching down was captured, so emitting a key-up for a key that was
		// never pressed would look like a release of whatever that button normally means. Checked before the capture
		// itself, because the capture is usually already gone by the time the finger comes off the button.
		heldRawButtons.get(controller)?.remove(buttonCode)
		if (capturedDownButtons.get(controller)?.remove(buttonCode) == true) return true
		if (captureTarget != null) return true
		val key = keyFor(controller, buttonCode)
		if (key == NO_KEY) return false
		// The raw button is already out of `heldRawButtons`, so this asks about everything still down.
		if (!anyButtonHolds(key)) emitKeyUp(key)
		return true
	}

	override fun axisMoved(controller: Controller, axisCode: Int, value: Float): Boolean {
		if (captureTarget != null) return false
		if (!uiBindings.axisNavigationEnabled) return false
		return checkVert(controller) || checkHor(controller)
	}

	private fun checkVert(controller: Controller): Boolean {
		val axisValue = controller.getAxis(uiBindings.verticalAxis)
		val upKey = uiBindings.canonicalKeyFor(MetaUiAction.NAVIGATE_UP)
		val downKey = uiBindings.canonicalKeyFor(MetaUiAction.NAVIGATE_DOWN)
		if (currentVertDownKey != upKey && axisValue < -deadzone) {
			emitKeyUp(currentVertDownKey)
			currentVertDownKey = upKey
			emitKeyDown(currentVertDownKey)
			return true
		} else if (currentVertDownKey == upKey && axisValue > -deadzone) {
			emitKeyUp(currentVertDownKey)
			currentVertDownKey = -1
			return true
		}
		if (currentVertDownKey != downKey && axisValue > deadzone) {
			emitKeyUp(currentVertDownKey)
			currentVertDownKey = downKey
			emitKeyDown(currentVertDownKey)
			return true
		} else if (currentVertDownKey == downKey && axisValue < deadzone) {
			emitKeyUp(currentVertDownKey)
			currentVertDownKey = -1
			return true
		}
		return false
	}

	private fun checkHor(controller: Controller): Boolean {
		val axisValue = controller.getAxis(uiBindings.horizontalAxis)
		val leftKey = uiBindings.canonicalKeyFor(MetaUiAction.NAVIGATE_LEFT)
		val rightKey = uiBindings.canonicalKeyFor(MetaUiAction.NAVIGATE_RIGHT)
		if (currentHorDownKey != leftKey && axisValue < -deadzone) {
			emitKeyUp(currentHorDownKey)
			currentHorDownKey = leftKey
			emitKeyDown(currentHorDownKey)
			return true
		} else if (currentHorDownKey == leftKey && axisValue > -deadzone) {
			emitKeyUp(currentHorDownKey)
			currentHorDownKey = -1
			return true
		}
		if (currentHorDownKey != rightKey && axisValue > deadzone) {
			emitKeyUp(currentHorDownKey)
			currentHorDownKey = rightKey
			emitKeyDown(currentHorDownKey)
			return true
		} else if (currentHorDownKey == rightKey && axisValue < deadzone) {
			emitKeyUp(currentHorDownKey)
			currentHorDownKey = -1
			return true
		}
		return false
	}

	private fun releaseAxisKeys() {
		emitKeyUp(currentHorDownKey)
		emitKeyUp(currentVertDownKey)
		currentHorDownKey = -1
		currentVertDownKey = -1
	}

	/** Releases the keys [orphaned] buttons were holding, leaving down any that something else still holds. */
	private fun releaseKeysOf(controller: Controller, orphaned: IntSet) {
		scratchKeys.clear()
		orphaned.forEachIntReentrant { code ->
			val key = keyFor(controller, code)
			if (key != NO_KEY) scratchKeys.add(key)
		}
		scratchKeys.forEachIntReentrant { key -> if (!anyButtonHolds(key)) emitKeyUp(key) }
	}

	/**
	 * Releases every key any device is holding, once each. Used when a capture takes translation from all of them.
	 *
	 * `heldRawButtons` is deliberately left intact: the caller reads it next to work out which releases are still
	 * owed, and those are the same buttons.
	 */
	private fun releaseAllButtonKeys() {
		scratchKeys.clear()
		heldRawButtons.forEachEntryReentrant { controller, buttons ->
			buttons.forEachIntReentrant { code ->
				val key = keyFor(controller, code)
				if (key != NO_KEY) scratchKeys.add(key)
			}
		}
		scratchKeys.forEachIntReentrant(::emitKeyUp)
	}

	/**
	 * Whether any button still held, on any device, is bound to [key].
	 *
	 * Derived from the raw buttons rather than tracked as a set of keys, because a key is not held once - two buttons
	 * on one pad can both be bound to it, as A and START both are to CONFIRM by default. A set of keys cannot tell
	 * "one of the two was released" from "the last one was", so it let go of CONFIRM while the other button was still
	 * down and then let go of it again on the real release.
	 *
	 * Iterating allocates an iterator, which the helper's contract rules out for per-frame paths. This is not one: it
	 * runs on a button edge, a handful of times a second at human speed, over as many entries as there are pads.
	 *
	 * ### It merges devices on purpose
	 *
	 * The question is "is *anything* holding this key", not "is this pad holding it". That is deliberate and it is
	 * the existing model: there is one [de.fatox.meta.ui.UiControlHelper] with one focused actor, so there is one
	 * cursor for every player to share. Two pads pressing confirm on a shared menu is one confirm.
	 *
	 * What it is not is per-player UI navigation. Nothing here can give two players a cursor each, and for a
	 * keyboard there would be nothing to key it by anyway - couch co-op on one keyboard is two key sets on one
	 * device, not two devices. A game that wants separate cursors needs its own input path, the way a game already
	 * reads per-player gameplay bindings itself.
	 */
	private fun anyButtonHolds(key: Int): Boolean {
		var held = false
		heldRawButtons.forEachEntryReentrant { controller, buttons ->
			// A captured press is physically down and is not translating, which is not the same thing. Counting it
			// would report the key as already held: a second button bound to the same action would have its
			// legitimate press suppressed, and its release would then emit a key-up with no down to match it.
			val notTranslating = capturedDownButtons.get(controller)
			buttons.forEachIntReentrant { code ->
				if (!held && notTranslating?.contains(code) != true && keyFor(controller, code) == key) held = true
			}
		}
		return held
	}

	/** The canonical key [buttonCode] drives on [controller], or [NO_KEY] if it is not bound to a UI action. */
	private fun keyFor(controller: Controller, buttonCode: Int): Int {
		val action = uiBindings.actionForButton(controller, buttonCode) ?: return NO_KEY
		return uiBindings.canonicalKeyFor(action)
	}

	/** One [IntSet] per device, created on that device's first captured press. Never a per-frame path. */
	private fun capturedPressesOf(controller: Controller): IntSet {
		capturedDownButtons.get(controller)?.let { return it }
		return IntSet().also { capturedDownButtons.put(controller, it) }
	}

	private fun heldOf(controller: Controller): IntSet {
		heldRawButtons.get(controller)?.let { return it }
		return IntSet().also { heldRawButtons.put(controller, it) }
	}

	private fun emitKeyDown(keycode: Int) {
		if (keycode != -1) metaInput?.keyDown(keycode)
	}

	private fun emitKeyUp(keycode: Int) {
		if (keycode != -1) metaInput?.keyUp(keycode)
	}
}
