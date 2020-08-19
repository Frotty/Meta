package de.fatox.meta.api.extensions

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.ui.components.MetaClickListener

inline fun <reified T : Actor> T.onClick(crossinline action: MetaClickListener.(event: InputEvent) -> Unit): T {
	addListener(object : MetaClickListener() {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			action(event)
		}
	})
	return this
}

inline fun <reified T : Actor> T.onChange(crossinline action: ChangeListener.() -> Unit): T {
	addListener(object : ChangeListener() {
		override fun changed(event: ChangeEvent, actor: Actor) {
			action()
		}
	})
	return this
}