@file:Suppress("unused") // public utility class

package de.fatox.meta.api.extensions

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.Tooltip

inline fun <reified T : Actor> T.onClick(
	button: Int = Input.Buttons.LEFT,
	crossinline action: ClickListener.(event: InputEvent) -> Unit,
): T {
	addListener(object : ClickListener(button) {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			action(event)
		}
	})
	return this
}

inline fun <reified T : Actor> T.onChange(crossinline action: ChangeListener.(event: ChangeEvent) -> Unit): T {
	addListener(object : ChangeListener() {
		override fun changed(event: ChangeEvent, actor: Actor) {
			action(event)
		}
	})
	return this
}

inline fun <reified T : Actor> T.tooltip(text: String, align: Int = Align.center) {
	Tooltip().apply {
		setText(text)
		align(align)
		target = this@tooltip
	}
}
