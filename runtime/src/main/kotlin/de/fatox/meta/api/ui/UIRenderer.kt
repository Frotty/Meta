package de.fatox.meta.api.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Disposable
import com.kotcrab.vis.ui.util.ToastManager
import de.fatox.meta.reactive.Signal

interface UIRenderer : Disposable {
	fun load()
	fun addActor(actor: Actor)
	fun update()
	fun draw()
	fun resize(width: Int, height: Int)
	fun getCamera(): Camera
	fun getToastManager(): ToastManager
	fun setFocusedActor(actor: Actor?)

	/**
	 * Global UI scale factor (1.0 = one UI unit per physical pixel). Increase it on HiDPI / 4K / Retina displays so
	 * controls aren't tiny. It's a reactive [Signal]: set `uiScale.value` (e.g. from a settings slider, persisted in
	 * your config) and the whole scene2d UI re-scales live. Use `suggestedUiScale()` for a DPI-based default.
	 */
	val uiScale: Signal<Float>

	/** The UI surface size in UI units (physical pixels ÷ [uiScale]). Use these for stage layout/positioning -
	 *  NEVER `Gdx.graphics.width/height` (physical pixels), which are wrong by a factor of [uiScale]. */
	val uiWidth: Float
	val uiHeight: Float

	override fun dispose() = Unit
}
