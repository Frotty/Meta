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

	// Exclusive input owners as a LIFO stack; the top receives all input. See MetaInputProcessor docs.
	private val exclusiveProcessors = Array<InputProcessor>()
	override var exclusiveProcessor: InputProcessor?
		get() = if (exclusiveProcessors.isEmpty) null else exclusiveProcessors.peek()
		set(value) {
			if (value == null) {
				if (exclusiveProcessors.notEmpty()) exclusiveProcessors.pop()
			} else {
				pushExclusiveProcessor(value)
			}
		}

	override fun pushExclusiveProcessor(processor: InputProcessor) {
		exclusiveProcessors.removeValue(processor, true) // avoid duplicates; (re)push to the top
		exclusiveProcessors.add(processor)
	}

	override fun popExclusiveProcessor(processor: InputProcessor): Boolean =
		exclusiveProcessors.removeValue(processor, true)

	override fun clearExclusiveProcessors() {
		exclusiveProcessors.clear()
	}

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
		MetaControllerListener.metaInput = this
		Controllers.addListener(MetaControllerListener)
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
			val listeners = screenKeyListeners[keycode]
			for (i in 0 until listeners.size) listeners[i].onDown()
		}
		if (globalKeyListeners.containsKey(keycode)) {
			val listeners = globalKeyListeners[keycode]
			for (i in 0 until listeners.size) listeners[i].onDown()
		}
		for (i in 0 until globalProcessors.size) globalProcessors[i].keyDown(keycode)
		for (i in 0 until screenProcessors.size) screenProcessors[i].keyDown(keycode)
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
			val listeners = screenKeyListeners[keycode]
			for (i in 0 until listeners.size) listeners[i].onUp()
		}
		if (globalKeyListeners.containsKey(keycode)) {
			val listeners = globalKeyListeners[keycode]
			for (i in 0 until listeners.size) listeners[i].onUp()
		}
		for (i in 0 until globalProcessors.size) globalProcessors[i].keyUp(keycode)
		for (i in 0 until screenProcessors.size) screenProcessors[i].keyUp(keycode)
		return false
	}

	override fun keyTyped(character: Char): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.keyTyped(character)
			return false
		}
		for (i in 0 until globalProcessors.size) {
			globalProcessors[i].keyTyped(character)
		}
		for (i in 0 until screenProcessors.size) {
			screenProcessors[i].keyTyped(character)
		}
		return false
	}

	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.touchDown(screenX, screenY, pointer, button)
			return true
		}
		for (i in 0 until globalProcessors.size) if (globalProcessors[i].touchDown(screenX, screenY, pointer, button)) return true
		for (i in 0 until screenProcessors.size) if (screenProcessors[i].touchDown(screenX, screenY, pointer, button)) return true
		return true
	}

	override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.touchUp(screenX, screenY, pointer, button)
			return false
		}
		for (i in 0 until globalProcessors.size) if (globalProcessors[i].touchUp(screenX, screenY, pointer, button)) return true
		for (i in 0 until screenProcessors.size) if (screenProcessors[i].touchUp(screenX, screenY, pointer, button)) return true
		return false
	}

	override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		// A cancelled touch (e.g. OS-interrupted gesture) must release any drag state, so treat it like a touch-up
		// rather than crashing. libGDX does call this on some platforms.
		return touchUp(screenX, screenY, pointer, button)
	}

	override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.touchDragged(screenX, screenY, pointer)
			return false
		}
		for (i in 0 until globalProcessors.size) if (globalProcessors[i].touchDragged(screenX, screenY, pointer)) return true
		for (i in 0 until screenProcessors.size) if (screenProcessors[i].touchDragged(screenX, screenY, pointer)) return true
		return true
	}

	override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.mouseMoved(screenX, screenY)
			return false
		}
		for (i in 0 until globalProcessors.size) if (globalProcessors[i].mouseMoved(screenX, screenY)) return true
		for (i in 0 until screenProcessors.size) if (screenProcessors[i].mouseMoved(screenX, screenY)) return true
		return false
	}

	override fun scrolled(amountX: Float, amountY: Float): Boolean {
		val exclusiveProcessor = exclusiveProcessor
		if (exclusiveProcessor != null) {
			exclusiveProcessor.scrolled(amountX, amountY)
			return true
		}
		for (i in 0 until globalScrollListeners.size) globalScrollListeners[i].onScroll()
		for (i in 0 until screenScrollListeners.size) screenScrollListeners[i].onScroll()
		for (i in 0 until globalProcessors.size) if (globalProcessors[i].scrolled(amountX, amountY)) return true
		for (i in 0 until screenProcessors.size) if (screenProcessors[i].scrolled(amountX, amountY)) return true
		return true
	}
}
