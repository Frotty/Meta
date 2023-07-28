@file:Suppress("GDXKotlinUnsafeIterator")

package de.fatox.meta.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import de.fatox.meta.api.MetaInputProcessor

class MetaInput : MetaInputProcessor {
	private val globalKeyListeners = IntMap<Array<KeyListener>>()
	private val screenKeyListeners = IntMap<Array<KeyListener>>()
	private val globalProcessors = Array<InputProcessor>()
	private val screenProcessors = Array<InputProcessor>()
	private val globalScrollListeners = Array<ScrollListener>()
	private val screenScrollListeners = Array<ScrollListener>()

	override var exclusiveProcessor: InputProcessor? = null
	override var isLeftCtrlDown: Boolean = false
		private set
	override var isRightCtrlDown: Boolean = false
		private set
	override var isLeftShiftDown: Boolean = false
		private set
	override var isRightShiftDown: Boolean = false
		private set

	init {
		Gdx.input.inputProcessor = this
		Controllers.addListener(MetaControllerListener(this))
	}

	override fun changeScreen() {
		screenKeyListeners.clear()
		screenProcessors.clear()
		screenScrollListeners.clear()
	}

	override fun addGlobalInputProcessor(inputProcessor: InputProcessor): InputProcessor =
		inputProcessor.also { globalProcessors.add(it) }

	override fun removeGlobalInputProcessor(inputProcessor: InputProcessor): Boolean =
		globalProcessors.removeValue(inputProcessor, true)

	override fun addScreenInputProcessor(inputProcessor: InputProcessor): InputProcessor =
		inputProcessor.also { screenProcessors.add(it) }

	override fun removeScreenInputProcessor(inputProcessor: InputProcessor): Boolean =
		screenProcessors.removeValue(inputProcessor, true)

	override fun addGlobalKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener {
		if (!globalKeyListeners.containsKey(keycode)) {
			globalKeyListeners.put(keycode, Array())
		}
		keyListener.requiredLengthMillis = millisRequired
		globalKeyListeners[keycode].add(keyListener)
		return keyListener
	}

	override fun removeGlobalKeyListener(keycode: Int, keyListener: KeyListener): Boolean =
		globalKeyListeners.containsKey(keycode) && globalKeyListeners.get(keycode).removeValue(keyListener, true)

	override fun addScreenKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener {
		if (!screenKeyListeners.containsKey(keycode)) {
			screenKeyListeners.put(keycode, Array())
		}
		keyListener.requiredLengthMillis = millisRequired
		screenKeyListeners[keycode].add(keyListener)
		return keyListener
	}

	override fun removeScreenKeyListener(keycode: Int, keyListener: KeyListener): Boolean =
		screenKeyListeners.containsKey(keycode) && screenKeyListeners.get(keycode).removeValue(keyListener, true)

	override fun addGlobalScrollListener(scrollListener: ScrollListener): ScrollListener =
		scrollListener.also { globalScrollListeners.add(it) }

	override fun removeGlobalScrollListener(scrollListener: ScrollListener): Boolean =
		globalScrollListeners.removeValue(scrollListener, true)

	override fun addScreenScrollListener(scrollListener: ScrollListener): ScrollListener =
		scrollListener.also { screenScrollListeners.add(it) }

	override fun removeScreenScrollListener(scrollListener: ScrollListener): Boolean =
		screenScrollListeners.removeValue(scrollListener, true)

	override fun keyDown(keycode: Int): Boolean {
		when (keycode) {
			Input.Keys.CONTROL_LEFT -> isLeftCtrlDown = true
			Input.Keys.CONTROL_RIGHT -> isRightCtrlDown = true
			Input.Keys.SHIFT_LEFT -> isLeftShiftDown = true
			Input.Keys.SHIFT_RIGHT -> isRightShiftDown = true
		}

		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.keyDown(keycode)
			return false
		}
		if (screenKeyListeners.containsKey(keycode)) {
			for (listener in screenKeyListeners[keycode]) listener.onDown()
		}
		if (globalKeyListeners.containsKey(keycode)) {
			for (listener in globalKeyListeners[keycode]) listener.onDown()
		}
		for (processor in globalProcessors) processor.keyDown(keycode)
		for (processor in screenProcessors) processor.keyDown(keycode)
		return false
	}

	override fun keyUp(keycode: Int): Boolean {
		when (keycode) {
			Input.Keys.CONTROL_LEFT -> isLeftCtrlDown = false
			Input.Keys.CONTROL_RIGHT -> isRightCtrlDown = false
			Input.Keys.SHIFT_LEFT -> isLeftShiftDown = false
			Input.Keys.SHIFT_RIGHT -> isRightShiftDown = false
		}

		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.keyUp(keycode)
			return false
		}
		if (screenKeyListeners.containsKey(keycode)) {
			for (listener in screenKeyListeners[keycode]) listener.onUp()
		}
		if (globalKeyListeners.containsKey(keycode)) {
			for (listener in globalKeyListeners[keycode]) listener.onUp()
		}
		for (processor in globalProcessors) processor.keyUp(keycode)
		for (processor in screenProcessors) processor.keyUp(keycode)
		return false
	}

	override fun keyTyped(character: Char): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.keyTyped(character)
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

	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.touchDown(screenX, screenY, pointer, button)
			return true
		}
		for (processor in globalProcessors) if (processor.touchDown(screenX, screenY, pointer, button)) return true
		for (processor in screenProcessors) if (processor.touchDown(screenX, screenY, pointer, button)) return true
		return true
	}

	override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.touchUp(screenX, screenY, pointer, button)
			return false
		}
		for (processor in globalProcessors) if (processor.touchUp(screenX, screenY, pointer, button)) return true
		for (processor in screenProcessors) if (processor.touchUp(screenX, screenY, pointer, button)) return true
		return false
	}

	override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		TODO("Not yet implemented")
	}

	override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.touchDragged(screenX, screenY, pointer)
			return false
		}
		for (processor in globalProcessors) if (processor.touchDragged(screenX, screenY, pointer)) return true
		for (processor in screenProcessors) if (processor.touchDragged(screenX, screenY, pointer)) return true
		return true
	}

	override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.mouseMoved(screenX, screenY)
			return false
		}
		for (processor in globalProcessors) if (processor.mouseMoved(screenX, screenY)) return true
		for (processor in screenProcessors) if (processor.mouseMoved(screenX, screenY)) return true
		return false
	}

	override fun scrolled(amountX: Float, amountY: Float): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.scrolled(amountX, amountY)
			return true
		}
		for (listener in globalScrollListeners) listener.onScroll()
		for (listener in screenScrollListeners) listener.onScroll()
		for (processor in globalProcessors) if (processor.scrolled(amountX, amountY)) return true
		for (processor in screenProcessors) if (processor.scrolled(amountX, amountY)) return true
		return true
	}
}