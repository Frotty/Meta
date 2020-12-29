package de.fatox.meta.ui.components

import com.badlogic.gdx.Input

class MetaKeyButton(private var keyCode: Int) : MetaTextButton(Input.Keys.toString(keyCode)) {

    init {
        addListener(MetaListener({
            // TODO dialog
        }))
    }

    fun setFromKeyCode(keyCode: Int) {
        this.keyCode = keyCode
        setText(Input.Keys.toString(keyCode))
    }
}
