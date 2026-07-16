package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import kotlin.math.min

/**
 * A restrained open-ring loading indicator. The ring texture belongs to [MetaSkin], so each spinner is one normal
 * scene2d batch draw with no GL resource ownership, batch flush, or fragmented per-segment geometry.
 */
class MetaLoadingSpinner @JvmOverloads constructor(
	private val size: Float = 32f,
	@Suppress("unused") private val thickness: Float = 3f,
	color: Color = MetaColor.ACCENT,
	private val rotationsPerSecond: Float = 0.8f,
	private val padding: Float = MetaSpacing.XS,
) : Widget() {
	val activeValue: Signal<Boolean> = signal(true)
	private val spinnerColor = color.cpy()
	private val ring = MetaSkin.skin().getDrawable(MetaSkin.LOADING_RING) as TextureRegionDrawable
	private var angle = 0f
	private val preferredSize = size + padding * 2f

	var active: Boolean
		get() = activeValue.peek()
		set(value) {
			activeValue.value = value
		}

	init {
		require(size >= 0f) { "size must not be negative" }
		require(thickness >= 0f) { "thickness must not be negative" }
		require(padding >= 0f) { "padding must not be negative" }
		setSize(preferredSize, preferredSize)
	}

	override fun getPrefWidth(): Float = preferredSize
	override fun getPrefHeight(): Float = preferredSize

	override fun act(delta: Float) {
		super.act(delta)
		if (activeValue.peek()) angle = (angle - 360f * rotationsPerSecond * delta) % 360f
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		if (!activeValue.peek() || color.a <= 0f) return
		val diameter = min(size, (min(width, height) - padding * 2f).coerceAtLeast(0f))
		if (diameter <= 0f || thickness <= 0f) return
		val drawX = x + (width - diameter) * 0.5f
		val drawY = y + (height - diameter) * 0.5f
		val oldColor = batch.packedColor
		batch.setColor(
			spinnerColor.r * color.r,
			spinnerColor.g * color.g,
			spinnerColor.b * color.b,
			spinnerColor.a * color.a * parentAlpha,
		)
		ring.draw(batch, drawX, drawY, diameter * 0.5f, diameter * 0.5f, diameter, diameter, 1f, 1f, angle)
		batch.packedColor = oldColor
	}

	/** Retained for source compatibility; the ring is owned and disposed by [MetaSkin]. */
	fun dispose() = Unit
}
