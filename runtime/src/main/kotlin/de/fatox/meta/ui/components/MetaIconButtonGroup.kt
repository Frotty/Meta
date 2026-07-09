package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal

/**
 * Visual-only selection group for icon-button palettes such as editor brush tools.
 *
 * This does not use scene2d's checked/focus state. Regular button clicks stay momentary, while the currently selected
 * button gets Meta's blue active border until another button in the group is selected.
 */
class MetaIconButtonGroup {
	private val buttons = com.badlogic.gdx.utils.Array<MetaIconButton>()
	private val listeners = ObjectMap<MetaIconButton, ChangeListener>()

	val selectedButtonValue: Signal<MetaIconButton?> = signal(null)
	var selectedButton: MetaIconButton? = null
		private set

	constructor()

	constructor(vararg buttons: MetaIconButton) {
		for (button in buttons) add(button)
	}

	fun add(button: MetaIconButton, selected: Boolean = false): MetaIconButton {
		if (buttons.contains(button, true)) return button
		buttons.add(button)
		val listener = object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				select(button)
			}
		}
		listeners.put(button, listener)
		button.addListener(listener)
		if (selected) select(button)
		return button
	}

	fun select(button: MetaIconButton?) {
		if (button != null && !buttons.contains(button, true)) add(button)
		if (selectedButton === button) return
		selectedButton?.selected = false
		selectedButton = button
		selectedButtonValue.value = button
		selectedButton?.selected = true
	}

	fun clearSelection() {
		select(null)
	}

	fun clear() {
		for (i in 0 until buttons.size) {
			val button = buttons[i]
			button.selected = false
			listeners.remove(button)?.let { button.removeListener(it) }
		}
		buttons.clear()
		listeners.clear()
		selectedButton = null
		selectedButtonValue.value = null
	}
}
