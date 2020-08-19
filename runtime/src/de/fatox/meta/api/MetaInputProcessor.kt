package de.fatox.meta.api

import com.badlogic.gdx.InputProcessor
import de.fatox.meta.camera.ArcCamControl
import de.fatox.meta.input.KeyListener

inline fun MetaInputProcessor.registerGlobalKeyListener(
	keycode: Int,
	millisRequired: Long = 0,
	crossinline keyListener: KeyListener.() -> Unit,
) {
	registerGlobalKeyListener(keycode, millisRequired, object : KeyListener() {
		override fun onEvent() {
			keyListener()
		}
	})
}

inline fun MetaInputProcessor.registerScreenKeyListener(
	keycode: Int,
	millisRequired: Long = 0,
	crossinline keyListener: KeyListener.() -> Unit,
) {
	registerScreenKeyListener(keycode, millisRequired, object : KeyListener() {
		override fun onEvent() {
			keyListener()
		}
	})
}

interface MetaInputProcessor : InputProcessor {
	fun changeScreen()
	fun addAdapterForScreen(adapter: InputProcessor)
	fun registerGlobalKeyListener(keycode: Int, keyListener: KeyListener)
	fun registerGlobalKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener)
	fun registerScreenKeyListener(keycode: Int, keyListener: KeyListener)
	fun registerScreenKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener)
	fun setExclusiveProcessor(exclusiveProcessor: InputProcessor?)
	fun addGlobalAdapter(processor: InputProcessor)
	fun removeAdapterFromScreen(camControl: ArcCamControl)
}