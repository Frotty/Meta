package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.ui.MetaColor

internal class MetaButtonFocusStyle(
	private val button: Button,
	initialStyle: Button.ButtonStyle,
	private val focusedStyleFactory: (Button.ButtonStyle) -> Button.ButtonStyle,
) {
	private var normalStyle = Button.ButtonStyle(initialStyle)
	private var focusedStyle = focusedStyleFactory(normalStyle)
	private var focused = false

	fun install(style: Button.ButtonStyle) {
		normalStyle = Button.ButtonStyle(style)
		focusedStyle = focusedStyleFactory(normalStyle)
		apply()
	}

	fun setFocused(value: Boolean) {
		if (focused == value) return
		focused = value
		apply()
	}

	private fun apply() {
		button.style = if (focused) focusedStyle else normalStyle
	}
}

internal class MetaTextFieldFocusStyle(
	private val field: TextField,
	initialStyle: TextField.TextFieldStyle,
	private val focusedStyleFactory: (TextField.TextFieldStyle) -> TextField.TextFieldStyle,
) {
	private var normalStyle = TextField.TextFieldStyle(initialStyle)
	private var focusedStyle = focusedStyleFactory(normalStyle)
	private var focused = false
	private var focusEnabled = true

	fun install(style: TextField.TextFieldStyle, focusEnabled: Boolean) {
		normalStyle = TextField.TextFieldStyle(style)
		this.focusEnabled = focusEnabled
		rebuildFocusedStyle()
		apply()
	}

	fun setFocusEnabled(value: Boolean) {
		if (focusEnabled == value) return
		focusEnabled = value
		rebuildFocusedStyle()
		apply()
	}

	private fun rebuildFocusedStyle() {
		focusedStyle = if (focusEnabled) focusedStyleFactory(normalStyle) else TextField.TextFieldStyle(normalStyle)
	}

	fun setFocused(value: Boolean) {
		if (focused == value) return
		focused = value
		apply()
	}

	private fun apply() {
		field.style = if (focused) focusedStyle else normalStyle
	}
}

internal class MetaSelectBoxFocusStyle<T>(
	private val selectBox: SelectBox<T>,
	initialStyle: SelectBox.SelectBoxStyle,
	private val focusedStyleFactory: (SelectBox.SelectBoxStyle) -> SelectBox.SelectBoxStyle,
) {
	private var normalStyle = SelectBox.SelectBoxStyle(initialStyle)
	private var focusedStyle = focusedStyleFactory(normalStyle)
	private var focused = false

	fun install(style: SelectBox.SelectBoxStyle) {
		normalStyle = SelectBox.SelectBoxStyle(style)
		focusedStyle = focusedStyleFactory(normalStyle)
		apply()
	}

	fun setFocused(value: Boolean) {
		if (focused == value) return
		focused = value
		apply()
	}

	private fun apply() {
		selectBox.style = SelectBox.SelectBoxStyle(if (focused) focusedStyle else normalStyle)
		selectBox.list.style = List.ListStyle(selectBox.style.listStyle)
	}
}

internal class MetaDisabledTint(private val root: Group) {
	private val originalColors = ObjectMap<Actor, Color>()

	fun apply(disabled: Boolean) {
		if (disabled) {
			tintChildren(root)
		} else {
			restore()
		}
	}

	private fun tintChildren(group: Group) {
		val children = group.children
		for (i in 0 until children.size) {
			val child = children[i]
			if (!originalColors.containsKey(child)) originalColors.put(child, Color(child.color))
			child.color.set(DISABLED_CHILD_COLOR)
			if (child is Group) tintChildren(child)
		}
	}

	private fun restore() {
		val entries = originalColors.entries()
		while (entries.hasNext()) {
			val entry = entries.next()
			entry.key.color.set(entry.value)
		}
		originalColors.clear()
	}

	private companion object {
		private val DISABLED_CHILD_COLOR = Color(MetaColor.TEXT_DISABLED)
	}
}
