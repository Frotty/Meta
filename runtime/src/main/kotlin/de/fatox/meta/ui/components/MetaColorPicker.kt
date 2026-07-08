package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.kotcrab.vis.ui.widget.color.ColorPicker
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter
import com.kotcrab.vis.ui.widget.color.ColorPickerListener

/**
 * Thin Meta wrapper around VisUI's [ColorPicker] so downstream projects can use a non-VisUI type in DI and game code.
 */
open class MetaColorPicker @JvmOverloads constructor(
	title: String = "Pick a color",
	allowAlphaEdit: Boolean = false,
	listener: MetaColorPickerListener? = null,
) : ColorPicker(title, toListenerAdapter(listener)) {
	init {
		isAllowAlphaEdit = allowAlphaEdit
	}

	var metaListener: MetaColorPickerListener? = listener
		set(value) {
			field = value
			setListener(toListenerAdapter(value))
		}
}

/**
 * Listener wrapper with stable defaults for color picker callbacks.
 */
interface MetaColorPickerListener {
	fun changed(newColor: Color) {}
	fun finished(newColor: Color) = changed(newColor)
	fun canceled(oldColor: Color) {}
	fun reset(newColor: Color, oldColor: Color) {}
}

private fun toListenerAdapter(listener: MetaColorPickerListener?): ColorPickerListener? {
	return listener?.let { adapter ->
		object : ColorPickerAdapter() {
			override fun changed(newColor: Color) {
				adapter.changed(newColor)
			}

			override fun finished(newColor: Color) {
				adapter.finished(newColor)
			}

			override fun canceled(oldColor: Color) {
				adapter.canceled(oldColor)
			}

			override fun reset(newColor: Color, oldColor: Color) {
				adapter.reset(newColor, oldColor)
			}
		}
	}
}
