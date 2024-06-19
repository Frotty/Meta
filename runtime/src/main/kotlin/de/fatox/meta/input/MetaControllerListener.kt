package de.fatox.meta.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import kotlin.math.absoluteValue

private val log = MetaLoggerFactory.logger {}

object MetaControllerListener : ControllerListener {
	var metaInput: MetaInput? = null
	private var currentDownKey = -1
	var deadzone = 0.39f

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
		val axisValue = controller.getAxis(1)
		if (currentDownKey != Input.Keys.UP && axisValue < -deadzone) {
			metaInput?.keyUp(currentDownKey)
			currentDownKey = Input.Keys.UP
			metaInput?.keyDown(currentDownKey)
			return true
		} else if (currentDownKey == Input.Keys.UP && axisValue > -deadzone) {
			metaInput?.keyUp(currentDownKey)
			currentDownKey = -1
			return true
		}
		if (currentDownKey != Input.Keys.DOWN && axisValue > deadzone) {
			metaInput?.keyUp(currentDownKey)
			currentDownKey = Input.Keys.DOWN
			metaInput?.keyDown(currentDownKey)
			return true
		} else if (currentDownKey == Input.Keys.DOWN && axisValue < deadzone) {
			metaInput?.keyUp(currentDownKey)
			currentDownKey = -1
			return true
		}
		return false
	}

	private fun checkHor(controller: Controller): Boolean {
		val axisValue = controller.getAxis(0)
		if (currentDownKey != Input.Keys.LEFT && axisValue < -deadzone) {
			metaInput?.keyUp(currentDownKey)
			currentDownKey = Input.Keys.LEFT
			metaInput?.keyDown(currentDownKey)
			return true
		} else if (currentDownKey == Input.Keys.LEFT && axisValue > -deadzone) {
			metaInput?.keyUp(currentDownKey)
			currentDownKey = -1
			return true
		}
		if (currentDownKey != Input.Keys.RIGHT && axisValue > deadzone) {
			metaInput?.keyUp(currentDownKey)
			currentDownKey = Input.Keys.RIGHT
			metaInput?.keyDown(currentDownKey)
			return true
		} else if (currentDownKey == Input.Keys.RIGHT && axisValue < deadzone) {
			metaInput?.keyUp(currentDownKey)
			currentDownKey = -1
			return true
		}
		return false
	}
}
