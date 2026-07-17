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
class MetaFlexBox(
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
	)

	private val itemSpecs = ObjectMap<Actor, ItemSpec>()
	private var measuredMainSize = Float.NaN
	private var itemCapacity = 0
	private var itemMain = FloatArray(0)
	private var itemCross = FloatArray(0)
	private var itemGrow = FloatArray(0)
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
	): MetaFlexBox = apply {
		configure(actor, basisWidth, basisHeight, grow)
		addActor(actor)
		invalidateHierarchy()
	}

	fun configure(
		actor: Actor,
		basisWidth: Float? = null,
		basisHeight: Float? = null,
		grow: Float = 0f,
	): MetaFlexBox = apply {
		if (basisWidth != null) checkedNonNegative(basisWidth, "Flex item width")
		if (basisHeight != null) checkedNonNegative(basisHeight, "Flex item height")
		checkedNonNegative(grow, "Flex grow")
		val resolvedWidth = basisWidth ?: if (actor is Layout) null else actor.width
		val resolvedHeight = basisHeight ?: if (actor is Layout) null else actor.height
		itemSpecs.put(actor, ItemSpec(resolvedWidth, resolvedHeight, grow))
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
			val free = (mainAvailable - lineMain[line]).coerceAtLeast(0f)
			val growUnit = if (lineGrow[line] > 0f) free / lineGrow[line] else 0f
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
				val actorMain = itemMain[index] + itemGrow[index] * growUnit
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

	override fun getMinWidth(): Float = axisMaximum(horizontal = true, useMinimum = true)

	override fun getMinHeight(): Float = axisMaximum(horizontal = false, useMinimum = true)

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
			val free = (mainAvailable - lineMain[line]).coerceAtLeast(0f)
			val growUnit = if (lineGrow[line] > 0f) free / lineGrow[line] else 0f
			val end = lineStart[line] + lineCount[line]
			for (index in lineStart[line] until end) {
				val actor = children[index]
				val actorMain = itemMain[index] + itemGrow[index] * growUnit
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
			} else {
				itemMain[index] = actorHeight
				itemCross[index] = actorWidth
			}
			itemGrow[index] = spec?.grow ?: 0f
		}
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
			spec?.basisWidth ?: if (useMinimum) minimumWidth(actor) else preferredWidth(actor)
		} else {
			spec?.basisHeight ?: if (useMinimum) minimumHeight(actor) else preferredHeight(actor)
		}
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
		fun checkedNonNegative(value: Float, label: String): Float {
			require(value.isFinite() && value >= 0f) { "$label must be finite and not negative" }
			return value
		}
	}
}
