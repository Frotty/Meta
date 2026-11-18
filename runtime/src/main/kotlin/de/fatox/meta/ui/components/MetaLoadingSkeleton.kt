package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin

/**
 * A non-interactive placeholder for content that is still loading. Compose blocks with Meta layout widgets
 * to hold the space of the expected content. A soft highlight sweeps across each block from left to right.
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
	private val highlight = MetaSkin.skin().getDrawable(MetaSkin.LOADING_SKELETON_HIGHLIGHT) as TextureRegionDrawable
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
		val oldColor = batch.packedColor
		val red = tint.r * color.r
		val green = tint.g * color.g
		val blue = tint.b * color.b
		val alpha = tint.a * color.a * parentAlpha
		batch.setColor(red, green, blue, alpha * 0.18f)
		fill.draw(batch, x, y, width, height)

		if (animated && phase > 0f) {
			val bandWidth = width * 0.72f
			val bandStart = (width + bandWidth) * phase - bandWidth
			val left = bandStart.coerceAtLeast(0f)
			val right = (bandStart + bandWidth).coerceAtMost(width)
			if (right > left) {
				val region = highlight.region
				val texture = region.texture
				// Sample texel centers to keep the clipped band inside the packed gradient.
				val halfTexel = 0.5f / texture.width
				val u = region.u + halfTexel
				val span = region.u2 - halfTexel - u
				val v = (region.v + region.v2) * 0.5f
				batch.setColor(red, green, blue, alpha * 0.38f)
				batch.draw(texture, x + left, y, right - left, height,
					u + span * (left - bandStart) / bandWidth, v,
					u + span * (right - bandStart) / bandWidth, v)
			}
		}
		batch.packedColor = oldColor
	}
}
