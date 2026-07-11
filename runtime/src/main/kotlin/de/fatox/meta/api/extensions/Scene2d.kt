@file:Suppress("unused") // public utility class

package de.fatox.meta.api.extensions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Align
import de.fatox.meta.ui.components.MetaTooltip

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

inline fun <reified T : Actor> T.tooltip(
	text: String,
	align: Int = Align.center,
	showDelaySeconds: Float = 0f,
	hideDelaySeconds: Float = 0.04f,
	maxWidth: Float = 280f,
) {
	MetaTooltip.attach(this, text, align, showDelaySeconds, hideDelaySeconds, maxWidth)
}

inline fun <reified T : Actor> T.removeTooltip() {
	MetaTooltip.remove(this)
}

fun <T : Actor> T.cursorPointer(): T {
	for (i in 0 until listeners.size) if (listeners[i] is PointerCursorListener) return this
	addListener(PointerCursorListener(this))
	return this
}

private class PointerCursorListener(private val owner: Actor) : InputListener() {
	override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
		if (pointer == MOUSE_POINTER && event.target.nearestEnabledPointerOwner() === owner) setPointerCursor(true)
	}

	override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
		if (pointer == MOUSE_POINTER && event.target.nearestEnabledPointerOwner() === owner) {
			setPointerCursor(toActor.nearestEnabledPointerOwner() != null)
		}
	}

	private fun Actor?.nearestEnabledPointerOwner(): Actor? {
		var actor = this
		while (actor != null) {
			val disabled = actor is Disableable && actor.isDisabled
			if (!disabled) {
				for (i in 0 until actor.listeners.size) {
					if (actor.listeners[i] is PointerCursorListener) return actor
				}
			}
			actor = actor.parent
		}
		return null
	}

	private fun setPointerCursor(pointer: Boolean) {
		Gdx.graphics.setSystemCursor(if (pointer) Cursor.SystemCursor.Hand else Cursor.SystemCursor.Arrow)
	}

	private companion object {
		const val MOUSE_POINTER = -1
	}
}
