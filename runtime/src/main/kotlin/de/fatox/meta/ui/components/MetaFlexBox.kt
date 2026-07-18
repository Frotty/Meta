package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.ui.MetaSpacing
import kotlin.math.max

enum class MetaFlexDirection { ROW, COLUMN }

enum class MetaFlexJustify { START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY }

enum class MetaFlexAlign { START, CENTER, END, STRETCH }

/**
 * Scene2D flex-style layout with optional wrapping, variable item bases, main-axis growth and row/column direction.
 * Wrapped boxes request only one item's main-axis size so parents may assign the available width/height responsively.
 * Steady-state measurement and layout are allocation-free; scratch buffers grow only when the child count grows.
 */
open class MetaFlexBox(
	direction: MetaFlexDirection = MetaFlexDirection.ROW,
	wrap: Boolean = false,
	mainGap: Float = MetaSpacing.XS,
	crossGap: Float = mainGap,
	justify: MetaFlexJustify = MetaFlexJustify.START,
	align: MetaFlexAlign = MetaFlexAlign.START,
) : WidgetGroup() {
	private data class ItemSpec(
		val basisWidth: Float?,
		val basisHeight: Float?,
		val grow: Float,
		val shrink: Float,
		val minWidth: Float?,
		val minHeight: Float?,
	)

	private val itemSpecs = ObjectMap<Actor, ItemSpec>()
	private var measuredMainSize = Float.NaN
	private var itemCapacity = 0
	private var itemMain = FloatArray(0)
	private var itemCross = FloatArray(0)
	private var itemGrow = FloatArray(0)
	private var itemShrink = FloatArray(0)
	private var itemMinMain = FloatArray(0)
	private var itemLayoutMain = FloatArray(0)
	private var lineCapacity = 0
	private var lineStart = IntArray(0)
	private var lineCount = IntArray(0)
	private var lineMain = FloatArray(0)
	private var lineCross = FloatArray(0)
	private var lineGrow = FloatArray(0)

	var direction: MetaFlexDirection = direction
		set(value) {
			if (field == value) return
			field = value
			measuredMainSize = Float.NaN
			invalidateHierarchy()
		}
	var wrap: Boolean = wrap
		set(value) {
			if (field == value) return
			field = value
			invalidateHierarchy()
		}
	var mainGap: Float = checkedNonNegative(mainGap, "Flex main gap")
		set(value) {
			field = checkedNonNegative(value, "Flex main gap")
			invalidateHierarchy()
		}
	var crossGap: Float = checkedNonNegative(crossGap, "Flex cross gap")
		set(value) {
			field = checkedNonNegative(value, "Flex cross gap")
			invalidateHierarchy()
		}
	var justify: MetaFlexJustify = justify
		set(value) {
			field = value
			invalidate()
		}
	var align: MetaFlexAlign = align
		set(value) {
			field = value
			invalidate()
		}

	fun addItem(
		actor: Actor,
		basisWidth: Float? = null,
		basisHeight: Float? = null,
		grow: Float = 0f,
		shrink: Float = 1f,
		minWidth: Float? = null,
		minHeight: Float? = null,
	): MetaFlexBox = apply {
		configure(actor, basisWidth, basisHeight, grow, shrink, minWidth, minHeight)
		addActor(actor)
		invalidateHierarchy()
	}

	fun configure(
		actor: Actor,
		basisWidth: Float? = null,
		basisHeight: Float? = null,
		grow: Float = 0f,
		shrink: Float = 1f,
		minWidth: Float? = null,
		minHeight: Float? = null,
	): MetaFlexBox = apply {
		if (basisWidth != null) checkedNonNegative(basisWidth, "Flex item width")
		if (basisHeight != null) checkedNonNegative(basisHeight, "Flex item height")
		checkedNonNegative(grow, "Flex grow")
		checkedNonNegative(shrink, "Flex shrink")
		if (minWidth != null) checkedNonNegative(minWidth, "Flex item minimum width")
		if (minHeight != null) checkedNonNegative(minHeight, "Flex item minimum height")
		val resolvedWidth = basisWidth ?: if (actor is Layout) null else actor.width
		val resolvedHeight = basisHeight ?: if (actor is Layout) null else actor.height
		itemSpecs.put(actor, ItemSpec(resolvedWidth, resolvedHeight, grow, shrink, minWidth, minHeight))
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
		val mainAvailable = if (direction == MetaFlexDirection.ROW) width else height
		val crossAvailable = if (direction == MetaFlexDirection.ROW) height else width
		val lines = buildLines(mainAvailable)
		var crossCursor = 0f
		for (line in 0 until lines) {
			val count = lineCount[line]
			val layoutCross = if (!wrap) crossAvailable else lineCross[line]
			val usedMain = resolveLineMainSizes(line, mainAvailable)
			val free = (mainAvailable - usedMain).coerceAtLeast(0f)
			var offset = 0f
			var gap = mainGap
			if (lineGrow[line] == 0f) {
				when (justify) {
					MetaFlexJustify.START -> Unit
					MetaFlexJustify.CENTER -> offset = free * 0.5f
					MetaFlexJustify.END -> offset = free
					MetaFlexJustify.SPACE_BETWEEN -> if (count > 1) gap += free / (count - 1)
					MetaFlexJustify.SPACE_AROUND -> {
						val space = free / count
						offset = space * 0.5f
						gap += space
					}
					MetaFlexJustify.SPACE_EVENLY -> {
						val space = free / (count + 1)
						offset = space
						gap += space
					}
				}
			}
			var mainCursor = offset
			val end = lineStart[line] + count
			for (index in lineStart[line] until end) {
				val actor = children[index]
				val actorMain = itemLayoutMain[index]
				val actorCross = if (align == MetaFlexAlign.STRETCH) layoutCross else itemCross[index]
				val crossOffset = when (align) {
					MetaFlexAlign.START, MetaFlexAlign.STRETCH -> 0f
					MetaFlexAlign.CENTER -> (layoutCross - actorCross) * 0.5f
					MetaFlexAlign.END -> layoutCross - actorCross
				}
				if (direction == MetaFlexDirection.ROW) {
					actor.setBounds(mainCursor, height - crossCursor - crossOffset - actorCross, actorMain, actorCross)
				} else {
					actor.setBounds(crossCursor + crossOffset, height - mainCursor - actorMain, actorCross, actorMain)
				}
				if (actor is Layout) actor.validate()
				mainCursor += actorMain + gap
			}
			crossCursor += layoutCross + crossGap
		}
	}

	override fun sizeChanged() {
		super.sizeChanged()
		val mainSize = if (direction == MetaFlexDirection.ROW) width else height
		if (mainSize != measuredMainSize) {
			measuredMainSize = mainSize
			invalidateHierarchy()
		}
	}

	override fun getMinWidth(): Float = minimumSize(horizontal = true)

	override fun getMinHeight(): Float = minimumSize(horizontal = false)

	override fun getPrefWidth(): Float = preferredSize(horizontal = true)

	override fun getPrefHeight(): Float = preferredSize(horizontal = false)

	private fun preferredSize(horizontal: Boolean): Float {
		if (children.size == 0) return 0f
		val rowDirection = direction == MetaFlexDirection.ROW
		if (!wrap) {
			return if (horizontal == rowDirection) {
				axisTotal(horizontal) + mainGap * max(0, children.size - 1)
			} else {
				buildLines(if (rowDirection) width else height)
				lineCross[0]
			}
		}
		if (horizontal == rowDirection) return axisMaximum(horizontal)
		val mainAvailable = if (rowDirection) width else height
		val lines = buildLines(mainAvailable)
		var total = crossGap * max(0, lines - 1)
		for (line in 0 until lines) total += lineCross[line]
		return total
	}

	private fun buildLines(mainAvailable: Float): Int {
		measureItems()
		if (children.size == 0) return 0
		ensureLineCapacity(children.size)
		var largestMain = 0f
		for (index in 0 until children.size) largestMain = max(largestMain, itemMain[index])
		val limit = if (wrap) mainAvailable.coerceAtLeast(largestMain) else Float.POSITIVE_INFINITY
		var lines = 1
		resetLine(0, 0)
		for (index in 0 until children.size) {
			var line = lines - 1
			val nextMain = lineMain[line] + (if (lineCount[line] == 0) 0f else mainGap) + itemMain[index]
			if (wrap && lineCount[line] > 0 && nextMain > limit) {
				line = lines++
				resetLine(line, index)
			}
			if (lineCount[line] > 0) lineMain[line] += mainGap
			lineCount[line]++
			lineMain[line] += itemMain[index]
			lineCross[line] = max(lineCross[line], itemCross[index])
			lineGrow[line] += itemGrow[index]
		}
		measureResponsiveCrossSizes(lines, mainAvailable)
		return lines
	}

	private fun measureResponsiveCrossSizes(lines: Int, mainAvailable: Float) {
		for (line in 0 until lines) {
			lineCross[line] = 0f
			resolveLineMainSizes(line, mainAvailable)
			val end = lineStart[line] + lineCount[line]
			for (index in lineStart[line] until end) {
				val actor = children[index]
				val actorMain = itemLayoutMain[index]
				itemCross[index] = responsiveCrossSize(actor, actorMain)
				lineCross[line] = max(lineCross[line], itemCross[index])
			}
		}
	}

	private fun responsiveCrossSize(actor: Actor, assignedMain: Float): Float {
		val spec = itemSpecs[actor]
		if (direction == MetaFlexDirection.ROW) {
			spec?.basisHeight?.let { return it }
			val layout = actor as? Layout ?: return actor.height
			if (actor.width != assignedMain) {
				actor.width = assignedMain
				layout.invalidate()
			}
			return layout.prefHeight
		}
		spec?.basisWidth?.let { return it }
		val layout = actor as? Layout ?: return actor.width
		if (actor.height != assignedMain) {
			actor.height = assignedMain
			layout.invalidate()
		}
		return layout.prefWidth
	}

	private fun measureItems() {
		ensureItemCapacity(children.size)
		for (index in 0 until children.size) {
			val actor = children[index]
			val spec = itemSpecs[actor]
			val actorWidth = spec?.basisWidth ?: preferredWidth(actor)
			val actorHeight = spec?.basisHeight ?: preferredHeight(actor)
			if (direction == MetaFlexDirection.ROW) {
				itemMain[index] = actorWidth
				itemCross[index] = actorHeight
				itemMinMain[index] = spec?.minWidth ?: minimumWidth(actor)
			} else {
				itemMain[index] = actorHeight
				itemCross[index] = actorWidth
				itemMinMain[index] = spec?.minHeight ?: minimumHeight(actor)
			}
			itemGrow[index] = spec?.grow ?: 0f
			itemShrink[index] = spec?.shrink ?: 1f
			if (itemMinMain[index] > itemMain[index]) itemMinMain[index] = itemMain[index]
		}
	}

	private fun resolveLineMainSizes(line: Int, mainAvailable: Float): Float {
		val start = lineStart[line]
		val end = start + lineCount[line]
		val gaps = mainGap * max(0, lineCount[line] - 1)
		var itemTotal = 0f
		for (index in start until end) {
			itemLayoutMain[index] = itemMain[index]
			itemTotal += itemMain[index]
		}
		val itemAvailable = (mainAvailable - gaps).coerceAtLeast(0f)
		if (itemTotal < itemAvailable && lineGrow[line] > 0f) {
			val growUnit = (itemAvailable - itemTotal) / lineGrow[line]
			for (index in start until end) itemLayoutMain[index] += itemGrow[index] * growUnit
			itemTotal = itemAvailable
		} else if (itemTotal > itemAvailable) {
			var remaining = itemTotal - itemAvailable
			while (remaining > SIZE_EPSILON) {
				var weight = 0f
				for (index in start until end) {
					if (itemLayoutMain[index] > itemMinMain[index] + SIZE_EPSILON) {
						weight += itemShrink[index] * itemMain[index]
					}
				}
				if (weight <= SIZE_EPSILON) break
				var removed = 0f
				for (index in start until end) {
					if (itemLayoutMain[index] <= itemMinMain[index] + SIZE_EPSILON) continue
					val share = remaining * (itemShrink[index] * itemMain[index]) / weight
					val next = (itemLayoutMain[index] - share).coerceAtLeast(itemMinMain[index])
					removed += itemLayoutMain[index] - next
					itemLayoutMain[index] = next
				}
				if (removed <= SIZE_EPSILON) break
				remaining -= removed
			}
			itemTotal = 0f
			for (index in start until end) itemTotal += itemLayoutMain[index]
		}
		return itemTotal + gaps
	}

	private fun axisTotal(horizontal: Boolean): Float {
		var total = 0f
		for (index in 0 until children.size) total += axisSize(children[index], horizontal, useMinimum = false)
		return total
	}

	private fun axisMaximum(horizontal: Boolean, useMinimum: Boolean = false): Float {
		var maximum = 0f
		for (index in 0 until children.size) {
			maximum = max(maximum, axisSize(children[index], horizontal, useMinimum))
		}
		return maximum
	}

	private fun axisSize(actor: Actor, horizontal: Boolean, useMinimum: Boolean): Float {
		val spec = itemSpecs[actor]
		return if (horizontal) {
			if (useMinimum) spec?.minWidth ?: minimumWidth(actor) else spec?.basisWidth ?: preferredWidth(actor)
		} else {
			if (useMinimum) spec?.minHeight ?: minimumHeight(actor) else spec?.basisHeight ?: preferredHeight(actor)
		}
	}

	private fun minimumSize(horizontal: Boolean): Float {
		if (children.size == 0) return 0f
		val mainAxis = horizontal == (direction == MetaFlexDirection.ROW)
		if (!mainAxis) return axisMaximum(horizontal, useMinimum = true)
		if (wrap) return axisMaximum(horizontal, useMinimum = true)
		return axisTotalMinimum(horizontal) + mainGap * max(0, children.size - 1)
	}

	private fun axisTotalMinimum(horizontal: Boolean): Float {
		var total = 0f
		for (index in 0 until children.size) total += axisSize(children[index], horizontal, useMinimum = true)
		return total
	}

	private fun resetLine(line: Int, start: Int) {
		lineStart[line] = start
		lineCount[line] = 0
		lineMain[line] = 0f
		lineCross[line] = 0f
		lineGrow[line] = 0f
	}

	private fun ensureItemCapacity(required: Int) {
		if (required <= itemCapacity) return
		itemCapacity = max(8, max(required, itemCapacity * 2))
		itemMain = itemMain.copyOf(itemCapacity)
		itemCross = itemCross.copyOf(itemCapacity)
		itemGrow = itemGrow.copyOf(itemCapacity)
		itemShrink = itemShrink.copyOf(itemCapacity)
		itemMinMain = itemMinMain.copyOf(itemCapacity)
		itemLayoutMain = itemLayoutMain.copyOf(itemCapacity)
	}

	private fun ensureLineCapacity(required: Int) {
		if (required <= lineCapacity) return
		lineCapacity = max(8, max(required, lineCapacity * 2))
		lineStart = lineStart.copyOf(lineCapacity)
		lineCount = lineCount.copyOf(lineCapacity)
		lineMain = lineMain.copyOf(lineCapacity)
		lineCross = lineCross.copyOf(lineCapacity)
		lineGrow = lineGrow.copyOf(lineCapacity)
	}

	private fun preferredWidth(actor: Actor): Float = (actor as? Layout)?.prefWidth ?: actor.width
	private fun preferredHeight(actor: Actor): Float = (actor as? Layout)?.prefHeight ?: actor.height
	private fun minimumWidth(actor: Actor): Float = (actor as? Layout)?.minWidth ?: actor.width
	private fun minimumHeight(actor: Actor): Float = (actor as? Layout)?.minHeight ?: actor.height

	private companion object {
		const val SIZE_EPSILON = 0.001f
		fun checkedNonNegative(value: Float, label: String): Float {
			require(value.isFinite() && value >= 0f) { "$label must be finite and not negative" }
			return value
		}
	}
}
