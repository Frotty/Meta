package de.fatox.meta.input

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.absoluteValue

private val log = MetaLoggerFactory.logger {}

object MetaControllerListener : ControllerListener {
	var metaInput: MetaInput? = null
	private val uiBindings: MetaUiInputBindings by lazyInject()
	private var currentHorDownKey = -1
	private var currentVertDownKey = -1
	var deadzone = 0.39f

	override fun connected(controller: Controller) {
		log.debug { "Controller connected." }
	}

	override fun disconnected(controller: Controller) {
		log.debug { "Controller disconnected." }
		releaseAxisKeys()
	}

	override fun buttonDown(controller: Controller, buttonCode: Int): Boolean {
		return uiBindings.actionForButton(controller, buttonCode)?.let {
			emitKeyDown(uiBindings.canonicalKeyFor(it))
			true
		} ?: false
	}

	override fun buttonUp(controller: Controller, buttonCode: Int): Boolean {
		return uiBindings.actionForButton(controller, buttonCode)?.let {
			emitKeyUp(uiBindings.canonicalKeyFor(it))
			true
		} ?: false
	}

	override fun axisMoved(controller: Controller, axisCode: Int, value: Float): Boolean {
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

	private fun emitKeyDown(keycode: Int) {
		if (keycode != -1) metaInput?.keyDown(keycode)
	}

	private fun emitKeyUp(keycode: Int) {
		if (keycode != -1) metaInput?.keyUp(keycode)
	}
}
