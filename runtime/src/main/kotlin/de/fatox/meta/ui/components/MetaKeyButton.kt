package de.fatox.meta.ui.components

import com.badlogic.gdx.Input
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal

class MetaKeyButton(private var keyCode: Int) : MetaTextButton(Input.Keys.toString(keyCode)) {
	val keyCodeValue: Signal<Int> = signal(keyCode)

	init {
		addListener(MetaListener {
			// TODO dialog
		})
	}

	fun setFromKeyCode(keyCode: Int) {
		this.keyCode = keyCode
		keyCodeValue.value = keyCode
		setText(Input.Keys.toString(keyCode))
	}
}
