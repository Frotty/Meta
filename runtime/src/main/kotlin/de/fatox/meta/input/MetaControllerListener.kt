package de.fatox.meta.input

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.utils.IntSet
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.MetaInputProcessor
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

	/** Canonical keys currently held down via controller buttons, so a disconnect can release them (no stuck keys). */
	private val downButtonKeys = IntSet()
	var deadzone = 0.39f

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
	private var captureTarget: ((Controller, Int) -> Unit)? = null

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
		releaseButtonKeys()
		captureTarget = target
	}

	/**
	 * Stops routing to [target]. A no-op if some other capture has since taken over, so a dialog closing late
	 * cannot switch off a capture it does not own.
	 */
	fun releaseCapture(target: (Controller, Int) -> Unit) {
		if (captureTarget === target) captureTarget = null
	}

	override fun connected(controller: Controller) {
		log.debug { "Controller connected." }
	}

	override fun disconnected(controller: Controller) {
		log.debug { "Controller disconnected." }
		releaseAxisKeys()
		releaseButtonKeys()
	}

	override fun buttonDown(controller: Controller, buttonCode: Int): Boolean {
		captureTarget?.let {
			it(controller, buttonCode)
			return true
		}
		return uiBindings.actionForButton(controller, buttonCode)?.let {
			val key = uiBindings.canonicalKeyFor(it)
			downButtonKeys.add(key)
			emitKeyDown(key)
			true
		} ?: false
	}

	override fun buttonUp(controller: Controller, buttonCode: Int): Boolean {
		// Consumed rather than translated: the matching down was captured, so emitting a key-up for a key that was
		// never pressed would look like a release of whatever that button normally means.
		if (captureTarget != null) return true
		return uiBindings.actionForButton(controller, buttonCode)?.let {
			val key = uiBindings.canonicalKeyFor(it)
			downButtonKeys.remove(key)
			emitKeyUp(key)
			true
		} ?: false
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

	private fun releaseButtonKeys() {
		downButtonKeys.forEachIntReentrant(::emitKeyUp)
		downButtonKeys.clear()
	}

	private fun emitKeyDown(keycode: Int) {
		if (keycode != -1) metaInput?.keyDown(keycode)
	}

	private fun emitKeyUp(keycode: Int) {
		if (keycode != -1) metaInput?.keyUp(keycode)
	}
}
