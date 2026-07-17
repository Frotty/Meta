package de.fatox.meta.api.model

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import de.fatox.meta.Meta
import de.fatox.meta.audioVideoDataKey
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.signal
import org.slf4j.Logger

private val log: Logger = MetaLoggerFactory.logger {}

/** Application-lifetime source of truth for audio/video settings. Persisted data is a snapshot, never live state. */
object MetaAudioVideoState {
	private val stateSignal = signal(MetaAudioVideoData())
	val state: ReactiveValue<MetaAudioVideoData> = stateSignal

	fun initialize(value: MetaAudioVideoData) {
		val initial = value.copy()
		// Legacy saves predate the explicit flag. A save already in decorated windowed mode necessarily has usable
		// windowed history; fullscreen/borderless legacy saves use the new centered first-use fallback.
		if (!initial.windowedBoundsInitialized && !initial.usesBorderlessPresentation()) {
			initial.windowedBoundsInitialized = true
		}
		stateSignal.value = initial
	}

	fun current(): MetaAudioVideoData = stateSignal.peek().copy()

	fun replace(value: MetaAudioVideoData, applyDisplay: Boolean = false, persist: Boolean = true) {
		val next = value.copy()
		if (applyDisplay) next.apply()
		stateSignal.value = next.copy()
		if (persist) inject<MetaData>().save(audioVideoDataKey, next)
	}

	fun update(applyDisplay: Boolean = false, persist: Boolean = true, change: MetaAudioVideoData.() -> Unit) {
		replace(current().apply(change), applyDisplay, persist)
	}
}

/** True while programmatic fullscreen/windowed callbacks must not be persisted as user-moved window bounds. */
object MetaDisplayTransition {
	@Volatile
	var inProgress: Boolean = false
		private set

	internal fun begin() {
		inProgress = true
		System.setProperty("meta.displayTransition.inProgress", "true")
	}

	internal fun finishAfterCallbacks(expectedBounds: IntArray? = null) {
		// LWJGL mode changes produce resize callbacks after the setter returns. Keep the guard through two render queues.
		Gdx.app.postRunnable {
			Gdx.app.postRunnable {
				val expected = expectedBounds
				if (!Gdx.graphics.isFullscreen && expected != null && !matchesExpectedBounds(expected)) {
					log.warn(
						"Display transition geometry drifted to {}x{} at {},{}; reapplying {}x{} at {},{}",
						Gdx.graphics.width, Gdx.graphics.height, Meta.instance.windowHandler.x, Meta.instance.windowHandler.y,
						expected[2], expected[3], expected[0], expected[1],
					)
					Gdx.graphics.setWindowedMode(expected[2], expected[3])
					Meta.instance.windowHandler.modify(expected[0], expected[1])
					// Let the repair's callbacks drain too; the transition guard remains active throughout.
					Gdx.app.postRunnable { finish(expected) }
				} else {
					finish(expected)
				}
			}
		}
	}

	private fun matchesExpectedBounds(expected: IntArray): Boolean =
		Gdx.graphics.width == expected[2] &&
			Gdx.graphics.height == expected[3] &&
			Meta.instance.windowHandler.x == expected[0] &&
			Meta.instance.windowHandler.y == expected[1]

	private fun finish(expected: IntArray?) {
		inProgress = false
		System.setProperty("meta.displayTransition.inProgress", "false")
		log.info(
			"Display transition settled: fullscreen={}, actual={}x{} at {},{}, expected={}",
			Gdx.graphics.isFullscreen,
			Gdx.graphics.width,
			Gdx.graphics.height,
			Meta.instance.windowHandler.x,
			Meta.instance.windowHandler.y,
			expected?.let { "${it[2]}x${it[3]} at ${it[0]},${it[1]}" } ?: "fullscreen",
		)
	}
}


data class MetaDisplayMode(
	val width: Int = 0,
	val height: Int = 0,
	val refreshRate: Int = 0,
	val bitsPerPixel: Int = 0,
	val monitorIndex: Int = 0 // Simplified to index, might cause problems when having more than two monitors
) {
	constructor(dm: Graphics.DisplayMode) : this(
		dm.width,
		dm.height,
		dm.refreshRate,
		dm.bitsPerPixel,
		dm.monitorIndex
	)

	fun equalsDisplayMode(dm: Graphics.DisplayMode): Boolean =
		width == dm.width &&
			height == dm.height &&
			refreshRate == dm.refreshRate &&
			bitsPerPixel == dm.bitsPerPixel
}

private inline val Graphics.DisplayMode.monitorIndex
	get() = Meta.instance.monitorHandler.monitorIndex(this)

fun Graphics.DisplayMode.toMetaDisplayMode(): MetaDisplayMode = MetaDisplayMode(this)

/** Safe decorated-window geometry shared by startup launchers and persisted display restoration. */
object MetaWindowBounds {
	const val MIN_WIDTH = 854
	const val MIN_HEIGHT = 480
	private const val MONITOR_MARGIN = 80

	internal fun initial(monitorX: Int, monitorY: Int, monitorWidth: Int, monitorHeight: Int): IntArray {
		val limits = limits(monitorWidth, monitorHeight)
		val width = (monitorWidth * 0.75f).toInt().coerceIn(limits[0], limits[2])
		val height = (monitorHeight * 0.75f).toInt().coerceIn(limits[1], limits[3])
		return intArrayOf(
			monitorX + (monitorWidth - width) / 2,
			monitorY + (monitorHeight - height) / 2,
			width,
			height,
		)
	}

	internal fun sanitize(
		x: Int,
		y: Int,
		width: Int,
		height: Int,
		monitorX: Int,
		monitorY: Int,
		monitorWidth: Int,
		monitorHeight: Int,
	): IntArray {
		val limits = limits(monitorWidth, monitorHeight)
		val safeWidth = width.coerceIn(limits[0], limits[2])
		val safeHeight = height.coerceIn(limits[1], limits[3])
		val safeX = x.coerceIn(monitorX, monitorX + monitorWidth - safeWidth)
		val safeY = y.coerceIn(monitorY, monitorY + monitorHeight - safeHeight)
		return intArrayOf(safeX, safeY, safeWidth, safeHeight)
	}

	private fun limits(monitorWidth: Int, monitorHeight: Int): IntArray {
		val maximumWidth = (monitorWidth - MONITOR_MARGIN).coerceAtLeast(1)
		val maximumHeight = (monitorHeight - MONITOR_MARGIN).coerceAtLeast(1)
		return intArrayOf(
			MIN_WIDTH.coerceAtMost(maximumWidth),
			MIN_HEIGHT.coerceAtMost(maximumHeight),
			maximumWidth,
			maximumHeight,
		)
	}
}

private fun getCurrentMonitor(): Graphics.Monitor {
	val graphics = Gdx.graphics
	val monitors = graphics.monitors
	val windowX = Meta.instance.windowHandler.x
	val windowY = Meta.instance.windowHandler.y
	val windowWidth = graphics.width.coerceAtLeast(1)
	val windowHeight = graphics.height.coerceAtLeast(1)
	val windowRight = windowX + windowWidth
	val windowBottom = windowY + windowHeight
	var bestMonitor = graphics.primaryMonitor
	var bestOverlap = -1L
	var bestCenterDistance = Long.MAX_VALUE

	for (i in monitors.indices) {
		val monitor = monitors[i]
		val mode = graphics.getDisplayMode(monitor)
		val monitorRight = monitor.virtualX + mode.width
		val monitorBottom = monitor.virtualY + mode.height
		val overlapWidth = (minOf(windowRight, monitorRight) - maxOf(windowX, monitor.virtualX)).coerceAtLeast(0)
		val overlapHeight = (minOf(windowBottom, monitorBottom) - maxOf(windowY, monitor.virtualY)).coerceAtLeast(0)
		val overlap = overlapWidth.toLong() * overlapHeight.toLong()
		val centerDx = (windowX * 2L + windowWidth) - (monitor.virtualX * 2L + mode.width)
		val centerDy = (windowY * 2L + windowHeight) - (monitor.virtualY * 2L + mode.height)
		val centerDistance = centerDx * centerDx + centerDy * centerDy
		if (overlap > bestOverlap || overlap == bestOverlap && centerDistance < bestCenterDistance) {
			bestOverlap = overlap
			bestCenterDistance = centerDistance
			bestMonitor = monitor
		}
	}
	return bestMonitor
}

private fun requestedMonitor(requested: MetaDisplayMode): Graphics.Monitor {
	val monitors = Gdx.graphics.monitors
	return if (requested.width > 0 && requested.monitorIndex in monitors.indices) {
		monitors[requested.monitorIndex]
	} else {
		getCurrentMonitor()
	}
}

/**
 * Created by Frotty on 05.11.2016.
 */
data class MetaAudioVideoData(
	var profile: String = "default",
	var hd: Boolean = true,
	var resizeable: Boolean = true,
	var borderless: Boolean = false,
	var fullscreen: Boolean = false,
	var x: Int = 32,
	var y: Int = 32,
	var width: Int = 1536,
	var height: Int = 864,
	var displayMode: Int = 0,
	var vsyncEnabled: Boolean = true,
	var maxFps: Int = 128,
	var videoDebug: Boolean = false,
	var masterVolume: Float = 0.5f,
	var musicVolume: Float = 1f,
	var soundVolume: Float = 1f,
	var metaDisplayMode: MetaDisplayMode = MetaDisplayMode(),
	var runWithUI: Boolean = true,
	var windowedBoundsInitialized: Boolean = false,
) {
	/** Both persisted flags deliberately map to the same compositor-friendly borderless-windowed presentation. */
	internal fun usesBorderlessPresentation(): Boolean = fullscreen || borderless

	fun captureWindowedBounds() {
		// Meta presents fullscreen as an undecorated monitor-sized window. Never overwrite the remembered decorated
		// bounds with that temporary presentation, even though libGDX correctly reports it as "windowed".
		if (usesBorderlessPresentation() || Gdx.graphics.isFullscreen) return
		x = Meta.instance.windowHandler.x
		y = Meta.instance.windowHandler.y
		width = Gdx.graphics.width
		height = Gdx.graphics.height
		// Remember which monitor owns these bounds for the next fullscreen/borderless transition.
		metaDisplayMode = Gdx.graphics.getDisplayMode(getCurrentMonitor()).toMetaDisplayMode()
		windowedBoundsInitialized = true
	}

	fun apply() {
		if (!runWithUI) return
		MetaDisplayTransition.begin()
		log.info(
			"Applying display state: fullscreen={}, borderless={}, saved={}x{} at {},{}, requestedMode={}x{}@{} monitor={}",
			fullscreen, borderless, width, height, x, y,
			metaDisplayMode.width, metaDisplayMode.height, metaDisplayMode.refreshRate, metaDisplayMode.monitorIndex,
		)

		var expectedBounds: IntArray? = null
		if (usesBorderlessPresentation()) {
				// Borderless is a monitor-sized window, not the previously saved decorated-window rectangle. Keep x/y/
				// width/height untouched as windowed-mode history, and place this presentation on the selected monitor.
				val monitor = requestedMonitor(metaDisplayMode)
				val mode = Gdx.graphics.getDisplayMode(monitor)
				metaDisplayMode = mode.toMetaDisplayMode()
				Gdx.graphics.setUndecorated(true)
				Gdx.graphics.setWindowedMode(mode.width, mode.height)
				Meta.instance.windowHandler.modify(monitor.virtualX, monitor.virtualY)
				expectedBounds = intArrayOf(monitor.virtualX, monitor.virtualY, mode.width, mode.height)
		} else {
				val restored = safeWindowedBounds()
				x = restored[0]
				y = restored[1]
				width = restored[2]
				height = restored[3]
				Gdx.graphics.setUndecorated(false)
				Gdx.graphics.setWindowedMode(width, height)
				Meta.instance.windowHandler.modify(x, y)
				expectedBounds = intArrayOf(x, y, width, height)
		}
		Gdx.graphics.setVSync(vsyncEnabled)
		// LWJGL3 treats 0 as "never sleep" (uncapped); map any non-positive value to that.
		// Applied regardless of vsync so the cap takes effect immediately if vsync is toggled off.
		Gdx.graphics.setForegroundFPS(if (maxFps > 0) maxFps else 0)
		Meta.instance.windowHandler.focus()
		MetaDisplayTransition.finishAfterCallbacks(expectedBounds)
	}

	private fun safeWindowedBounds(): IntArray {
		val monitors = Gdx.graphics.monitors
		var monitor = Gdx.graphics.primaryMonitor
		var bestOverlap = -1L
		for (i in monitors.indices) {
			val candidate = monitors[i]
			val mode = Gdx.graphics.getDisplayMode(candidate)
			val overlapWidth = (minOf(x + width, candidate.virtualX + mode.width) - maxOf(x, candidate.virtualX)).coerceAtLeast(0)
			val overlapHeight = (minOf(y + height, candidate.virtualY + mode.height) - maxOf(y, candidate.virtualY)).coerceAtLeast(0)
			val overlap = overlapWidth.toLong() * overlapHeight.toLong()
			if (overlap > bestOverlap) {
				bestOverlap = overlap
				monitor = candidate
			}
		}
		val mode = Gdx.graphics.getDisplayMode(monitor)
		if (!windowedBoundsInitialized) {
			val initial = MetaWindowBounds.initial(monitor.virtualX, monitor.virtualY, mode.width, mode.height)
			windowedBoundsInitialized = true
			log.info(
				"Using initial centered windowed bounds {}x{} at {},{} on monitor {},{} {}x{}",
				initial[2], initial[3], initial[0], initial[1],
				monitor.virtualX, monitor.virtualY, mode.width, mode.height,
			)
			return initial
		}
		val safe = MetaWindowBounds.sanitize(
			x, y, width, height,
			monitor.virtualX, monitor.virtualY, mode.width, mode.height,
		)
		val safeX = safe[0]
		val safeY = safe[1]
		val safeWidth = safe[2]
		val safeHeight = safe[3]
		if (safeX != x || safeY != y || safeWidth != width || safeHeight != height) {
			log.warn(
				"Corrected unsafe windowed bounds {}x{} at {},{} to {}x{} at {},{} on monitor {},{} {}x{}",
				width, height, x, y, safeWidth, safeHeight, safeX, safeY,
				monitor.virtualX, monitor.virtualY, mode.width, mode.height,
			)
		}
		return safe
	}
}
