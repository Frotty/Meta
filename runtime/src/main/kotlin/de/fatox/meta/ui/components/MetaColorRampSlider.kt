package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Disposable
import de.fatox.meta.reactive.Signal

/**
 * Reactive Meta slider over a live color ramp. The ramp is presentation-only; [slider] retains standard Meta focus,
 * keyboard/controller and pointer behavior.
 */
class MetaColorRampSlider(
	min: Float,
	max: Float,
	stepSize: Float,
	private val checkerboard: Boolean = false,
	private val colorAt: (position: Float, out: Color) -> Unit,
) : MetaStack(), Disposable {
	val slider = MetaSlider(min, max, stepSize, showTrack = false)
	val valueValue: Signal<Float> get() = slider.valueValue
	val committedValue: Signal<Float> get() = slider.committedValue
	private val ramp = MetaColorRamp(checkerboard, colorAt)

	init {
		addItem(ramp)
		addItem(slider)
	}

	override fun dispose() {
		ramp.dispose()
	}
}

private class MetaColorRamp(
	private val checkerboard: Boolean,
	private val colorAt: (position: Float, out: Color) -> Unit,
) : Widget(), Disposable {
	private val shapeRendererDelegate = lazy { ShapeRenderer() }
	private val shapeRenderer by shapeRendererDelegate
	private val origin = com.badlogic.gdx.math.Vector2()
	private val oppositeCorner = com.badlogic.gdx.math.Vector2()
	private val left = Color()
	private val right = Color()
	private var disposed = false

	init {
		touchable = Touchable.disabled
	}

	override fun getPrefHeight(): Float = RAMP_HEIGHT

	override fun draw(batch: Batch, parentAlpha: Float) {
		if (disposed || color.a <= 0f || width <= 0f || height <= 0f) return
		val stage = stage ?: return
		localToStageCoordinates(origin.set(0f, 0f))
		localToStageCoordinates(oppositeCorner.set(width, height))
		val drawWidth = oppositeCorner.x - origin.x
		val drawHeight = oppositeCorner.y - origin.y
		val verticalScale = if (height > 0f) drawHeight / height else 1f
		val rampHeight = (RAMP_HEIGHT * verticalScale).coerceAtMost(drawHeight)
		val rampY = origin.y + (drawHeight - rampHeight) * 0.5f
		val alpha = color.a * parentAlpha

		batch.end()
		Gdx.gl.glEnable(GL20.GL_BLEND)
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
		shapeRenderer.projectionMatrix = stage.camera.combined
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
		if (checkerboard) drawCheckerboard(origin.x, rampY, drawWidth, rampHeight, alpha)
		val segmentWidth = drawWidth / SEGMENTS
		for (index in 0 until SEGMENTS) {
			colorAt(index.toFloat() / SEGMENTS, left)
			colorAt((index + 1f) / SEGMENTS, right)
			left.a *= alpha
			right.a *= alpha
			shapeRenderer.rect(
				origin.x + index * segmentWidth,
				rampY,
				segmentWidth,
				rampHeight,
				left,
				right,
				right,
				left,
			)
		}
		shapeRenderer.end()
		batch.begin()
	}

	private fun drawCheckerboard(x: Float, y: Float, width: Float, height: Float, alpha: Float) {
		val columns = (width / CHECK_SIZE).toInt() + 1
		val rows = (height / CHECK_SIZE).toInt() + 1
		for (row in 0 until rows) {
			for (column in 0 until columns) {
				val cellWidth = (width - column * CHECK_SIZE).coerceIn(0f, CHECK_SIZE)
				val cellHeight = (height - row * CHECK_SIZE).coerceIn(0f, CHECK_SIZE)
				if (cellWidth <= 0f || cellHeight <= 0f) continue
				val shade = if ((row + column) and 1 == 0) CHECK_LIGHT else CHECK_DARK
				shapeRenderer.setColor(shade, shade, shade, alpha)
				shapeRenderer.rect(
					x + column * CHECK_SIZE,
					y + row * CHECK_SIZE,
					cellWidth,
					cellHeight,
				)
			}
		}
	}

	override fun dispose() {
		if (disposed) return
		disposed = true
		if (shapeRendererDelegate.isInitialized()) shapeRenderer.dispose()
	}

	private companion object {
		const val SEGMENTS = 24
		const val RAMP_HEIGHT = 16f
		const val CHECK_SIZE = 6f
		const val CHECK_LIGHT = 0.72f
		const val CHECK_DARK = 0.42f
	}
}
