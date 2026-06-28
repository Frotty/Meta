package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.util.ToastManager
import com.kotcrab.vis.ui.widget.toast.Toast

/**
 * A [ToastManager] that cooperates with Meta's window/dialog layering so toasts are ALWAYS on top - above regular
 * windows, modal dialogs and the shared backdrop.
 *
 * VisUI's [ToastManager] adds its root group to the stage exactly once and never re-fronts it. Anything added to the
 * stage afterwards (a window via `UIRenderer.addActor`, a dialog brought to front, the modal backdrop) ends up on top
 * of the toast layer and hides notifications. This variant fixes that from two sides:
 *  - every [show] re-fronts the toast layer, so a toast raised while a dialog is open still appears over it, and
 *  - [de.fatox.meta.ui.MetaUIRenderer.addActor] / [MetaUiManager] call [toFront] again whenever the layering changes
 *    (a window or dialog is shown), so an already-visible toast is lifted back above the newcomer.
 *
 * All of VisUI's `show(...)` overloads funnel through `show(Toast, Float)`, so overriding that single method keeps
 * every entry point (string, table, toast) on top.
 */
class MetaToastManager(stage: Stage) : ToastManager(stage) {
	init {
		alignment = Align.bottomRight
	}

	override fun show(toast: Toast, fadeOutDelay: Float) {
		super.show(toast, fadeOutDelay)
		toFront()
	}
}
