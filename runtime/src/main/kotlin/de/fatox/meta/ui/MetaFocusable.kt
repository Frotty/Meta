package de.fatox.meta.ui

/**
 * Implemented by Meta widgets that render keyboard/controller focus as part of their own style. The renderer still
 * keeps the focused actor for non-Meta fallback drawing, but actors with this contract should not get the overlay box.
 */
interface MetaFocusable {
	val handlesMetaFocus: Boolean
		get() = true

	/**
	 * Presentation only. Restyle here; do not treat this as the notification that focus moved.
	 *
	 * It is not one. `UiControlHelper` reaches this through [MetaFocus.assign], which only
	 * [de.fatox.meta.api.ui.UIRenderer.setFocusedActor] calls - so a widget that keeps state here works against
	 * `MetaUIRenderer` and is silently inert against a renderer that does not relay it, including the layout-only one
	 * in `MetaHeadlessUi`. A consumer that wires selection to this callback gets a UI that behaves correctly when run
	 * and cannot be covered by a test at all, which is the wrong way round.
	 *
	 * Read `UiControlHelper.focusedActor` in an effect for state. It is a `ReactiveValue`, it is assigned whether or
	 * not a renderer is present, and it is the value this callback is derived from.
	 */
	fun setMetaFocused(focused: Boolean)
}
