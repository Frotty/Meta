package de.fatox.meta.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.camera.ArcCamControl

object MetaInput : MetaInputProcessor {
	private val globalKeyListeners = IntMap<Array<KeyListener>>()
	private val screenKeyListeners = IntMap<Array<KeyListener>>()
	private var exclusiveProcessor: InputProcessor? = null
	private val globalProcessors = Array<InputProcessor>()
	private val screenProcessors = Array<InputProcessor>()

	override var isLeftCtrlDown = false
		private set
	override var isRightCtrlDown = false
		private set
	override var isLeftShiftDown = false
		private set
	override var isRightShiftDown = false
		private set

	init {
		Gdx.input.inputProcessor = this
		Controllers.addListener(MetaControllerListener(this))
	}

	override fun changeScreen() {
		screenKeyListeners.clear()
		screenProcessors.clear()
	}

	override fun addAdapterForScreen(adapter: InputProcessor) {
		screenProcessors.add(adapter)
	}

	override fun registerGlobalKeyListener(keycode: Int, keyListener: KeyListener) {
		registerGlobalKeyListener(keycode, 0, keyListener)
	}

	override fun registerGlobalKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener) {
		if (!globalKeyListeners.containsKey(keycode)) {
			globalKeyListeners.put(keycode, Array())
		}
		keyListener.requiredLengthMillis = millisRequired
		globalKeyListeners[keycode].add(keyListener)
	}

	override fun registerScreenKeyListener(keycode: Int, keyListener: KeyListener) {
		registerScreenKeyListener(keycode, 0, keyListener)
	}

	override fun registerScreenKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener) {
		if (!screenKeyListeners.containsKey(keycode)) {
			screenKeyListeners.put(keycode, Array())
		}
		keyListener.requiredLengthMillis = millisRequired
		screenKeyListeners[keycode].add(keyListener)
	}

	override fun keyTyped(character: Char): Boolean {
		if (exclusiveProcessor != null) {
			exclusiveProcessor!!.keyTyped(character)
			return false
		}
		for (processor in globalProcessors) {
			processor.keyTyped(character)
		}
		for (processor in screenProcessors) {
			processor.keyTyped(character)
		}
		return false
	}

	override fun keyDown(keycode: Int): Boolean {
		when (keycode) {
			Input.Keys.CONTROL_LEFT -> isLeftCtrlDown = true
			Input.Keys.CONTROL_RIGHT -> isRightCtrlDown = true
			Input.Keys.SHIFT_LEFT -> isLeftShiftDown = true
			Input.Keys.SHIFT_RIGHT -> isRightShiftDown = true
		}

		if (exclusiveProcessor != null) {
			exclusiveProcessor!!.keyDown(keycode)
			return false
		}
		if (screenKeyListeners.containsKey(keycode)) {
			for (listener in screenKeyListeners[keycode]) {
				listener.onDown()
			}
		}
		if (globalKeyListeners.containsKey(keycode)) {
			for (listener in globalKeyListeners[keycode]) {
				listener.onDown()
			}
		}
		for (processor in globalProcessors) {
			processor.keyDown(keycode)
		}
		for (processor in screenProcessors) {
			processor.keyDown(keycode)
		}
		return false
	}

	override fun keyUp(keycode: Int): Boolean {
		when (keycode) {
			Input.Keys.CONTROL_LEFT -> isLeftCtrlDown = false
			Input.Keys.CONTROL_RIGHT -> isRightCtrlDown = false
			Input.Keys.SHIFT_LEFT -> isLeftShiftDown = false
			Input.Keys.SHIFT_RIGHT -> isRightShiftDown = false
		}

		if (exclusiveProcessor != null) {
			exclusiveProcessor!!.keyUp(keycode)
			return false
		}
		if (screenKeyListeners.containsKey(keycode)) {
			for (listener in screenKeyListeners[keycode]) {
				listener.onUp()
			}
		}
		if (globalKeyListeners.containsKey(keycode)) {
			for (listener in globalKeyListeners[keycode]) {
				listener.onUp()
			}
		}
		for (processor in globalProcessors) {
			processor.keyUp(keycode)
		}
		for (processor in screenProcessors) {
			processor.keyUp(keycode)
		}
		return false
	}

	override fun scrolled(amountX: Float, amountY: Float): Boolean {
		if (exclusiveProcessor != null) {
			exclusiveProcessor!!.scrolled(amountX, amountY)
			return true
		}
		for (processor in globalProcessors) {
			if (processor.scrolled(amountX, amountY)) return true
		}
		for (processor in screenProcessors) {
			if (processor.scrolled(amountX, amountY)) return true
		}
		return true
	}

	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		if (exclusiveProcessor != null) {
			exclusiveProcessor!!.touchDown(screenX, screenY, pointer, button)
			return true
		}
		for (processor in globalProcessors) {
			if (processor.touchDown(screenX, screenY, pointer, button)) return true
		}
		for (processor in screenProcessors) {
			if (processor.touchDown(screenX, screenY, pointer, button)) return true
		}
		return true
	}

	override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		if (exclusiveProcessor != null) {
			exclusiveProcessor!!.touchUp(screenX, screenY, pointer, button)
			return false
		}
		for (processor in globalProcessors) {
			if (processor.touchUp(screenX, screenY, pointer, button)) return true
		}
		for (processor in screenProcessors) {
			if (processor.touchUp(screenX, screenY, pointer, button)) return true
		}
		return false
	}

	override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
		if (exclusiveProcessor != null) {
			exclusiveProcessor!!.touchDragged(screenX, screenY, pointer)
			return false
		}
		for (processor in globalProcessors) {
			if (processor.touchDragged(screenX, screenY, pointer)) return true
		}
		for (processor in screenProcessors) {
			if (processor.touchDragged(screenX, screenY, pointer)) return true
		}
		return true
	}

	override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
		if (exclusiveProcessor != null) {
			exclusiveProcessor!!.mouseMoved(screenX, screenY)
			return false
		}
		for (processor in globalProcessors) {
			if (processor.mouseMoved(screenX, screenY)) return true
		}
		for (processor in screenProcessors) {
			if (processor.mouseMoved(screenX, screenY)) return true
		}
		return false
	}

	override fun setExclusiveProcessor(exclusiveProcessor: InputProcessor?) {
		this.exclusiveProcessor = exclusiveProcessor
	}

	override fun addGlobalAdapter(processor: InputProcessor) {
		globalProcessors.add(processor)
	}

	override fun removeAdapterFromScreen(camControl: ArcCamControl) {
		screenProcessors.removeValue(camControl, true)
	}
}