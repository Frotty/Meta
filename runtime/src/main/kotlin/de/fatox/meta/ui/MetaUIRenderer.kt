package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.MonitorHandler
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.extensions.trace
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.ui.FocusRenderer
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.components.MetaFileChooser
import de.fatox.meta.ui.components.MetaTooltip
import de.fatox.meta.ui.components.nearestScrollableMetaScrollPane
import de.fatox.meta.ui.components.scrollDebugPath
import de.fatox.meta.ui.components.scrollDebugState
import de.fatox.meta.ui.components.updateMetaScrollFocus
import kotlin.math.abs

private val log = MetaLoggerFactory.logger {}

/**
 * Suggested default UI scale. OS scaling is already represented by the back-buffer/logical-size ratio, so Meta
 * never applies a second scale in that case. For an unscaled 4K/5K desktop it accepts a larger default only when
 * resolution and a sane EDID density agree. Ambiguous displays remain at 100%; users can always override this.
 */
fun suggestedUiScale(): Float = suggestedUiScale(
	logicalWidth = Gdx.graphics.width,
	logicalHeight = Gdx.graphics.height,
	backBufferWidth = Gdx.graphics.backBufferWidth,
	backBufferHeight = Gdx.graphics.backBufferHeight,
	density = Gdx.graphics.density,
	osContentScale = runCatching { MetaInject.inject<MonitorHandler>().osContentScale }.getOrDefault(1f),
)

internal fun suggestedUiScale(
	logicalWidth: Int,
	logicalHeight: Int,
	backBufferWidth: Int,
	backBufferHeight: Int,
	density: Float,
	osContentScale: Float = 1f,
): Float {
	if (logicalWidth <= 0 || logicalHeight <= 0 || backBufferWidth <= 0 || backBufferHeight <= 0) return 1f
	val contentScaleX = backBufferWidth.toFloat() / logicalWidth
	val contentScaleY = backBufferHeight.toFloat() / logicalHeight
	// The framebuffer is already bigger than the window, so the platform is scaling for us - macOS Retina. Scaling
	// again here would double it.
	if (contentScaleX > 1.1f || contentScaleY > 1.1f) return 1f
	// Windows is the opposite case and the one that used to fall through to "no scaling". A DPI-aware process gets
	// a framebuffer exactly the size it asked for, so the ratio above is always 1.0 no matter what the display
	// settings say - the window is simply physically smaller on a dense panel. The OS scale is the only signal
	// that the user asked for a larger interface, and matching it is what every other application on the desktop
	// does. Clamped because a bad platform reading should not make the UI unusable in either direction.
	if (osContentScale > 1.05f) return osContentScale.coerceIn(1f, MAX_OS_CONTENT_SCALE)
	// No OS scaling to follow. Resolution or EDID density alone is ambiguous, so require both and reject
	// implausible EDID values.
	if (density !in 1.4f..4f) return 1f
	return when {
		backBufferWidth >= 5120 && backBufferHeight >= 2880 && density >= 2f -> 1.5f
		backBufferWidth >= 3840 && backBufferHeight >= 2160 -> 1.25f
		else -> 1f
	}
}

/** Windows offers up to 350% in its own settings; beyond that a reading is more likely wrong than extreme. */
private const val MAX_OS_CONTENT_SCALE = 4f

class MetaUIRenderer : UIRenderer {
	private var focusedActor: Actor? = null
	private val metaInput: MetaInputProcessor by lazyInject()
	private val spriteBatch: SpriteBatch by lazyInject()
	private val focusRenderer: FocusRenderer by lazyInject()
	private val fontProvider: FontProvider by lazyInject()
	private val uiControlHelper: UiControlHelper by lazyInject()

	private val stage: Stage = Stage(ScreenViewport(), spriteBatch)
	private val toastManager = MetaToastManager(stage)
	private val reactiveScope = ReactiveScope()
	private var loaded = false
	private var loadStarted = false
	private var loadingWithUI = false
	private var disposed = false
	private var startupTransitionPending = false
	private var startupTransitionActive = false
	private var startupTransitionElapsed = 0f
	private var startupTransitionDuration = 0f
	private var startupTransitionDelayFrames = 0
	private var startupTransitionPixel: Texture? = null
	private val startupTransitionColor = Color(0f, 0f, 0f, 1f)
	private var lastScrollHoverTarget: Actor? = null
	private var lastScrollHoverPane: Actor? = null
	private var lastReportedScrollFocus: Actor? = null

	/**
	 * True once anything other than this renderer has written [uiScale].
	 *
	 * Provenance, not value. Comparing against the last automatic scale looked equivalent and is not: a player who
	 * selects the value the renderer had already chosen - or returns to it later - is indistinguishable from
	 * nobody having chosen anything, and the next monitor change would overwrite a deliberate choice. Recording
	 * *who* wrote it cannot make that mistake.
	 *
	 * `internal` so a test can assert it. Whether a write was recorded is otherwise invisible - the case that
	 * matters most is the one where the value does not change - and a regression here is silent until someone's
	 * chosen scale is overwritten by moving a window.
	 */
	internal var scaleChosenByUser = false
		private set

	/** Set only while this renderer is assigning [uiScale], so its own write is not mistaken for a user's. */
	private var applyingAutomaticScale = false

	/** The pixel scale the current faces were rasterized at, so a back-buffer change can be noticed. */
	private var lastPixelScale: Float = Float.NaN

	private val backingUiScale: Signal<Float> = signal(1f) { a, b -> abs(a - b) < 0.001f }

	/**
	 * Wrapped so provenance is recorded at the write, not at the change.
	 *
	 * A signal suppresses a write equal to its current value - it returns before notifying - so a subscription
	 * never sees a player committing the scale that was already in effect. Watching for changes therefore misses
	 * the one case where the difference between "chosen" and "defaulted" matters most, and the next monitor
	 * transition would overwrite that choice. Every write lands here whether or not it changes anything.
	 */
	override val uiScale: Signal<Float> = object : Signal<Float> {
		override var value: Float
			get() = backingUiScale.value
			set(newValue) {
				if (!applyingAutomaticScale) scaleChosenByUser = true
				backingUiScale.value = newValue
			}

		override fun peek(): Float = backingUiScale.peek()
	}

	// The stage's world size already is physical-pixels ÷ unitsPerPixel (= ÷ uiScale) — i.e. UI units.
	override val uiWidth: Float get() = stage.width
	override val uiHeight: Float get() = stage.height

	/** Applies the current [uiScale] to the viewport (fewer UI units per pixel = larger UI) and re-lays-out. */
	private fun applyViewport(width: Int, height: Int) {
		(stage.viewport as ScreenViewport).unitsPerPixel = 1f / uiScale.value.coerceAtLeast(0.25f)
		stage.viewport.update(width, height, true)
	}

	init {
		log.debug { "Injected MetaUi." }
	}

	override fun load() {
		check(!disposed) { "A disposed UI renderer cannot be loaded again" }
		if (loaded) return
		beginLoad()
		if (loadingWithUI) {
			while (!MetaSkin.updateIncrementalInitialize(Int.MAX_VALUE)) {
				// Synchronous compatibility path.
			}
		}
		completeLoad()
	}

	override fun updateLoad(millis: Int): Boolean {
		check(!disposed) { "A disposed UI renderer cannot be loaded again" }
		if (loaded) return true
		if (millis <= 0) return false
		beginLoad()
		if (loadingWithUI && !MetaSkin.updateIncrementalInitialize(millis)) return false
		completeLoad()
		return true
	}

	private fun beginLoad() {
		if (loadStarted) return
		loadStarted = true
		loadingWithUI = MetaAudioVideoState.state.value.runWithUI
		log.trace { "load with UI enabled = $loadingWithUI" }
		if (loadingWithUI) MetaSkin.beginIncrementalInitialize()
	}

	private fun completeLoad() {
		if (loaded) return
		loaded = true
		if (loadingWithUI) {
			MetaFileChooser.setDefaultPrefsName("de.fatox.meta")
			toastManager.flushPending()
			log.debug { "Loaded Meta UI." }
		}

		stage.root.addCaptureListener(object : InputListener() {
			override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
				val target = event.target
				val pane = target.nearestScrollableMetaScrollPane()
				val focusBefore = stage.scrollFocus
				updateMetaScrollFocus(stage, event.target)
				val focusAfter = stage.scrollFocus
				if (target !== lastScrollHoverTarget || pane !== lastScrollHoverPane ||
					focusAfter !== lastReportedScrollFocus
				) {
					log.debug {
						"scroll-hover target=${target.scrollDebugPath()} pane=${pane?.scrollDebugState() ?: "-"} " +
							"focus=${focusBefore.scrollDebugPath()}->${focusAfter.scrollDebugPath()}"
					}
					lastScrollHoverTarget = target
					lastScrollHoverPane = pane
					lastReportedScrollFocus = focusAfter
				}
				return false
			}

			override fun scrolled(
				event: InputEvent,
				x: Float,
				y: Float,
				amountX: Float,
				amountY: Float,
			): Boolean {
				val hit = stage.hit(event.stageX, event.stageY, true)
				val pane = hit.nearestScrollableMetaScrollPane()
				log.debug {
					"scroll-wheel hit=${hit.scrollDebugPath()} routed=${event.target.scrollDebugPath()} " +
						"pane=${pane?.scrollDebugState() ?: "-"} focus=${stage.scrollFocus.scrollDebugPath()} " +
						"dx=$amountX dy=$amountY"
				}
				return false
			}

			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
				uiControlHelper.focusFromPointer(event.target)
				if (!event.target.isInside<TextField>()) stage.keyboardFocus = null
				if (!event.target.isInside<ScrollPane>()) stage.scrollFocus = null
				return false
			}
		})
		metaInput.addGlobalInputProcessor(stage)

		// HiDPI: every consumer gets DPI-correct UI by default (no per-game wiring). Re-apply live on any uiScale
		// change (e.g. a settings slider), and seed the default from the display. Games may override uiScale.value
		// afterwards with a user-chosen / persisted value.
		reactiveScope.subscribe(backingUiScale) {
			applyViewport(Gdx.graphics.width, Gdx.graphics.height)
			regenerateForPixelScale()
		}
		applySuggestedScale()
		applyViewport(Gdx.graphics.width, Gdx.graphics.height)
		val g = Gdx.graphics
		log.debug {
			"UI scale = ${uiScale.value} | logical ${g.width}x${g.height} | backbuffer ${g.backBufferWidth}x" +
				"${g.backBufferHeight} | contentScale ${g.backBufferWidth.toFloat() / g.width} | density " +
				"${g.density} | osScale ${runCatching { MetaInject.inject<MonitorHandler>().osContentScale }
					.getOrDefault(1f)}"
		}
	}

	override fun refreshStartupDisplay() {
		if (!loaded || disposed) return
		applySuggestedScale()
		applyViewport(Gdx.graphics.width, Gdx.graphics.height)
		// The same pixel-scale check `resize` makes, because this is a second way in and startup is where the
		// display is most likely to move. An application restores its saved display mode from `onLoaded`, the
		// splash calls this immediately afterwards, and the splash's own `resize` does not forward to the renderer
		// - so on a mode change that alters the back-buffer ratio without altering the logical scale, this is the
		// only chance to notice before the first screen is handed over holding faces from the old atlas.
		if (pixelScaleChanged()) regenerateForPixelScale()
	}

	/**
	 * Re-evaluates the automatic scale, unless something else has set one.
	 *
	 * A scale the game or the player set is left alone. That is decided by who wrote the value rather than by what
	 * the value is, because the two are not the same question: choosing the scale the renderer had already picked,
	 * or returning to it later, is a deliberate choice that value comparison cannot see.
	 */
	private fun applySuggestedScale() {
		if (scaleChosenByUser) return
		applyingAutomaticScale = true
		try {
			uiScale.value = suggestedUiScale()
		} finally {
			applyingAutomaticScale = false
		}
	}

	override fun armStartupTransition(durationSeconds: Float, delayFrames: Int) {
		if (durationSeconds <= 0f) return
		startupTransitionDuration = durationSeconds
		startupTransitionElapsed = 0f
		startupTransitionDelayFrames = delayFrames.coerceAtLeast(0)
		startupTransitionPending = true
		startupTransitionActive = false
		createStartupTransitionPixel()
	}

	private inline fun <reified T : Actor> Actor?.isInside(): Boolean {
		var current = this
		while (current != null) {
			if (current is T) return true
			current = current.parent
		}
		return false
	}

	override fun cancelTouchFocus() = stage.cancelTouchFocus()

	override fun addActor(actor: Actor) {
		try {
			stage.addActor(actor)
			// Newly added windows/dialogs land on top of the toast layer; lift toasts back above them.
			toastManager.toFront()
			MetaTooltip.bringVisibleToFront()
		} catch (e: Exception) {
			log.error(e) { "Failed to add actor: $actor!" }
		}
	}

	override fun update() {
		stage.act(Gdx.graphics.deltaTime)
	}

	override fun draw() {
		if (!MetaAudioVideoState.state.value.runWithUI) return
		if (startupTransitionPending) {
			startupTransitionPending = false
			startupTransitionActive = true
			startupTransitionElapsed = 0f
		}

		stage.batch.setBlendFunction(-1, -1)
		Gdx.gl.glBlendFuncSeparate(
			GL20.GL_SRC_ALPHA,
			GL20.GL_ONE_MINUS_SRC_ALPHA,
			GL20.GL_ONE,
			GL20.GL_ONE_MINUS_SRC_ALPHA
		)

		val deltaTime = Gdx.graphics.deltaTime
		MetaTooltip.bringVisibleToFront()
		stage.draw()
		focusRenderer.draw(stage, focusedActor, deltaTime)
		drawStartupTransition(deltaTime)
	}

	private fun drawStartupTransition(deltaTime: Float) {
		if (!startupTransitionActive) return
		if (startupTransitionDelayFrames > 0) {
			startupTransitionDelayFrames--
			startupTransitionColor.a = 1f
			drawStartupTransitionCover()
			return
		}
		val progress = (startupTransitionElapsed / startupTransitionDuration).coerceIn(0f, 1f)
		val eased = progress * progress * (3f - 2f * progress)
		startupTransitionColor.a = 1f - eased
		drawStartupTransitionCover()
		// The first frame after a native display change can report either zero or the whole resize stall as its
		// delta. Do not let either value freeze the cover forever or skip most of the fade in one frame.
		val fadeDelta = if (deltaTime.isFinite() && deltaTime > 0f) {
			deltaTime.coerceAtMost(1f / 30f)
		} else {
			1f / 60f
		}
		startupTransitionElapsed += fadeDelta
		if (startupTransitionElapsed >= startupTransitionDuration) startupTransitionActive = false
	}

	private fun drawStartupTransitionCover() {
		spriteBatch.projectionMatrix.set(stage.camera.combined)
		spriteBatch.begin()
		spriteBatch.setColor(startupTransitionColor)
		spriteBatch.draw(startupTransitionPixel!!, 0f, 0f, uiWidth, uiHeight)
		spriteBatch.color = Color.WHITE
		spriteBatch.end()
	}

	private fun createStartupTransitionPixel() {
		if (startupTransitionPixel != null) return
		val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
		pixmap.setColor(Color.WHITE)
		pixmap.fill()
		startupTransitionPixel = Texture(pixmap)
		pixmap.dispose()
	}

	override fun resize(width: Int, height: Int) {
		// A window dragged onto a monitor with different scaling arrives here, and on Windows that is the only
		// notification: the framebuffer matches the window on both monitors, so nothing about the size says the
		// density changed. Re-suggesting here is what makes the setting followed rather than sampled once.
		applySuggestedScale()
		applyViewport(width, height)
		// The logical scale is not the whole story. Fonts are rasterized at logical scale times the back-buffer
		// ratio, and on macOS that ratio changes between a Retina and a non-Retina monitor while the logical scale
		// stays put - so the move produces no signal at all through uiScale, and a manual scale suppresses it
		// outright. Left alone, on-stage widgets keep fonts rasterized for the old pixel ratio and draw blurred.
		//
		// Worse than blurred, in fact: the provider orphans its caches the moment anything asks for a font at the
		// new ratio, and a later disposeOrphanedFonts would free faces those widgets are still drawing from.
		if (pixelScaleChanged()) regenerateForPixelScale()
		toastManager.resize()
	}

	/**
	 * The scale fonts are actually rasterized at: the logical scale times the back-buffer ratio.
	 *
	 * Mirrors `MetaFontProvider.physicalUiScale`, which is the number that decides whether cached faces are stale.
	 */
	private fun currentPixelScale(): Float {
		val graphics = Gdx.graphics
		val contentScale = if (graphics.width > 0) graphics.backBufferWidth.toFloat() / graphics.width else 1f
		return (uiScale.peek() * contentScale).coerceAtLeast(0.01f)
	}

	private fun pixelScaleChanged(): Boolean {
		val current = currentPixelScale()
		if (lastPixelScale.isNaN() || abs(current - lastPixelScale) > 0.001f) {
			lastPixelScale = current
			return true
		}
		return false
	}

	/**
	 * Moves the atlas to a fresh page, re-fetches every widget's font, then releases the old faces.
	 *
	 * The order is the whole of it. The atlas moves first so the glyphs rasterized during the walk land on the new
	 * page and the previous set goes with the page that is dropped - the only way this atlas reclaims anything,
	 * since PixmapPacker cannot free a region. Disposal is last, once nothing on stage still references a face.
	 */
	private fun regenerateForPixelScale() {
		lastPixelScale = currentPixelScale()
		MetaSkin.rebuildAtlas()
		stage.root.refreshFontsRecursively()
		fontProvider.disposeOrphanedFonts()
	}

	override fun getCamera(): Camera {
		return stage.camera
	}

	override fun getToastManager(): MetaToastManager {
		return toastManager
	}

	override fun setFocusedActor(actor: Actor?) {
		focusedActor = MetaFocus.assign(focusedActor, actor)
	}

	override fun dispose() {
		if (disposed) return
		disposed = true
		setFocusedActor(null)
		reactiveScope.dispose()
		if (loaded) metaInput.removeGlobalInputProcessor(stage)
		focusRenderer.dispose()
		stage.dispose()
		fontProvider.dispose()
		MetaSkin.dispose()
		startupTransitionPixel?.dispose()
		startupTransitionPixel = null
	}
}
