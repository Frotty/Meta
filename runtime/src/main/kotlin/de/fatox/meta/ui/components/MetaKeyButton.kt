package de.fatox.meta.ui.components

import com.badlogic.gdx.Input
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe

class MetaKeyButton(keyCode: Int) : MetaTextButton(Input.Keys.toString(keyCode)) {
	val keyCodeValue: Signal<Int> = signal(keyCode)
	val keyCode: Int get() = keyCodeValue.peek()
	@Suppress("unused")
	private val keyCodeBinding = keyCodeValue.subscribe { setText(Input.Keys.toString(keyCodeValue.peek())) }

	init {
		addListener(MetaListener {
			// TODO dialog
		})
	}

	fun setFromKeyCode(keyCode: Int) {
		keyCodeValue.value = keyCode
	}
}
