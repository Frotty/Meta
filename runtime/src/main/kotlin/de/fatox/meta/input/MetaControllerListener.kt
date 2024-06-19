package de.fatox.meta.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug

private val log = MetaLoggerFactory.logger {}

class MetaControllerListener(private val metaInput: MetaInput) : ControllerListener {
	private var currentDownKey = -1
	var deadzone = 0.395f

	override fun connected(controller: Controller) {
		log.debug { "Controller connected." }
	}

	override fun disconnected(controller: Controller) {
		log.debug { "Controller disconnected." }
	}

	override fun buttonDown(controller: Controller, buttonCode: Int): Boolean {
		return false
	}

	override fun buttonUp(controller: Controller, buttonCode: Int): Boolean {
		log.debug { "$buttonCode" }
		return false
	}

	override fun axisMoved(controller: Controller, axisCode: Int, value: Float): Boolean {
		checkVert(controller)
		checkHor(controller)
		return false
	}

	private fun checkVert(controller: Controller): Boolean {
		if (currentDownKey != Input.Keys.UP && controller.getAxis(1) < -deadzone) {
			metaInput.keyUp(currentDownKey)
			currentDownKey = Input.Keys.UP
			metaInput.keyDown(currentDownKey)
			return true
		} else if (currentDownKey == Input.Keys.UP && controller.getAxis(1) > -deadzone) {
			metaInput.keyUp(currentDownKey)
			currentDownKey = -1
			return true
		}
		if (currentDownKey != Input.Keys.DOWN && controller.getAxis(1) > deadzone) {
			metaInput.keyUp(currentDownKey)
			currentDownKey = Input.Keys.DOWN
			metaInput.keyDown(currentDownKey)
			return true
		} else if (currentDownKey == Input.Keys.DOWN && controller.getAxis(1) < deadzone) {
			metaInput.keyUp(currentDownKey)
			currentDownKey = -1
			return true
		}
		return false
	}

	private fun checkHor(controller: Controller): Boolean {
		if (currentDownKey != Input.Keys.LEFT && controller.getAxis(0) < -deadzone) {
			metaInput.keyUp(currentDownKey)
			currentDownKey = Input.Keys.LEFT
			metaInput.keyDown(currentDownKey)
			return true
		} else if (currentDownKey == Input.Keys.LEFT && controller.getAxis(0) > -deadzone) {
			metaInput.keyUp(currentDownKey)
			currentDownKey = -1
			return true
		}
		if (currentDownKey != Input.Keys.RIGHT && controller.getAxis(0) > deadzone) {
			metaInput.keyUp(currentDownKey)
			currentDownKey = Input.Keys.RIGHT
			metaInput.keyDown(currentDownKey)
			return true
		} else if (currentDownKey == Input.Keys.RIGHT && controller.getAxis(0) < deadzone) {
			metaInput.keyUp(currentDownKey)
			currentDownKey = -1
			return true
		}
		return false
	}
}
