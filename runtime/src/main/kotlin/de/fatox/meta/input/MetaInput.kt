package de.fatox.meta.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.controllers.Controllers
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import de.fatox.meta.camera.ArcCamControl
import de.fatox.meta.ui.components.MetaClickListener

class MetaInput : InputProcessor {
    private val globalKeyListeners = IntMap<Array<KeyListener>>()
    private val screenKeyListeners = IntMap<Array<KeyListener>>()
    private var exclusiveProcessor: InputProcessor? = null
    private val globalProcessors = Array<InputProcessor>()
    private val screenProcessors = Array<InputProcessor>()
    fun changeScreen() {
        screenKeyListeners.clear()
        screenProcessors.clear()
    }

    fun addAdapterForScreen(adapter: InputProcessor) {
        screenProcessors.add(adapter)
    }

    fun registerGlobalKeyListener(keycode: Int, keyListener: KeyListener) {
        registerGlobalKeyListener(keycode, 0, keyListener)
    }

    fun registerGlobalKeyListener(keycode: Int, milisRequired: Long, keyListener: KeyListener) {
        if (!globalKeyListeners.containsKey(keycode)) {
            globalKeyListeners.put(keycode, Array())
        }
        keyListener.setRequiredLengthMillis(milisRequired)
        globalKeyListeners[keycode].add(keyListener)
    }

    fun registerScreenKeyListener(keycode: Int, keyListener: KeyListener) {
        registerScreenKeyListener(keycode, 0, keyListener)
    }

    fun registerScreenKeyListener(keycode: Int, milisRequired: Long, keyListener: KeyListener) {
        if (!screenKeyListeners.containsKey(keycode)) {
            screenKeyListeners.put(keycode, Array())
        }
        keyListener.setRequiredLengthMillis(milisRequired)
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

    fun scrolled(amount: Int): Boolean {
        if (exclusiveProcessor != null) {
            exclusiveProcessor!!.scrolled(amount.toFloat())
            return true
        }
        for (processor in globalProcessors) {
            if (processor.scrolled(amount.toFloat())) return true
        }
        for (processor in screenProcessors) {
            if (processor.scrolled(amount.toFloat())) return true
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

    fun registerClick(simpleName: String?, metaClickListener: MetaClickListener?) {}
    fun setExclusiveProcessor(exclusiveProcessor: InputProcessor?) {
        this.exclusiveProcessor = exclusiveProcessor
    }

    fun addGlobalAdapter(processor: InputProcessor) {
        globalProcessors.add(processor)
    }

    fun removeAdapterFromScreen(camControl: ArcCamControl) {
        screenProcessors.removeValue(camControl, true)
    }

    init {
        Gdx.input.inputProcessor = this
        Controllers.addListener(MetaControllerListener(this))
    }
}