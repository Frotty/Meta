package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSpacing
import kotlin.math.min

/**
 * Textureless loading indicator built from a ring of filled quads. The geometry is generated around the actor's
 * exact centre, avoiding the asymmetric bearings that make rotating font glyphs wobble.
 *
 * Call [dispose] when a manually-owned spinner is permanently discarded. Window-owned instances may be disposed
 * from their window's existing teardown hook.
 */
class MetaLoadingSpinner @JvmOverloads constructor(
	private val size: Float = 32f,
	private val thickness: Float = 3f,
	color: Color = MetaColor.ACCENT,
	private val rotationsPerSecond: Float = 0.8f,
	private val padding: Float = MetaSpacing.XS,
) : Widget() {
	val activeValue: Signal<Boolean> = signal(true)
	private val spinnerColor = color.cpy()
	private val center = Vector2()
	private val shapeRenderer = ShapeRenderer()
	private var angle = 0f
	private var disposed = false
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
		setOrigin(preferredSize * 0.5f, preferredSize * 0.5f)
	}

	override fun getPrefWidth(): Float = preferredSize
	override fun getPrefHeight(): Float = preferredSize

	override fun act(delta: Float) {
		super.act(delta)
		if (!activeValue.peek()) return
		angle = (angle - 360f * rotationsPerSecond * delta) % 360f
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		if (disposed || !activeValue.peek() || color.a <= 0f) return
		val stage = stage ?: return
		localToStageCoordinates(center.set(width * 0.5f, height * 0.5f))
		val availableRadius = (min(width, height) * 0.5f - padding).coerceAtLeast(0f)
		val outerRadius = min(size * 0.5f, availableRadius)
		val innerRadius = (outerRadius - thickness.coerceAtMost(outerRadius)).coerceAtLeast(0f)
		if (outerRadius <= 0f || innerRadius >= outerRadius) return

		batch.end()
		shapeRenderer.projectionMatrix = stage.camera.combined
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
		for (i in 0 until VISIBLE_SEGMENTS) {
			val progress = i.toFloat() / (VISIBLE_SEGMENTS - 1)
			val start = angle + i * SEGMENT_ANGLE
			val end = start + SEGMENT_ANGLE + SEAM_OVERLAP_DEGREES
			val cos0 = MathUtils.cosDeg(start)
			val sin0 = MathUtils.sinDeg(start)
			val cos1 = MathUtils.cosDeg(end)
			val sin1 = MathUtils.sinDeg(end)
			val innerX0 = center.x + cos0 * innerRadius
			val innerY0 = center.y + sin0 * innerRadius
			val outerX0 = center.x + cos0 * outerRadius
			val outerY0 = center.y + sin0 * outerRadius
			val innerX1 = center.x + cos1 * innerRadius
			val innerY1 = center.y + sin1 * innerRadius
			val outerX1 = center.x + cos1 * outerRadius
			val outerY1 = center.y + sin1 * outerRadius
			shapeRenderer.color.set(
				spinnerColor.r * color.r,
				spinnerColor.g * color.g,
				spinnerColor.b * color.b,
				spinnerColor.a * color.a * parentAlpha * (MIN_ALPHA + progress * (1f - MIN_ALPHA)),
			)
			shapeRenderer.triangle(innerX0, innerY0, outerX0, outerY0, outerX1, outerY1)
			shapeRenderer.triangle(innerX0, innerY0, outerX1, outerY1, innerX1, innerY1)
		}
		shapeRenderer.end()
		batch.begin()
	}

	fun dispose() {
		if (disposed) return
		disposed = true
		shapeRenderer.dispose()
	}

	private companion object {
		const val SEGMENTS = 48
		const val VISIBLE_SEGMENTS = 38
		const val SEGMENT_ANGLE = 360f / SEGMENTS
		const val SEAM_OVERLAP_DEGREES = 0.35f
		const val MIN_ALPHA = 0.08f
	}
}
