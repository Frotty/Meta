package de.fatox.meta.api.ui

/** Edge sidebar a movable [com.badlogic.gdx.scenes.scene2d.ui.Window] can dock into. */
enum class MetaDockSide(val persistedName: String) {
	LEFT("left"),
	RIGHT("right");

	companion object {
		fun fromPersisted(name: String): MetaDockSide? = entries.firstOrNull { it.persistedName == name }
	}
}

/** The completed gesture that caused a window's persisted bounds to be updated. */
enum class MetaWindowInteraction {
	PROGRAMMATIC,
	MOVE,
	RESIZE,
	DOCK_WIDTH_RESIZE,
}

/** Per-screen docking geometry. Pass `null` to `UIManager.configureWindowDocking` to disable edge docking. */
data class MetaDockConfig(
	val leftWidth: Float = 300f,
	val rightWidth: Float = 340f,
	val margin: Float = 8f,
	val gap: Float = 6f,
	val topInset: Float = 40f,
	val bottomInset: Float = 48f,
	val snapDistance: Float = 24f,
	val minimumCenterWidth: Float = 320f,
	/** Smallest user-selected sidebar width. Individual docked windows reflow inside this shared width. */
	val minimumSidebarWidth: Float = 180f,
) {
	init {
		require(
			listOf(
				leftWidth, rightWidth, margin, gap, topInset, bottomInset, snapDistance, minimumCenterWidth,
				minimumSidebarWidth,
			)
				.all { it.isFinite() },
		) {
			"Dock geometry must be finite"
		}
		require(leftWidth > 0f && rightWidth > 0f && minimumSidebarWidth > 0f) { "Dock widths must be positive" }
		require(
			margin >= 0f && gap >= 0f && topInset >= 0f && bottomInset >= 0f &&
				snapDistance >= 0f && minimumCenterWidth >= 0f,
		) {
			"Dock spacing, insets, and snap distance must not be negative"
		}
	}
}

/** Per-screen user sizing for the two shared sidebars. Zero means "use the configured default". */
data class MetaDockLayoutData(
	var leftWidth: Float = 0f,
	var rightWidth: Float = 0f,
)

internal data class MetaDockItem(
	val key: String,
	val order: Int,
	val minimumWidth: Float,
	val minimumHeight: Float,
	val requestedHeight: Float,
	val fill: Boolean,
)

internal data class MetaDockBounds(val x: Float, val y: Float, val width: Float, val height: Float)

/** Pure layout core: fixed panels keep their requested height and fill panels divide the remaining sidebar. */
internal fun calculateDockBounds(
	viewportWidth: Float,
	viewportHeight: Float,
	config: MetaDockConfig,
	side: MetaDockSide,
	items: List<MetaDockItem>,
	widthOverride: Float? = null,
): Map<String, MetaDockBounds> {
	if (items.isEmpty()) return emptyMap()
	val sorted = items.sortedWith(compareBy(MetaDockItem::order, MetaDockItem::key))
	val availableHeight = (viewportHeight - config.topInset - config.bottomInset -
		config.gap * (sorted.size - 1)).coerceAtLeast(0f)
	val heights = FloatArray(sorted.size) { sorted[it].minimumHeight.coerceAtLeast(0f) }
	val minimumTotal = heights.sum()
	if (minimumTotal > availableHeight && minimumTotal > 0f) {
		// A dock is a bounded viewport, not an infinitely tall column. When consumers add more panels than their full
		// content minima can accommodate, preserve every slot proportionally and let MetaWindow's content viewport
		// scroll. This keeps all headers reachable and avoids overlap or panels escaping below the screen.
		val scale = availableHeight / minimumTotal
		for (index in heights.indices) heights[index] *= scale
	}
	var remaining = (availableHeight - heights.sum()).coerceAtLeast(0f)
	for (index in sorted.indices) {
		val item = sorted[index]
		if (item.fill || remaining <= 0f) continue
		val wantedExtra = (item.requestedHeight - heights[index]).coerceAtLeast(0f)
		val allocated = wantedExtra.coerceAtMost(remaining)
		heights[index] += allocated
		remaining -= allocated
	}
	val fillCount = sorted.count(MetaDockItem::fill)
	if (fillCount > 0 && remaining > 0f) {
		val fillExtra = remaining / fillCount
		for (index in sorted.indices) if (sorted[index].fill) heights[index] += fillExtra
	}

	val configuredWidth = if (side === MetaDockSide.LEFT) config.leftWidth else config.rightWidth
	val width = widthOverride ?: resolveDockWidth(
		viewportWidth,
		config.margin,
		configuredWidth,
		sorted.maxOf { it.minimumWidth.coerceAtLeast(0f) },
	)
	val x = if (side === MetaDockSide.LEFT) config.margin else viewportWidth - config.margin - width
	var top = viewportHeight - config.topInset
	return buildMap {
		for (index in sorted.indices) {
			val height = heights[index]
			val y = top - height
			put(sorted[index].key, MetaDockBounds(x, y, width, height))
			top = y - config.gap
		}
	}
}

internal fun dockZoneBounds(
	viewportWidth: Float,
	viewportHeight: Float,
	config: MetaDockConfig,
	side: MetaDockSide,
	minimumWidth: Float = 0f,
	widthOverride: Float? = null,
): MetaDockBounds {
	val width = widthOverride ?: resolveDockWidth(
		viewportWidth,
		config.margin,
		if (side == MetaDockSide.LEFT) config.leftWidth else config.rightWidth,
		minimumWidth,
	)
	val x = if (side == MetaDockSide.LEFT) config.margin else viewportWidth - config.margin - width
	return MetaDockBounds(
		x = x,
		y = config.bottomInset,
		width = width,
		height = (viewportHeight - config.topInset - config.bottomInset).coerceAtLeast(0f),
	)
}

internal data class MetaDockWidths(val left: Float?, val right: Float?)

/** The sidebar may be compact, but never narrower than the widest window's declared usable minimum. */
internal fun resolveDockMinimum(configuredMinimum: Float, windowMinimums: Iterable<Float>): Float? {
	var minimum: Float? = null
	for (windowMinimum in windowMinimums) {
		val usableMinimum = windowMinimum.coerceAtLeast(configuredMinimum)
		minimum = minimum?.coerceAtLeast(usableMinimum) ?: usableMinimum
	}
	return minimum
}

/** Keeps both sidebars and a useful center canvas inside the viewport whenever their real minima permit it. */
internal fun resolveDockWidths(
	viewportWidth: Float,
	config: MetaDockConfig,
	leftMinimumWidth: Float?,
	rightMinimumWidth: Float?,
	desiredLeftWidth: Float = config.leftWidth,
	desiredRightWidth: Float = config.rightWidth,
): MetaDockWidths {
	val leftMinimum = leftMinimumWidth?.coerceAtLeast(0f)
	val rightMinimum = rightMinimumWidth?.coerceAtLeast(0f)
	if (leftMinimum == null && rightMinimum == null) return MetaDockWidths(null, null)
	val available = (viewportWidth - config.margin * 2f - config.minimumCenterWidth).coerceAtLeast(0f)
	if (rightMinimum == null) {
		return MetaDockWidths(resolveSingleDockWidth(desiredLeftWidth, leftMinimum!!, available), null)
	}
	if (leftMinimum == null) {
		return MetaDockWidths(null, resolveSingleDockWidth(desiredRightWidth, rightMinimum, available))
	}

	val desiredLeft = desiredLeftWidth.coerceAtLeast(leftMinimum)
	val desiredRight = desiredRightWidth.coerceAtLeast(rightMinimum)
	if (desiredLeft + desiredRight <= available) return MetaDockWidths(desiredLeft, desiredRight)
	val minimumTotal = leftMinimum + rightMinimum
	if (minimumTotal >= available) return MetaDockWidths(leftMinimum, rightMinimum)
	val extra = available - minimumTotal
	val desiredLeftExtra = desiredLeft - leftMinimum
	val desiredRightExtra = desiredRight - rightMinimum
	val desiredExtraTotal = desiredLeftExtra + desiredRightExtra
	if (desiredExtraTotal <= 0f) return MetaDockWidths(leftMinimum, rightMinimum)
	return MetaDockWidths(
		leftMinimum + extra * desiredLeftExtra / desiredExtraTotal,
		rightMinimum + extra * desiredRightExtra / desiredExtraTotal,
	)
}

/** Allocation-free single-side form for drag-preview hot paths. */
internal fun resolveDockWidthForSide(
	viewportWidth: Float,
	config: MetaDockConfig,
	targetSide: MetaDockSide,
	leftMinimumWidth: Float?,
	rightMinimumWidth: Float?,
	desiredLeftWidth: Float = config.leftWidth,
	desiredRightWidth: Float = config.rightWidth,
): Float? {
	val targetMinimum = (if (targetSide == MetaDockSide.LEFT) leftMinimumWidth else rightMinimumWidth)
		?: return null
	val otherMinimum = if (targetSide == MetaDockSide.LEFT) rightMinimumWidth else leftMinimumWidth
	val configured = if (targetSide == MetaDockSide.LEFT) desiredLeftWidth else desiredRightWidth
	val available = (viewportWidth - config.margin * 2f - config.minimumCenterWidth).coerceAtLeast(0f)
	if (otherMinimum == null) return resolveSingleDockWidth(configured, targetMinimum, available)

	val leftMinimum = leftMinimumWidth ?: return null
	val rightMinimum = rightMinimumWidth ?: return null
	val desiredLeft = desiredLeftWidth.coerceAtLeast(leftMinimum)
	val desiredRight = desiredRightWidth.coerceAtLeast(rightMinimum)
	if (desiredLeft + desiredRight <= available) {
		return if (targetSide == MetaDockSide.LEFT) desiredLeft else desiredRight
	}
	val minimumTotal = leftMinimum + rightMinimum
	if (minimumTotal >= available) return targetMinimum
	val desiredLeftExtra = desiredLeft - leftMinimum
	val desiredRightExtra = desiredRight - rightMinimum
	val desiredExtraTotal = desiredLeftExtra + desiredRightExtra
	if (desiredExtraTotal <= 0f) return targetMinimum
	val targetExtra = if (targetSide == MetaDockSide.LEFT) desiredLeftExtra else desiredRightExtra
	return targetMinimum + (available - minimumTotal) * targetExtra / desiredExtraTotal
}

private fun resolveSingleDockWidth(configured: Float, minimum: Float, available: Float): Float =
	if (available >= minimum) configured.coerceIn(minimum, available) else minimum

internal fun resolveDockWidth(
	viewportWidth: Float,
	margin: Float,
	configuredWidth: Float,
	minimumWidth: Float,
): Float {
	val minimum = minimumWidth.coerceAtLeast(0f)
	val available = (viewportWidth - margin * 2f).coerceAtLeast(0f)
	return if (available >= minimum) configuredWidth.coerceIn(minimum, available) else minimum
}

internal data class MetaDockUpdate(
	val side: MetaDockSide?,
	val updateFloatingBounds: Boolean,
	val updateDockHeight: Boolean,
)

/** Pure persistence decision used by MetaUiManager for gesture and programmatic window updates. */
internal fun resolveDockUpdate(
	currentSide: MetaDockSide?,
	interaction: MetaWindowInteraction,
	detectedSide: MetaDockSide?,
): MetaDockUpdate = if (interaction === MetaWindowInteraction.PROGRAMMATIC) {
	MetaDockUpdate(
		currentSide,
		updateFloatingBounds = currentSide == null,
		updateDockHeight = false,
	)
} else if (interaction === MetaWindowInteraction.RESIZE) {
	MetaDockUpdate(
		currentSide,
		updateFloatingBounds = currentSide == null,
		updateDockHeight = currentSide != null,
	)
} else if (interaction === MetaWindowInteraction.DOCK_WIDTH_RESIZE) {
	MetaDockUpdate(
		currentSide,
		updateFloatingBounds = false,
		updateDockHeight = false,
	)
} else {
	MetaDockUpdate(
		detectedSide,
		updateFloatingBounds = detectedSide == null,
		updateDockHeight = detectedSide != null,
	)
}

internal fun windowGestureChanged(
	interaction: MetaWindowInteraction,
	startX: Float,
	startY: Float,
	startWidth: Float,
	startHeight: Float,
	endX: Float,
	endY: Float,
	endWidth: Float,
	endHeight: Float,
	epsilon: Float = 0.01f,
): Boolean = if (interaction === MetaWindowInteraction.PROGRAMMATIC) {
	true
} else if (interaction === MetaWindowInteraction.MOVE) {
	kotlin.math.abs(endX - startX) >= epsilon || kotlin.math.abs(endY - startY) >= epsilon
} else if (interaction === MetaWindowInteraction.RESIZE) {
		kotlin.math.abs(endWidth - startWidth) >= epsilon || kotlin.math.abs(endHeight - startHeight) >= epsilon
} else {
	kotlin.math.abs(endWidth - startWidth) >= epsilon
}

/** Pointer movement must keep live dock ordering active even after stage clamping freezes the window bounds. */
internal fun dockPreviewChanged(
	x: Float,
	y: Float,
	width: Float,
	orderAnchorY: Float,
	previousX: Float,
	previousY: Float,
	previousWidth: Float,
	previousOrderAnchorY: Float,
): Boolean = x != previousX || y != previousY || width != previousWidth ||
	!sameDockPreviewCoordinate(orderAnchorY, previousOrderAnchorY)

private fun sameDockPreviewCoordinate(value: Float, previous: Float): Boolean =
	value == previous || value.isNaN() && previous.isNaN()

/** A dock divider occupies the configured inter-window gap, never either window's content bounds. */
internal fun dockDividerBottom(upperWindowBottom: Float, gap: Float): Float = upperWindowBottom - gap

/** Converts a divider drag into the fixed panel height on either side of the divider. */
internal fun resizedDockPanelHeight(
	startHeight: Float,
	deltaY: Float,
	resizeLowerPanel: Boolean,
	minimumHeight: Float,
): Float = (startHeight + if (resizeLowerPanel) deltaY else -deltaY).coerceAtLeast(minimumHeight)

/** The final lower panel moves as a fixed-height block; only interior fill panels absorb divider movement. */
internal fun shouldResizeLowerDockPanel(upperFill: Boolean, lowerIsLast: Boolean): Boolean =
	upperFill && !lowerIsLast

/**
 * Converts movement of a fill panel's lower divider into the requested height of its lower neighbour. The fill panel
 * continues to consume the remainder, while the explicitly sized lower panel stays fixed at the divider position.
 */
internal fun resizedLowerDockPanelHeight(
	upperBottom: Float,
	lowerTop: Float,
	lowerHeight: Float,
	gap: Float,
	minimumHeight: Float,
): Float {
	val upperGrowth = lowerTop + gap - upperBottom
	return (lowerHeight - upperGrowth).coerceAtLeast(minimumHeight)
}

internal data class MetaDockOrderItem(val key: String, val order: Int, val anchorY: Float)
internal data class MetaDockOrderUpdate(val order: Int, val normalizedOrders: Map<String, Int> = emptyMap())

internal fun calculateDockOrder(
	movingAnchorY: Float,
	existing: List<MetaDockOrderItem>,
	orderStep: Int = 100,
): MetaDockOrderUpdate {
	require(orderStep > 1) { "Dock order step must leave insertion space" }
	val ordered = existing.sortedWith(compareBy(MetaDockOrderItem::order, MetaDockOrderItem::key))
	val insertion = ordered.indexOfFirst { movingAnchorY > it.anchorY }
	val index = if (insertion < 0) ordered.size else insertion
	if (ordered.isEmpty()) return MetaDockOrderUpdate(0)
	if (index == 0) return MetaDockOrderUpdate(ordered.first().order - orderStep)
	if (index == ordered.size) return MetaDockOrderUpdate(ordered.last().order + orderStep)
	val before = ordered[index - 1].order
	val after = ordered[index].order
	if (after - before > 1) return MetaDockOrderUpdate(before + (after - before) / 2)

	val normalized = buildMap {
		for (orderedIndex in ordered.indices) {
			put(ordered[orderedIndex].key, (if (orderedIndex < index) orderedIndex else orderedIndex + 1) * orderStep)
		}
	}
	return MetaDockOrderUpdate(index * orderStep, normalized)
}

internal fun detectDockSide(
	x: Float,
	width: Float,
	viewportWidth: Float,
	snapDistance: Float,
): MetaDockSide? {
	val leftDistance = kotlin.math.abs(x)
	val rightDistance = kotlin.math.abs(viewportWidth - x - width)
	val leftCandidate = leftDistance <= snapDistance
	val rightCandidate = rightDistance <= snapDistance
	return when {
		leftCandidate && rightCandidate -> if (leftDistance <= rightDistance) MetaDockSide.LEFT else MetaDockSide.RIGHT
		leftCandidate -> MetaDockSide.LEFT
		rightCandidate -> MetaDockSide.RIGHT
		else -> null
	}
}
