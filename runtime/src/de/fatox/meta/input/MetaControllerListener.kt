package de.fatox.meta.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.PovDirection
import com.badlogic.gdx.math.Vector3
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MetaControllerListener(private val metaInput: MetaInput) : ControllerListener {
	private var currentDownKey = -1
	private val deadzone = 0.395f

	override fun connected(controller: Controller) {
		log.debug("Controller connected")
	}

	override fun disconnected(controller: Controller) {
		log.debug("Controller disconnected")
	}

	override fun buttonDown(controller: Controller, buttonCode: Int): Boolean {
		return false
	}

	override fun buttonUp(controller: Controller, buttonCode: Int): Boolean {
		println(buttonCode)
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

	override fun povMoved(controller: Controller, povCode: Int, value: PovDirection): Boolean {
		return false
	}

	override fun xSliderMoved(controller: Controller, sliderCode: Int, value: Boolean): Boolean {
		return false
	}

	override fun ySliderMoved(controller: Controller, sliderCode: Int, value: Boolean): Boolean {
		return false
	}

	override fun accelerometerMoved(controller: Controller, accelerometerCode: Int, value: Vector3): Boolean {
		return false
	}
}

private val log: Logger = LoggerFactory.getLogger(MetaControllerListener::class.java)