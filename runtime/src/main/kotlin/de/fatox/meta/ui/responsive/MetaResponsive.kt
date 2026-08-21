package de.fatox.meta.ui.responsive

import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.computed
import de.fatox.meta.reactive.signal

/**
 * A named, inclusive minimum-width breakpoint measured in Meta UI units.
 *
 * Breakpoints describe available layout space, not physical device classes. Prefer querying a component's
 * container so the same component remains reusable in a window, dialog, or full-screen root.
 */
data class MetaBreakpoint(val name: String, val minWidth: Float) {
	init {
		require(name.isNotBlank()) { "Breakpoint name must not be blank" }
		require(minWidth.isFinite() && minWidth >= 0f) { "Breakpoint minimum width must be finite and not negative" }
	}
}

/** Sparse desktop-first defaults, expressed in logical Meta UI units rather than framebuffer pixels. */
object MetaBreakpoints {
	/** Small windows and the uncommon phone-sized fallback. */
	val NARROW = MetaBreakpoint("narrow", 0f)
	val HD = MetaBreakpoint("hd", 1280f)
	val FULL_HD = MetaBreakpoint("full-hd", 1920f)
	val QHD = MetaBreakpoint("qhd", 2560f)
	val UHD = MetaBreakpoint("uhd", 3840f)

	val DEFAULT = MetaBreakpointSet(NARROW, HD, FULL_HD, QHD, UHD)
}

/** Ordered breakpoint scale used to determine the largest breakpoint reached by a container. */
class MetaBreakpointSet(vararg breakpoints: MetaBreakpoint) {
	private val values = breakpoints.copyOf().also { entries ->
		require(entries.isNotEmpty()) { "A breakpoint set must not be empty" }
		entries.sortBy(MetaBreakpoint::minWidth)
		require(entries[0].minWidth == 0f) { "A breakpoint set must start at width 0" }
		for (index in 1 until entries.size) {
			require(entries[index - 1].minWidth < entries[index].minWidth) {
				"Breakpoint minimum widths must be unique"
			}
		}
	}

	val size: Int get() = values.size

	operator fun get(index: Int): MetaBreakpoint = values[index]

	fun active(width: Float): MetaBreakpoint {
		require(width.isFinite() && width >= 0f) { "Responsive width must be finite and not negative" }
		var active = values[0]
		for (index in 1 until values.size) {
			val candidate = values[index]
			if (width < candidate.minWidth) break
			active = candidate
		}
		return active
	}
}

/** Immutable snapshot emitted when a responsive container changes size. */
data class MetaResponsiveSize(val width: Float, val height: Float)

/**
 * Container-query predicate. Minimum bounds are inclusive and maximum bounds are exclusive, avoiding overlap at
 * exact breakpoint boundaries.
 */
data class MetaResponsiveQuery(
	val minWidth: Float = 0f,
	val maxWidth: Float = Float.POSITIVE_INFINITY,
	val minHeight: Float = 0f,
	val maxHeight: Float = Float.POSITIVE_INFINITY,
) {
	init {
		require(minWidth.isFinite() && minWidth >= 0f) { "Minimum width must be finite and not negative" }
		require(maxWidth > minWidth && !maxWidth.isNaN()) { "Maximum width must be greater than minimum width" }
		require(minHeight.isFinite() && minHeight >= 0f) { "Minimum height must be finite and not negative" }
		require(maxHeight > minHeight && !maxHeight.isNaN()) { "Maximum height must be greater than minimum height" }
	}

	fun matches(size: MetaResponsiveSize): Boolean = matches(size.width, size.height)

	fun matches(width: Float, height: Float): Boolean =
		width >= minWidth && width < maxWidth && height >= minHeight && height < maxHeight

	companion object {
		fun from(breakpoint: MetaBreakpoint): MetaResponsiveQuery = MetaResponsiveQuery(minWidth = breakpoint.minWidth)

		fun below(breakpoint: MetaBreakpoint): MetaResponsiveQuery = MetaResponsiveQuery(maxWidth = breakpoint.minWidth)

		fun between(start: MetaBreakpoint, end: MetaBreakpoint): MetaResponsiveQuery =
			MetaResponsiveQuery(minWidth = start.minWidth, maxWidth = end.minWidth)

		fun heightBelow(height: Float): MetaResponsiveQuery = MetaResponsiveQuery(maxHeight = height)

		fun heightFrom(height: Float): MetaResponsiveQuery = MetaResponsiveQuery(minHeight = height)
	}
}

/**
 * A cascading value: [base] applies everywhere, and each matching override wins in declaration order.
 *
 * The usual progressive form is `responsive(base).from(MetaBreakpoints.FULL_HD, expanded)`.
 */
class MetaResponsiveValue<T> internal constructor(private val base: T) {
	private data class Override<T>(val query: MetaResponsiveQuery, val value: T)

	private val overrides = ArrayList<Override<T>>(2)
	private val revision = signal(0)

	fun from(breakpoint: MetaBreakpoint, value: T): MetaResponsiveValue<T> =
		whenMatches(MetaResponsiveQuery.from(breakpoint), value)

	fun below(breakpoint: MetaBreakpoint, value: T): MetaResponsiveValue<T> =
		whenMatches(MetaResponsiveQuery.below(breakpoint), value)

	fun between(start: MetaBreakpoint, end: MetaBreakpoint, value: T): MetaResponsiveValue<T> =
		whenMatches(MetaResponsiveQuery.between(start, end), value)

	fun whenMatches(query: MetaResponsiveQuery, value: T): MetaResponsiveValue<T> = apply {
		overrides.add(Override(query, value))
		revision.update { it + 1 }
	}

	fun resolve(size: MetaResponsiveSize): T = resolve(size.width, size.height)

	fun resolve(width: Float, height: Float): T {
		revision.value // Makes late fluent additions observable when resolve runs inside a computed/effect.
		var result = base
		for (index in overrides.indices) {
			val override = overrides[index]
			if (override.query.matches(width, height)) result = override.value
		}
		return result
	}
}

fun <T> responsive(base: T): MetaResponsiveValue<T> = MetaResponsiveValue(base)

/**
 * Reactive size and breakpoint state for a screen or arbitrary container. [resize] is the only write point;
 * consumers derive values through Meta's regular `computed`/`effect` APIs.
 */
class MetaResponsiveState(val breakpoints: MetaBreakpointSet = MetaBreakpoints.DEFAULT) {
	private var currentWidth = 0f
	private var currentHeight = 0f
	private val sizeRevision = signal(false)

	/** Immutable snapshots are allocated only when this value is actually observed. */
	val size: ReactiveValue<MetaResponsiveSize> = computed { MetaResponsiveSize(trackedWidth(), trackedHeight()) }
	val width: ReactiveValue<Float> = computed { trackedWidth() }
	val height: ReactiveValue<Float> = computed { trackedHeight() }
	val breakpoint: ReactiveValue<MetaBreakpoint> = computed { breakpoints.active(trackedWidth()) }
	val portrait: ReactiveValue<Boolean> = computed { trackedHeight() > trackedWidth() }

	/** Returns true only when a new size was published. */
	fun resize(width: Float, height: Float): Boolean {
		require(width.isFinite() && width >= 0f) { "Responsive width must be finite and not negative" }
		require(height.isFinite() && height >= 0f) { "Responsive height must be finite and not negative" }
		if (currentWidth == width && currentHeight == height) return false
		currentWidth = width
		currentHeight = height
		sizeRevision.value = !sizeRevision.peek()
		return true
	}

	fun matches(query: MetaResponsiveQuery): ReactiveValue<Boolean> =
		computed { query.matches(trackedWidth(), trackedHeight()) }

	fun <T> resolve(value: MetaResponsiveValue<T>): ReactiveValue<T> =
		computed { value.resolve(trackedWidth(), trackedHeight()) }

	internal fun trackedWidth(): Float {
		sizeRevision.value
		return currentWidth
	}

	internal fun trackedHeight(): Float {
		sizeRevision.value
		return currentHeight
	}
}
