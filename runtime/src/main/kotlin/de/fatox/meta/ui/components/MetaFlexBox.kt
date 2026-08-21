package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.responsive.MetaResponsiveSize
import de.fatox.meta.ui.responsive.MetaResponsiveState
import de.fatox.meta.ui.responsive.MetaResponsiveValue
import de.fatox.meta.ui.responsive.responsive
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
	private var responsiveExcludedItems: ObjectSet<Actor>? = null
	private var measuredMainSize = Float.NaN
	private var itemCapacity = 0
	private var measuredItemCount = 0
	private var itemChildIndex = IntArray(0)
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
	private var responsiveScope: ReactiveScope? = null
	private var responsiveConfiguration: MetaFlexResponsive? = null
	private var responsiveStateOrNull: MetaResponsiveState? = null

	/** Reactive container state, allocated on first use and then updated automatically from this box's bounds. */
	val responsiveState: MetaResponsiveState
		get() = responsiveStateOrNull ?: MetaResponsiveState().also {
			it.resize(width, height)
			responsiveStateOrNull = it
		}

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
			val checked = checkedNonNegative(value, "Flex main gap")
			if (field == checked) return
			field = checked
			invalidateHierarchy()
		}
	var crossGap: Float = checkedNonNegative(crossGap, "Flex cross gap")
		set(value) {
			val checked = checkedNonNegative(value, "Flex cross gap")
			if (field == checked) return
			field = checked
			invalidateHierarchy()
		}
	var justify: MetaFlexJustify = justify
		set(value) {
			if (field == value) return
			field = value
			invalidate()
		}
	var align: MetaFlexAlign = align
		set(value) {
			if (field == value) return
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
		val current = itemSpecs[actor]
		if (current != null &&
			current.basisWidth == resolvedWidth && current.basisHeight == resolvedHeight &&
			current.grow == grow && current.shrink == shrink &&
			current.minWidth == minWidth && current.minHeight == minHeight
		) return@apply
		itemSpecs.put(actor, ItemSpec(resolvedWidth, resolvedHeight, grow, shrink, minWidth, minHeight))
		invalidateHierarchy()
	}

	/**
	 * Adds progressive responsive behavior without rebuilding the actor tree. Rules cascade in declaration order.
	 * Repeated calls replace the previous responsive configuration.
	 */
	fun responsive(init: MetaFlexResponsive.() -> Unit): MetaFlexBox = apply {
		responsiveScope?.dispose()
		responsiveConfiguration?.restore()
		val configuration = MetaFlexResponsive(this).apply(init)
		responsiveConfiguration = configuration
		responsiveScope = ReactiveScope().also { scope ->
			scope.effect("MetaFlexBox.responsive") {
				configuration.apply(responsiveState.size.value)
			}
		}
	}

	override fun removeActor(actor: Actor, unfocus: Boolean): Boolean {
		val removed = super.removeActor(actor, unfocus)
		if (removed) {
			cleanupRemovedActor(actor)
		}
		return removed
	}

	override fun removeActorAt(index: Int, unfocus: Boolean): Actor {
		val actor = super.removeActorAt(index, unfocus)
		cleanupRemovedActor(actor)
		return actor
	}

	override fun clearChildren(unfocus: Boolean) {
		responsiveConfiguration?.clearItems()
		itemSpecs.clear()
		responsiveExcludedItems?.clear()
		super.clearChildren(unfocus)
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
				val actor = children[itemChildIndex[index]]
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
		responsiveStateOrNull?.resize(width, height)
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
		measureItems()
		if (measuredItemCount == 0) return 0f
		val rowDirection = direction == MetaFlexDirection.ROW
		if (!wrap) {
			return if (horizontal == rowDirection) {
				axisTotal(horizontal) + mainGap * max(0, measuredItemCount - 1)
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
		if (measuredItemCount == 0) return 0
		ensureLineCapacity(measuredItemCount)
		var largestMain = 0f
		for (index in 0 until measuredItemCount) largestMain = max(largestMain, itemMain[index])
		val limit = if (wrap) mainAvailable.coerceAtLeast(largestMain) else Float.POSITIVE_INFINITY
		var lines = 1
		resetLine(0, 0)
		for (index in 0 until measuredItemCount) {
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
				val actor = children[itemChildIndex[index]]
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
		measuredItemCount = 0
		for (childIndex in 0 until children.size) {
			val actor = children[childIndex]
			if (responsiveExcludedItems?.contains(actor) == true) continue
			val index = measuredItemCount++
			itemChildIndex[index] = childIndex
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
		for (index in 0 until measuredItemCount) {
			total += axisSize(children[itemChildIndex[index]], horizontal, useMinimum = false)
		}
		return total
	}

	private fun axisMaximum(horizontal: Boolean, useMinimum: Boolean = false): Float {
		var maximum = 0f
		for (index in 0 until measuredItemCount) {
			maximum = max(maximum, axisSize(children[itemChildIndex[index]], horizontal, useMinimum))
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
		measureItems()
		if (measuredItemCount == 0) return 0f
		val mainAxis = horizontal == (direction == MetaFlexDirection.ROW)
		if (!mainAxis) return axisMaximum(horizontal, useMinimum = true)
		if (wrap) return axisMaximum(horizontal, useMinimum = true)
		return axisTotalMinimum(horizontal) + mainGap * max(0, measuredItemCount - 1)
	}

	private fun axisTotalMinimum(horizontal: Boolean): Float {
		var total = 0f
		for (index in 0 until measuredItemCount) {
			total += axisSize(children[itemChildIndex[index]], horizontal, useMinimum = true)
		}
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
		itemChildIndex = itemChildIndex.copyOf(itemCapacity)
		itemMain = itemMain.copyOf(itemCapacity)
		itemCross = itemCross.copyOf(itemCapacity)
		itemGrow = itemGrow.copyOf(itemCapacity)
		itemShrink = itemShrink.copyOf(itemCapacity)
		itemMinMain = itemMinMain.copyOf(itemCapacity)
		itemLayoutMain = itemLayoutMain.copyOf(itemCapacity)
	}

	/** Fluent responsive declarations for a [MetaFlexBox]. */
	class MetaFlexResponsive internal constructor(private val flex: MetaFlexBox) {
		private val originalDirection = flex.direction
		private val originalWrap = flex.wrap
		private val originalMainGap = flex.mainGap
		private val originalCrossGap = flex.crossGap
		private val originalJustify = flex.justify
		private val originalAlign = flex.align
		private var direction: MetaResponsiveValue<MetaFlexDirection>? = null
		private var wrap: MetaResponsiveValue<Boolean>? = null
		private var mainGap: MetaResponsiveValue<Float>? = null
		private var crossGap: MetaResponsiveValue<Float>? = null
		private var justify: MetaResponsiveValue<MetaFlexJustify>? = null
		private var align: MetaResponsiveValue<MetaFlexAlign>? = null
		private val items = ArrayList<ResponsiveItem>(2)

		fun direction(base: MetaFlexDirection): MetaResponsiveValue<MetaFlexDirection> =
			responsive(base).also { direction = it }

		fun wrap(base: Boolean): MetaResponsiveValue<Boolean> = responsive(base).also { wrap = it }

		fun mainGap(base: Float): MetaResponsiveValue<Float> = responsive(base).also { mainGap = it }

		fun crossGap(base: Float): MetaResponsiveValue<Float> = responsive(base).also { crossGap = it }

		fun gap(base: Float): MetaResponsiveValue<Float> = responsive(base).also {
			mainGap = it
			crossGap = it
		}

		fun justify(base: MetaFlexJustify): MetaResponsiveValue<MetaFlexJustify> =
			responsive(base).also { justify = it }

		fun align(base: MetaFlexAlign): MetaResponsiveValue<MetaFlexAlign> = responsive(base).also { align = it }

		fun item(actor: Actor, init: ResponsiveItem.() -> Unit) {
			require(actor.parent === flex) { "Responsive flex items must already belong to this MetaFlexBox" }
			items.add(ResponsiveItem(flex, actor).apply(init))
		}

		internal fun removeActor(actor: Actor) {
			for (index in items.indices.reversed()) {
				if (items[index].actor === actor) {
					items[index].restoreVisibility()
					items.removeAt(index)
				}
			}
		}

		internal fun clearItems() {
			for (index in items.indices) items[index].restoreVisibility()
			items.clear()
		}

		internal fun restore() {
			if (direction != null) flex.direction = originalDirection
			if (wrap != null) flex.wrap = originalWrap
			if (mainGap != null) flex.mainGap = originalMainGap
			if (crossGap != null) flex.crossGap = originalCrossGap
			if (justify != null) flex.justify = originalJustify
			if (align != null) flex.align = originalAlign
			for (index in items.indices) items[index].restore()
		}

		internal fun apply(size: MetaResponsiveSize) {
			direction?.let { flex.direction = it.resolve(size) }
			wrap?.let { flex.wrap = it.resolve(size) }
			mainGap?.let { flex.mainGap = it.resolve(size) }
			crossGap?.let { flex.crossGap = it.resolve(size) }
			justify?.let { flex.justify = it.resolve(size) }
			align?.let { flex.align = it.resolve(size) }
			for (index in items.indices) items[index].apply(size)
		}
	}

	class ResponsiveItem internal constructor(private val flex: MetaFlexBox, internal val actor: Actor) {
		private val originalVisible = actor.isVisible
		private val original = flex.itemSpecs[actor] ?: ItemSpec(
			if (actor is Layout) null else actor.width,
			if (actor is Layout) null else actor.height,
			0f,
			1f,
			null,
			null,
		)
		private var visible: MetaResponsiveValue<Boolean>? = null
		private var basisWidth: MetaResponsiveValue<Float?>? = null
		private var basisHeight: MetaResponsiveValue<Float?>? = null
		private var grow: MetaResponsiveValue<Float>? = null
		private var shrink: MetaResponsiveValue<Float>? = null
		private var minWidth: MetaResponsiveValue<Float?>? = null
		private var minHeight: MetaResponsiveValue<Float?>? = null

		fun visible(base: Boolean): MetaResponsiveValue<Boolean> = responsive(base).also { visible = it }
		fun basisWidth(base: Float?): MetaResponsiveValue<Float?> = responsive(base).also { basisWidth = it }
		fun basisHeight(base: Float?): MetaResponsiveValue<Float?> = responsive(base).also { basisHeight = it }
		fun grow(base: Float): MetaResponsiveValue<Float> = responsive(base).also { grow = it }
		fun shrink(base: Float): MetaResponsiveValue<Float> = responsive(base).also { shrink = it }
		fun minWidth(base: Float?): MetaResponsiveValue<Float?> = responsive(base).also { minWidth = it }
		fun minHeight(base: Float?): MetaResponsiveValue<Float?> = responsive(base).also { minHeight = it }

		fun width(base: Float?): MetaResponsiveValue<Float?> = basisWidth(base)
		fun height(base: Float?): MetaResponsiveValue<Float?> = basisHeight(base)

		internal fun apply(size: MetaResponsiveSize) {
			visible?.resolve(size)?.let { flex.setResponsiveVisible(actor, it) }
			val responsiveWidth = basisWidth
			val responsiveHeight = basisHeight
			val responsiveMinWidth = minWidth
			val responsiveMinHeight = minHeight
			flex.configure(
				actor = actor,
				basisWidth = if (responsiveWidth == null) original.basisWidth else responsiveWidth.resolve(size),
				basisHeight = if (responsiveHeight == null) original.basisHeight else responsiveHeight.resolve(size),
				grow = grow?.resolve(size) ?: original.grow,
				shrink = shrink?.resolve(size) ?: original.shrink,
				minWidth = if (responsiveMinWidth == null) original.minWidth else responsiveMinWidth.resolve(size),
				minHeight = if (responsiveMinHeight == null) original.minHeight else responsiveMinHeight.resolve(size),
			)
		}

		internal fun restoreVisibility() {
			if (visible != null) flex.clearResponsiveVisibility(actor, originalVisible)
		}

		internal fun restore() {
			restoreVisibility()
			flex.configure(
				actor,
				original.basisWidth,
				original.basisHeight,
				original.grow,
				original.shrink,
				original.minWidth,
				original.minHeight,
			)
		}
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

	private fun setResponsiveVisible(actor: Actor, visible: Boolean) {
		val participationChanged = if (visible) {
			responsiveExcludedItems?.remove(actor) == true
		} else {
			val excluded = responsiveExcludedItems ?: ObjectSet<Actor>().also { responsiveExcludedItems = it }
			excluded.add(actor)
		}
		val visibilityChanged = actor.isVisible != visible
		if (!participationChanged && !visibilityChanged) return
		actor.isVisible = visible
		invalidateHierarchy()
	}

	private fun clearResponsiveVisibility(actor: Actor, visible: Boolean) {
		val participationChanged = responsiveExcludedItems?.remove(actor) == true
		val visibilityChanged = actor.isVisible != visible
		if (!participationChanged && !visibilityChanged) return
		actor.isVisible = visible
		invalidateHierarchy()
	}

	private fun cleanupRemovedActor(actor: Actor) {
		itemSpecs.remove(actor)
		responsiveExcludedItems?.remove(actor)
		responsiveConfiguration?.removeActor(actor)
		invalidateHierarchy()
	}

	private companion object {
		const val SIZE_EPSILON = 0.001f
		fun checkedNonNegative(value: Float, label: String): Float {
			require(value.isFinite() && value >= 0f) { "$label must be finite and not negative" }
			return value
		}
	}
}
