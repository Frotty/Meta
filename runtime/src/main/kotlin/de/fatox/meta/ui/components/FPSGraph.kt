package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.physicalPixelsPerUnit
import de.fatox.meta.api.graphics.snapToPhysicalPixel
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.FontGenerationTracker
import de.fatox.meta.ui.FontRefreshable

/**
 * A rolling FPS graph actor: plots the last [historySize] frame-rate samples, auto-scaling vertically to the peak
 * in the buffer, and prints current/average/min FPS. Uses the Meta TTF font (not a baked glyph font) and avoids
 * per-frame allocations - the stats string is rebuilt into a reused buffer only when a displayed value changes.
 *
 * Debug/dev tooling: it ends and restarts the scene2d [Batch] to draw lines with a [ShapeRenderer], so keep it on
 * its own (small) actor. Call [dispose] when done to release the [ShapeRenderer].
 */
class FPSGraph(
	width: Float,
	height: Float,
	private val historySize: Int,
) : Actor(), FontRefreshable {

	private val fontProvider: FontProvider by lazyInject()
	private val fontTracker = FontGenerationTracker()

	// Fetched lazily on first draw and dropped on [refreshFont], so a UI-scale change can't leave it stale/disposed.
	private var font: BitmapFont? = null

	private val fpsHistory = FloatArray(historySize)
	private var currentIndex = 0
	private var samplesCount = 0L

	private val shapeRenderer = ShapeRenderer()

	// Reused buffer for the stats line; only rewritten when the integer-rounded values change (no per-frame alloc).
	private val statsText = StringBuilder(48)
	private var lastCurrent = -1
	private var lastAvgTenths = -1
	private var lastMinTenths = -1

	init {
		setSize(width, height)
	}

	override fun act(delta: Float) {
		super.act(delta)
		fpsHistory[currentIndex] = Gdx.graphics.framesPerSecond.toFloat()
		currentIndex = (currentIndex + 1) % historySize
		if (samplesCount < Long.MAX_VALUE) samplesCount++
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		val validCount = minOf(samplesCount, historySize.toLong()).toInt()
		if (validCount <= 0) return

		val oldestIndex = (currentIndex - validCount + historySize) % historySize
		var sum = 0f
		var minFps = Float.MAX_VALUE
		var maxFps = Float.MIN_VALUE
		for (i in 0 until validCount) {
			val fps = fpsHistory[(oldestIndex + i) % historySize]
			sum += fps
			if (fps < minFps) minFps = fps
			if (fps > maxFps) maxFps = fps
		}
		val averageFps = sum / validCount

		batch.end()
		shapeRenderer.projectionMatrix = stage.camera.combined
		shapeRenderer.begin(ShapeType.Line)
		shapeRenderer.color = Color.GREEN
		if (validCount > 1 && maxFps > 0f) {
			val step = width / (validCount - 1)
			for (i in 0 until validCount - 1) {
				val x1 = x + step * i
				val x2 = x + step * (i + 1)
				val y1 = y + (fpsHistory[(oldestIndex + i) % historySize] / maxFps) * height
				val y2 = y + (fpsHistory[(oldestIndex + i + 1) % historySize] / maxFps) * height
				shapeRenderer.line(x1, y1, x2, y2)
			}
		}
		shapeRenderer.end()
		batch.begin()

		updateStatsText(Gdx.graphics.framesPerSecond, averageFps, minFps)
		val font = this.font ?: fontProvider.getFont(FONT_SIZE, FontType.MONO).also {
			this.font = it
			fontTracker.markFresh()
		}
		font.color = Color.WHITE
		val pixelsPerUnit = font.physicalPixelsPerUnit()
		font.draw(
			batch,
			statsText,
			snapToPhysicalPixel(x, pixelsPerUnit),
			snapToPhysicalPixel(y + height + font.lineHeight, pixelsPerUnit),
		)
	}

	override fun refreshFont() {
		font = null
	}

	/** Self-heal on (re)attach: a graph that was detached during a UI-scale change holds a disposed font. */
	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) fontTracker.refreshIfStale(this)
	}

	private fun updateStatsText(current: Int, average: Float, min: Float) {
		val avgTenths = (average * 10).toInt()
		val minTenths = (min * 10).toInt()
		if (current == lastCurrent && avgTenths == lastAvgTenths && minTenths == lastMinTenths) return
		lastCurrent = current
		lastAvgTenths = avgTenths
		lastMinTenths = minTenths
		statsText.setLength(0)
		statsText.append("Current: ").append(current)
			.append(" | Avg: ").appendTenths(avgTenths)
			.append(" | Min: ").appendTenths(minTenths)
	}

	/** Appends a value held as tenths (e.g. 423 -> "42.3") without allocating. */
	private fun StringBuilder.appendTenths(tenths: Int): StringBuilder = append(tenths / 10).append('.').append(tenths % 10)

	fun dispose() {
		shapeRenderer.dispose()
	}

	private companion object {
		const val FONT_SIZE = 14
	}
}
