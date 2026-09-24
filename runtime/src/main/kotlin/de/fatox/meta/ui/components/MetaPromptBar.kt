package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.FloatArray
import com.badlogic.gdx.utils.IntArray
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType
import kotlin.math.max
import kotlin.math.min

/**
 * A row of button prompts on a floating pill - the "[A] Select  [B] Back" strip along the bottom of a game screen.
 *
 * <p>The pill is rounded all round because it floats above the screen edge; the edge-docked [MetaBottomBar] has a
 * flat bottom that reads as clipped the moment it is lifted off the edge, which is what games did with it. Spacing
 * between prompts and the pill's padding are both taken from the font size, so a prompt bar scaled up for a TV is the
 * same bar, not a bigger font in the same box.
 *
 * <p>Never wider than [widthLimit] - by default a little less than the stage. When the prompts do not fit on one row
 * they wrap onto more, each row centred, rather than running off the screen: a longer translation or one more action
 * is exactly how a single row stops fitting. Laid out by hand rather than through a wrapping flex box, because a pill
 * has to know its own height from its own width in one pass - it is the thing whose size its parent is asking for.
 */
class MetaPromptBar @JvmOverloads constructor(
	private var fontSize: Int = MetaType.BODY,
	private val labelColor: Color = MetaColor.TEXT_MUTED,
) : WidgetGroup() {
	/** One prompt of the bar: the glyphs and what they do. */
	class Entry(val glyphs: List<MetaInputGlyph>, val label: String)

	/** The widest the bar may be. NaN, the default, means [STAGE_FRACTION] of the stage it is on. */
	var widthLimit: Float = Float.NaN
		set(value) {
			field = value
			invalidateHierarchy()
		}

	private val background: Drawable = MetaSkin.skin().getDrawable(MetaSkin.PROMPT_BAR)
	private val prompts = ArrayList<MetaInputPrompt>()
	/** Index of the first prompt of each row, then one past the last. Scratch; grows with the prompt count only. */
	private val rowStarts = IntArray()
	private val rowWidths = FloatArray()

	init {
		touchable = Touchable.disabled
	}

	/** Replaces the prompts. Cheap enough to call on every change of controller family or language. */
	fun setEntries(next: List<Entry>) {
		clearChildren()
		prompts.clear()
		for (index in next.indices) {
			val prompt = MetaInputPrompt(next[index].glyphs, next[index].label, fontSize, labelColor)
			prompts.add(prompt)
			addActor(prompt)
		}
		invalidateHierarchy()
	}

	fun setFontSize(size: Int) {
		if (size == fontSize) return
		fontSize = size
		for (index in prompts.indices) prompts[index].setFontSize(size)
		invalidateHierarchy()
	}

	private val padX: Float get() = fontSize * 0.9f
	private val padY: Float get() = fontSize * 0.45f
	private val gapX: Float get() = fontSize * 1.3f
	private val gapY: Float get() = fontSize * 0.4f

	private fun limit(): Float {
		if (!widthLimit.isNaN()) return widthLimit
		val stageWidth = stage?.width ?: return Float.POSITIVE_INFINITY
		return stageWidth * STAGE_FRACTION
	}

	/** Breaks the prompts into rows no wider than [inner]; returns the row count. A prompt wider than that gets a row. */
	private fun breakRows(inner: Float): Int {
		rowStarts.clear()
		rowWidths.clear()
		if (prompts.isEmpty()) return 0
		rowStarts.add(0)
		var rowWidth = 0f
		for (index in prompts.indices) {
			val w = prompts[index].prefWidth
			val next = if (index == rowStarts.peek()) w else rowWidth + gapX + w
			if (index != rowStarts.peek() && next > inner) {
				rowWidths.add(rowWidth)
				rowStarts.add(index)
				rowWidth = w
			} else {
				rowWidth = next
			}
		}
		rowWidths.add(rowWidth)
		rowStarts.add(prompts.size)
		return rowWidths.size
	}

	private fun rowHeight(): Float {
		var h = 0f
		for (index in prompts.indices) h = max(h, prompts[index].prefHeight)
		return h
	}

	override fun getPrefWidth(): Float {
		val rows = breakRows(limit() - padX * 2f)
		var widest = 0f
		for (row in 0 until rows) widest = max(widest, rowWidths[row])
		return min(widest + padX * 2f, limit())
	}

	override fun getPrefHeight(): Float {
		val rows = breakRows(limit() - padX * 2f)
		if (rows == 0) return padY * 2f
		return rows * rowHeight() + (rows - 1) * gapY + padY * 2f
	}

	override fun layout() {
		val rows = breakRows(width - padX * 2f)
		val rowH = rowHeight()
		// Rows top-down, as a reader expects the first actions first.
		var top = height - padY
		for (row in 0 until rows) {
			var cursor = (width - rowWidths[row]) * 0.5f
			val y = top - rowH
			for (index in rowStarts[row] until rowStarts[row + 1]) {
				val prompt = prompts[index]
				val w = prompt.prefWidth
				prompt.setBounds(cursor, y, w, rowH)
				cursor += w + gapX
			}
			top = y - gapY
		}
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		validate()
		val previous = batch.packedColor
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
		background.draw(batch, x, y, width, height)
		batch.packedColor = previous
		super.draw(batch, parentAlpha)
	}

	private companion object {
		/** How much of the stage a bar may span before it wraps: enough to keep it clear of the screen edges. */
		const val STAGE_FRACTION = 0.92f
	}
}
