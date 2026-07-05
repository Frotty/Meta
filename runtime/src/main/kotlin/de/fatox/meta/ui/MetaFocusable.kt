package de.fatox.meta.ui

/**
 * Implemented by Meta widgets that render keyboard/controller focus as part of their own style. The renderer still
 * keeps the focused actor for non-Meta fallback drawing, but actors with this contract should not get the overlay box.
 */
interface MetaFocusable {
	val handlesMetaFocus: Boolean
		get() = true

	fun setMetaFocused(focused: Boolean)
}
