package de.fatox.meta.input

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.forEachEntryReentrant
import de.fatox.meta.api.extensions.forEachIntReentrant
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

private val log = MetaLoggerFactory.logger {}

/**
 * Translates controller input into Meta UI actions and assigns each controller to one local player.
 *
 * Controllers are player one until [assignPlayer] says otherwise, preserving the original shared-menu behaviour.
 * Player one still emits ordinary canonical arrow/enter/escape keys. Later players emit private player-tagged keys
 * through the same [MetaInputProcessor], so only their own
 * [de.fatox.meta.ui.UiControlHelper] consumes the action.
 */
object MetaControllerListener : ControllerListener {
	/** Where translated controller events go. */
	var metaInput: MetaInputProcessor? = null

	private val uiBindings: MetaUiInputBindings by lazyInject()
	private val uiProfiles: MetaUiInputProfiles by lazyInject()
	private val playerAssignments = ObjectMap<Controller, MetaPlayer>()
	private val axisStates = ObjectMap<Controller, AxisState>()
	private val heldRawButtons = ObjectMap<Controller, IntSet>()
	private val suppressedDownButtons = ObjectMap<Controller, IntSet>()
	private val captureTargets = Array<ButtonCapture>()
	private val scratchKeys = IntSet()
	private val scratchControllers = Array<Controller>()

	var deadzone = 0.39f

	private const val NO_KEY = -1

	/** Assigns [controller] to [player]. Unassigned controllers belong to [MetaPlayer.ONE]. */
	fun assignPlayer(controller: Controller, player: MetaPlayer) {
		val previous = playerOf(controller)
		if (previous == player) return
		suspendControllerInput(controller)
		if (player == MetaPlayer.ONE) playerAssignments.remove(controller) else playerAssignments.put(controller, player)
	}

	/** Returns the explicit assignment, or [MetaPlayer.ONE] for an unassigned controller. */
	fun playerOf(controller: Controller): MetaPlayer = playerAssignments.get(controller) ?: MetaPlayer.ONE

	/** Restores [controller] to the backwards-compatible shared player-one cursor. */
	fun clearPlayerAssignment(controller: Controller) = assignPlayer(controller, MetaPlayer.ONE)

	/** Restores every controller assignment to player one, releasing translated input first. */
	fun clearPlayerAssignments() {
		scratchControllers.clear()
		playerAssignments.forEachEntryReentrant { controller, _ -> scratchControllers.add(controller) }
		for (index in 0 until scratchControllers.size) clearPlayerAssignment(scratchControllers[index])
		scratchControllers.clear()
	}

	/** True while any global or player-scoped raw-button capture is registered. */
	val capturing: Boolean get() = captureTargets.notEmpty()

	/** Whether [player]'s controllers are captured, including by a legacy global capture. */
	fun isCapturing(player: MetaPlayer): Boolean {
		for (index in captureTargets.size - 1 downTo 0) {
			val capturePlayer = captureTargets[index].player
			if (capturePlayer == null || capturePlayer == player) return true
		}
		return false
	}

	/** Captures raw buttons from every controller. This preserves the original rebind-dialog behaviour. */
	fun captureButtons(target: (Controller, Int) -> Unit) = registerCapture(null, target)

	/** Captures raw buttons only from controllers assigned to [player]. Other players keep navigating. */
	fun captureButtons(player: MetaPlayer, target: (Controller, Int) -> Unit) = registerCapture(player, target)

	private fun registerCapture(player: MetaPlayer?, target: (Controller, Int) -> Unit) {
		suspendPlayerInput(player)
		removeCapture(player, target)
		captureTargets.add(ButtonCapture(player, target))
	}

	/** Drops every capture. Outstanding physical releases remain suppressed. */
	fun clearCaptures() {
		captureTargets.clear()
	}

	/** Releases the matching legacy global capture. */
	fun releaseCapture(target: (Controller, Int) -> Unit) {
		removeCapture(null, target)
	}

	/** Releases the matching capture for [player], leaving other players' captures intact. */
	fun releaseCapture(player: MetaPlayer, target: (Controller, Int) -> Unit) {
		removeCapture(player, target)
	}

	private fun removeCapture(player: MetaPlayer?, target: (Controller, Int) -> Unit) {
		for (index in captureTargets.size - 1 downTo 0) {
			val capture = captureTargets[index]
			if (capture.player == player && capture.target === target) captureTargets.removeIndex(index)
		}
	}

	private fun captureFor(controller: Controller): ButtonCapture? {
		val player = playerOf(controller)
		for (index in captureTargets.size - 1 downTo 0) {
			val capture = captureTargets[index]
			if (capture.player == null || capture.player == player) return capture
		}
		return null
	}

	override fun connected(controller: Controller) {
		log.debug { "Controller connected." }
	}

	override fun disconnected(controller: Controller) {
		log.debug { "Controller disconnected." }
		scratchKeys.clear()
		val held = heldRawButtons.remove(controller)
		if (held != null) {
			held.forEachIntReentrant { code ->
				if (suppressedDownButtons.get(controller)?.contains(code) != true) {
					val key = keyFor(controller, code)
					if (key != NO_KEY) scratchKeys.add(key)
				}
			}
		}
		axisStates.remove(controller)?.let { state ->
			if (state.horizontalKey != NO_KEY) scratchKeys.add(state.horizontalKey)
			if (state.verticalKey != NO_KEY) scratchKeys.add(state.verticalKey)
		}
		suppressedDownButtons.remove(controller)
		playerAssignments.remove(controller)
		scratchKeys.forEachIntReentrant { key -> if (!anyInputHolds(key)) emitKeyUp(key, playerForKey(key)) }
	}

	override fun buttonDown(controller: Controller, buttonCode: Int): Boolean {
		val key = keyFor(controller, buttonCode)
		val alreadyDown = key != NO_KEY && anyInputHolds(key)
		heldOf(controller).add(buttonCode)
		captureFor(controller)?.let { capture ->
			suppressedPressesOf(controller).add(buttonCode)
			capture.target(controller, buttonCode)
			return true
		}
		if (key == NO_KEY) return false
		if (!alreadyDown) emitKeyDown(key, playerOf(controller))
		return true
	}

	override fun buttonUp(controller: Controller, buttonCode: Int): Boolean {
		heldRawButtons.get(controller)?.remove(buttonCode)
		if (suppressedDownButtons.get(controller)?.remove(buttonCode) == true) return true
		if (captureFor(controller) != null) return true
		val key = keyFor(controller, buttonCode)
		if (key == NO_KEY) return false
		if (!anyInputHolds(key)) emitKeyUp(key, playerOf(controller))
		return true
	}

	override fun axisMoved(controller: Controller, axisCode: Int, value: Float): Boolean {
		if (captureFor(controller) != null) return false
		val bindings = bindingsFor(playerOf(controller))
		if (!bindings.axisNavigationEnabled) return false
		val verticalChanged = checkVertical(controller, bindings)
		val horizontalChanged = checkHorizontal(controller, bindings)
		return verticalChanged || horizontalChanged
	}

	private fun checkVertical(controller: Controller, bindings: MetaUiInputBindings): Boolean {
		val value = controller.getAxis(bindings.verticalAxis)
		val action = when {
			value < -deadzone -> MetaUiAction.NAVIGATE_UP
			value > deadzone -> MetaUiAction.NAVIGATE_DOWN
			else -> null
		}
		return updateAxisKey(controller, vertical = true, action)
	}

	private fun checkHorizontal(controller: Controller, bindings: MetaUiInputBindings): Boolean {
		val value = controller.getAxis(bindings.horizontalAxis)
		val action = when {
			value < -deadzone -> MetaUiAction.NAVIGATE_LEFT
			value > deadzone -> MetaUiAction.NAVIGATE_RIGHT
			else -> null
		}
		return updateAxisKey(controller, vertical = false, action)
	}

	private fun updateAxisKey(controller: Controller, vertical: Boolean, action: MetaUiAction?): Boolean {
		val state = axisStateOf(controller)
		val oldKey = if (vertical) state.verticalKey else state.horizontalKey
		val player = playerOf(controller)
		val newKey = if (action == null) NO_KEY else MetaUiControllerKeys.keyFor(player, action, bindingsFor(player))
		if (oldKey == newKey) return false

		if (vertical) state.verticalKey = NO_KEY else state.horizontalKey = NO_KEY
		if (oldKey != NO_KEY && !anyInputHolds(oldKey)) emitKeyUp(oldKey, player)
		if (newKey != NO_KEY) {
			val alreadyDown = anyInputHolds(newKey)
			if (vertical) state.verticalKey = newKey else state.horizontalKey = newKey
			if (!alreadyDown) emitKeyDown(newKey, player)
		}
		return true
	}

	/** Releases and suppresses everything held by [player], or by everyone when null. */
	private fun suspendPlayerInput(player: MetaPlayer?) {
		scratchKeys.clear()
		heldRawButtons.forEachEntryReentrant { controller, buttons ->
			if (player == null || playerOf(controller) == player) {
				buttons.forEachIntReentrant { code ->
					val key = keyFor(controller, code)
					if (key != NO_KEY) scratchKeys.add(key)
				}
				suppressedPressesOf(controller).addAll(buttons)
			}
		}
		axisStates.forEachEntryReentrant { controller, state ->
			if (player == null || playerOf(controller) == player) {
				if (state.horizontalKey != NO_KEY) scratchKeys.add(state.horizontalKey)
				if (state.verticalKey != NO_KEY) scratchKeys.add(state.verticalKey)
				state.horizontalKey = NO_KEY
				state.verticalKey = NO_KEY
			}
		}
		scratchKeys.forEachIntReentrant { key -> emitKeyUp(key, playerForKey(key)) }
	}

	/** Releases one controller before its player identity changes; later physical releases are swallowed. */
	private fun suspendControllerInput(controller: Controller) {
		scratchKeys.clear()
		heldRawButtons.get(controller)?.let { buttons ->
			buttons.forEachIntReentrant { code ->
				val key = keyFor(controller, code)
				if (key != NO_KEY) scratchKeys.add(key)
			}
			suppressedPressesOf(controller).addAll(buttons)
		}
		axisStates.remove(controller)?.let { state ->
			if (state.horizontalKey != NO_KEY) scratchKeys.add(state.horizontalKey)
			if (state.verticalKey != NO_KEY) scratchKeys.add(state.verticalKey)
		}
		scratchKeys.forEachIntReentrant { key -> if (!anyInputHolds(key)) emitKeyUp(key, playerForKey(key)) }
	}

	/** Whether a button or stick on any controller still holds this player-tagged key. */
	private fun anyInputHolds(key: Int): Boolean {
		var held = false
		heldRawButtons.forEachEntryReentrant { controller, buttons ->
			val suppressed = suppressedDownButtons.get(controller)
			buttons.forEachIntReentrant { code ->
				if (!held && suppressed?.contains(code) != true && keyFor(controller, code) == key) held = true
			}
		}
		if (held) return true
		axisStates.forEachEntryReentrant { _, state ->
			if (state.horizontalKey == key || state.verticalKey == key) held = true
		}
		return held
	}

	private fun bindingsFor(player: MetaPlayer): MetaUiInputBindings =
		if (player == MetaPlayer.ONE) uiBindings else uiProfiles[player]

	private fun keyFor(controller: Controller, buttonCode: Int): Int {
		val player = playerOf(controller)
		val bindings = bindingsFor(player)
		val action = bindings.actionForButton(controller, buttonCode) ?: return NO_KEY
		return MetaUiControllerKeys.keyFor(player, action, bindings)
	}

	private fun suppressedPressesOf(controller: Controller): IntSet {
		suppressedDownButtons.get(controller)?.let { return it }
		return IntSet().also { suppressedDownButtons.put(controller, it) }
	}

	private fun heldOf(controller: Controller): IntSet {
		heldRawButtons.get(controller)?.let { return it }
		return IntSet().also { heldRawButtons.put(controller, it) }
	}

	private fun axisStateOf(controller: Controller): AxisState {
		axisStates.get(controller)?.let { return it }
		return AxisState().also { axisStates.put(controller, it) }
	}

	private fun playerForKey(keycode: Int): MetaPlayer = MetaUiControllerKeys.playerFor(keycode) ?: MetaPlayer.ONE

	private fun emitKeyDown(keycode: Int, player: MetaPlayer) {
		if (keycode == NO_KEY) return
		metaInput?.keyDownFromController(player, keycode)
	}

	private fun emitKeyUp(keycode: Int, player: MetaPlayer) {
		if (keycode == NO_KEY) return
		metaInput?.keyUpFromController(player, keycode)
	}

	private class AxisState {
		var horizontalKey = NO_KEY
		var verticalKey = NO_KEY
	}

	private class ButtonCapture(
		val player: MetaPlayer?,
		val target: (Controller, Int) -> Unit,
	)
}
