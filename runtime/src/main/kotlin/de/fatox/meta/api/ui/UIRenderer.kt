package de.fatox.meta.api.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Disposable
import de.fatox.meta.reactive.Signal
import de.fatox.meta.ui.MetaToastManager

interface UIRenderer : Disposable {
	fun load()
	fun addActor(actor: Actor)
	fun update()
	fun draw()
	fun resize(width: Int, height: Int)
	fun getCamera(): Camera
	fun getToastManager(): MetaToastManager
	fun setFocusedActor(actor: Actor?)
	/** Cancels scene2d presses/drags before a modal takes over input. */
	fun cancelTouchFocus() = Unit

	/**
	 * Global UI scale factor (1.0 = one UI unit per physical pixel). Increase it on HiDPI / 4K / Retina displays so
	 * controls aren't tiny. It's a reactive [Signal]: set `uiScale.value` to re-layout the scene2d UI. A scale slider
	 * should apply its committed/released value rather than changing geometry during its active pointer drag. Use
	 * `suggestedUiScale()` for a DPI-based default.
	 */
	val uiScale: Signal<Float>

	/** The UI surface size in UI units (physical pixels ÷ [uiScale]). Use these for stage layout/positioning -
	 *  NEVER `Gdx.graphics.width/height` (physical pixels), which are wrong by a factor of [uiScale]. */
	val uiWidth: Float
	val uiHeight: Float

	override fun dispose() = Unit
}
