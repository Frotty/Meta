package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.ui.MetaSpacing
import kotlin.math.floor
import kotlin.math.max

/**
 * Responsive equal-track grid similar to CSS `repeat(auto-fit, minmax(minColumnWidth, 1fr))`.
 *
 * Items auto-flow left-to-right, may span columns, and collapse into fewer columns as the assigned width narrows.
 * This is the table-like choice for cards and forms that need shared tracks without a fixed desktop-only column count.
 * Steady-state measurement/layout is allocation-free; scratch buffers grow only with the child count.
 */
class MetaGrid(
	minColumnWidth: Float,
	maxColumns: Int = Int.MAX_VALUE,
	columnGap: Float = MetaSpacing.SM,
	rowGap: Float = MetaSpacing.SM,
	horizontalAlign: MetaFlexAlign = MetaFlexAlign.STRETCH,
	verticalAlign: MetaFlexAlign = MetaFlexAlign.START,
) : WidgetGroup() {
	private data class ItemSpec(
		val columnSpan: Int,
		val basisHeight: Float?,
		val basisWidth: Float?,
	)

	private val itemSpecs = ObjectMap<Actor, ItemSpec>()
	private var capacity = 0
	private var itemColumn = IntArray(0)
	private var itemRow = IntArray(0)
	private var itemWidth = FloatArray(0)
	private var itemHeight = FloatArray(0)
	private var rowHeight = FloatArray(0)
	private var rowTop = FloatArray(0)
	private var measuredWidth = Float.NaN
	private var measuredRows = 0

	var minColumnWidth: Float = checkedPositive(minColumnWidth, "Grid minimum column width")
		set(value) {
			field = checkedPositive(value, "Grid minimum column width")
			invalidateHierarchy()
		}
	var maxColumns: Int = maxColumns
		set(value) {
			require(value > 0) { "Grid maximum columns must be positive" }
			field = value
			invalidateHierarchy()
		}
	var columnGap: Float = checkedNonNegative(columnGap, "Grid column gap")
		set(value) {
			field = checkedNonNegative(value, "Grid column gap")
			invalidateHierarchy()
		}
	var rowGap: Float = checkedNonNegative(rowGap, "Grid row gap")
		set(value) {
			field = checkedNonNegative(value, "Grid row gap")
			invalidateHierarchy()
		}
	var horizontalAlign: MetaFlexAlign = horizontalAlign
		set(value) {
			field = value
			invalidate()
		}
	var verticalAlign: MetaFlexAlign = verticalAlign
		set(value) {
			field = value
			invalidate()
		}

	init {
		require(maxColumns > 0) { "Grid maximum columns must be positive" }
	}

	fun addItem(
		actor: Actor,
		columnSpan: Int = 1,
		basisHeight: Float? = null,
		basisWidth: Float? = null,
	): MetaGrid = apply {
		configure(actor, columnSpan, basisHeight, basisWidth)
		addActor(actor)
		invalidateHierarchy()
	}

	fun configure(
		actor: Actor,
		columnSpan: Int = 1,
		basisHeight: Float? = null,
		basisWidth: Float? = null,
	): MetaGrid = apply {
		require(columnSpan > 0) { "Grid column span must be positive" }
		if (basisHeight != null) checkedNonNegative(basisHeight, "Grid item height")
		if (basisWidth != null) checkedNonNegative(basisWidth, "Grid item width")
		val resolvedHeight = basisHeight ?: if (actor is Layout) null else actor.height
		val resolvedWidth = basisWidth ?: if (actor is Layout) null else actor.width
		itemSpecs.put(actor, ItemSpec(columnSpan, resolvedHeight, resolvedWidth))
		invalidateHierarchy()
	}

	override fun removeActor(actor: Actor): Boolean {
		itemSpecs.remove(actor)
		return super.removeActor(actor).also { if (it) invalidateHierarchy() }
	}

	override fun clearChildren() {
		itemSpecs.clear()
		super.clearChildren()
		invalidateHierarchy()
	}

	override fun layout() {
		val columns = prepare(width)
		var top = height
		for (row in 0 until measuredRows) {
			rowTop[row] = top
			top -= rowHeight[row] + rowGap
		}
		val trackWidth = trackWidth(width, columns)
		for (index in 0 until children.size) {
			val actor = children[index]
			val cellX = itemColumn[index] * (trackWidth + columnGap)
			val cellWidth = itemWidth[index]
			val cellHeight = rowHeight[itemRow[index]]
			val actorWidth = assignedWidth(actor, cellWidth)
			val actorHeight = if (verticalAlign == MetaFlexAlign.STRETCH) cellHeight else itemHeight[index].coerceAtMost(cellHeight)
			val xOffset = alignmentOffset(horizontalAlign, cellWidth - actorWidth)
			val yOffset = alignmentOffset(verticalAlign, cellHeight - actorHeight)
			actor.setBounds(
				cellX + xOffset,
				rowTop[itemRow[index]] - actorHeight - yOffset,
				actorWidth,
				actorHeight,
			)
			if (actor is Layout) actor.validate()
		}
	}

	override fun sizeChanged() {
		super.sizeChanged()
		if (width != measuredWidth) {
			measuredWidth = width
			invalidateHierarchy()
		}
	}

	override fun getMinWidth(): Float = minColumnWidth

	override fun getMinHeight(): Float {
		var minimum = 0f
		for (index in 0 until children.size) minimum = max(minimum, minimumHeight(children[index]))
		return minimum
	}

	override fun getPrefWidth(): Float = minColumnWidth

	override fun getPrefHeight(): Float {
		prepare(width.coerceAtLeast(minColumnWidth))
		var total = rowGap * max(0, measuredRows - 1)
		for (row in 0 until measuredRows) total += rowHeight[row]
		return total
	}

	fun resolvedColumns(): Int = columnsFor(width.coerceAtLeast(minColumnWidth))

	private fun prepare(availableWidth: Float): Int {
		ensureCapacity(children.size)
		val columns = columnsFor(availableWidth)
		val trackWidth = trackWidth(availableWidth, columns)
		for (row in 0 until children.size) rowHeight[row] = 0f
		var row = 0
		var column = 0
		for (index in 0 until children.size) {
			val actor = children[index]
			val span = (itemSpecs[actor]?.columnSpan ?: 1).coerceAtMost(columns)
			if (column + span > columns) {
				row++
				column = 0
			}
			val cellWidth = trackWidth * span + columnGap * (span - 1)
			itemColumn[index] = column
			itemRow[index] = row
			itemWidth[index] = cellWidth
			itemHeight[index] = itemSpecs[actor]?.basisHeight
				?: preferredHeightAtWidth(actor, assignedWidth(actor, cellWidth))
			rowHeight[row] = max(rowHeight[row], itemHeight[index])
			column += span
			if (column == columns && index < children.size - 1) {
				row++
				column = 0
			}
		}
		measuredRows = if (children.size == 0) 0 else row + 1
		return columns
	}

	private fun columnsFor(availableWidth: Float): Int {
		val possible = floor((availableWidth + columnGap) / (minColumnWidth + columnGap)).toInt()
		return possible.coerceIn(1, maxColumns)
	}

	private fun trackWidth(availableWidth: Float, columns: Int): Float =
		((availableWidth - columnGap * (columns - 1)) / columns).coerceAtLeast(0f)

	private fun preferredHeightAtWidth(actor: Actor, assignedWidth: Float): Float {
		val layout = actor as? Layout ?: return actor.height
		if (actor.width != assignedWidth) {
			actor.width = assignedWidth
			layout.invalidate()
		}
		return layout.prefHeight
	}

	private fun assignedWidth(actor: Actor, cellWidth: Float): Float = if (horizontalAlign == MetaFlexAlign.STRETCH) {
		cellWidth
	} else {
		(itemSpecs[actor]?.basisWidth ?: preferredWidth(actor)).coerceAtMost(cellWidth)
	}

	private fun ensureCapacity(required: Int) {
		if (required <= capacity) return
		capacity = max(8, max(required, capacity * 2))
		itemColumn = itemColumn.copyOf(capacity)
		itemRow = itemRow.copyOf(capacity)
		itemWidth = itemWidth.copyOf(capacity)
		itemHeight = itemHeight.copyOf(capacity)
		rowHeight = rowHeight.copyOf(capacity)
		rowTop = rowTop.copyOf(capacity)
	}

	private fun preferredWidth(actor: Actor): Float = (actor as? Layout)?.prefWidth ?: actor.width
	private fun minimumHeight(actor: Actor): Float = (actor as? Layout)?.minHeight ?: actor.height

	private companion object {
		fun alignmentOffset(align: MetaFlexAlign, free: Float): Float = when (align) {
			MetaFlexAlign.START, MetaFlexAlign.STRETCH -> 0f
			MetaFlexAlign.CENTER -> free * 0.5f
			MetaFlexAlign.END -> free
		}

		fun checkedPositive(value: Float, label: String): Float {
			require(value.isFinite() && value > 0f) { "$label must be finite and positive" }
			return value
		}

		fun checkedNonNegative(value: Float, label: String): Float {
			require(value.isFinite() && value >= 0f) { "$label must be finite and not negative" }
			return value
		}
	}
}
