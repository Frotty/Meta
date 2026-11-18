package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin

/**
 * A non-interactive placeholder for content that is still loading. Compose blocks with Meta layout widgets
 * to hold the space of the expected content. Each block draws once using the skin atlas.
 *
 * Set [animated] to false for a static placeholder, for example when the consumer disables UI motion.
 */
class MetaLoadingSkeleton @JvmOverloads constructor(
	private val preferredWidth: Float,
	private val preferredHeight: Float,
	color: Color = MetaColor.TEXT_DISABLED,
	private val periodSeconds: Float = 1.4f,
) : Widget() {
	private val fill = MetaSkin.skin().getDrawable(MetaSkin.COLOR_FILL)
	private val tint = color.cpy()
	private var phase = 0f
	var animated: Boolean = true

	init {
		require(preferredWidth.isFinite() && preferredWidth >= 0f)
		require(preferredHeight.isFinite() && preferredHeight >= 0f)
		require(periodSeconds.isFinite() && periodSeconds > 0f)
		touchable = Touchable.disabled
		setSize(preferredWidth, preferredHeight)
	}

	override fun getPrefWidth(): Float = preferredWidth
	override fun getPrefHeight(): Float = preferredHeight

	override fun act(delta: Float) {
		super.act(delta)
		if (animated && isVisible) phase = (phase + delta / periodSeconds) % 1f
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		if (width <= 0f || height <= 0f || color.a <= 0f || parentAlpha <= 0f) return
		val opacity = if (animated) 0.23f - 0.09f * MathUtils.cos(phase * MathUtils.PI2) else 0.23f
		val oldColor = batch.packedColor
		batch.setColor(tint.r * color.r, tint.g * color.g, tint.b * color.b, tint.a * color.a * parentAlpha * opacity)
		fill.draw(batch, x, y, width, height)
		batch.packedColor = oldColor
	}
}
