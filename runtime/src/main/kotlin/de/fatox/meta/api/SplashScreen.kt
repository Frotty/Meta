package de.fatox.meta.api

import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.snapToPhysicalPixel
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.math.MathUtils
import de.fatox.meta.Meta
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.use
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaColor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val splashLog = MetaLoggerFactory.logger {}

/**
 * How much of itself the startup screen shows.
 *
 * [PANEL] is the informative one: a card carrying a mark, a title, a subtitle, a message, a phase status, a spinner
 * and a progress bar. It suits an application whose startup the user is expected to read.
 *
 * [QUIET] is a title and one indicator, centred on the background and sized from the window. It suits an
 * application whose startup the user is only waiting through — a game, where a panel of loading telemetry reads as
 * something having gone wrong rather than as the game starting.
 */
enum class SplashStyle { PANEL, QUIET }

/**
 * The startup screen's colours, for an application that does not want Meta's.
 *
 * Defaults to the theme, so a splash that says nothing about colour looks exactly as it did. A game generally does
 * want to say something: the splash is the first thing on screen and a hand-off into a menu of a different hue reads
 * as two programs starting rather than one.
 *
 * Copied on construction, because libGDX's [Color] is mutable and these are held for the life of the screen.
 */
class SplashPalette(
	background: Color = MetaColor.BACKGROUND,
	title: Color = MetaColor.TEXT,
	accent: Color = MetaColor.ACCENT,
	track: Color = MetaColor.BORDER,
) {
	val background: Color = Color(background)
	val title: Color = Color(title)
	val accent: Color = Color(accent)
	val track: Color = Color(track)
}

/**
 * Static copy for the startup panel. Font files must be available before application assets are prepared, so keep
 * custom splash fonts as loose or classpath bootstrap resources rather than relying on a later-mounted asset archive.
 */
data class SplashPresentation(
	val mark: String = "M",
	val title: String = "META",
	val subtitle: String = "APPLICATION FRAMEWORK",
	val message: String = "PREPARING YOUR WORKSPACE",
	val startingStatus: String = "STARTING",
	val preparationStatus: String = "DISCOVERING CONTENT",
	val queueStatus: String = "BUILDING LOAD QUEUE",
	val assetStatus: String = "LOADING ASSETS",
	val interfaceStatus: String = "BUILDING INTERFACE",
	val applicationStatus: String = "PREPARING APPLICATION",
	val readyStatus: String = "READY",
	val transition: SplashTransitionConfiguration = SplashTransitionConfiguration(),
	/**
	 * Which of the two presentations to draw. [SplashStyle.PANEL] by default, so an existing splash is unchanged.
	 *
	 * [SplashStyle.QUIET] uses only [title] and the transition timings; the mark, subtitle, message and status
	 * strings are not drawn, and the title is sized from the window rather than fixed, so it does not shrink into a
	 * corner of a large one.
	 */
	val style: SplashStyle = SplashStyle.PANEL,
	/** Colours to draw with. Defaults to Meta's theme. */
	val palette: SplashPalette = SplashPalette(),
)

/** Panel geometry and transition timings shared by launchers and [SplashScreen]. */
data class SplashTransitionConfiguration(
	val bootstrapWidth: Int = 860,
	val bootstrapHeight: Int = 320,
	/**
	 * Draw the panel as a centred banner of [bootstrapWidth] × [bootstrapHeight] rather than filling the window.
	 *
	 * Off by default, so an existing splash keeps the full-window panel it has. On, the panel becomes what its
	 * surface, border and accent strip already imply — a card on the background — which suits an application whose
	 * window is the size it will be played at rather than an IDE opening its workspace. Clamped to the window, so a
	 * banner wider than the window is simply the window.
	 */
	val banner: Boolean = false,
	val fadeInDuration: Float = 0.5f,
	val fadeOutDuration: Float = 0.5f,
	val minimumHoldDuration: Float = 0.2f,
	val uiFadeInDuration: Float = 0.35f,
	val uiFadeInDelayFrames: Int = 2,
) {
	init {
		require(bootstrapWidth > 0 && bootstrapHeight > 0) { "Splash bootstrap dimensions must be positive" }
		require(fadeInDuration > 0f && fadeOutDuration > 0f && minimumHoldDuration >= 0f && uiFadeInDuration >= 0f) {
			"Splash transition durations must be non-negative, with non-zero fade durations"
		}
		require(uiFadeInDelayFrames >= 0) { "Splash UI fade delay must not be negative" }
	}
}

/** A font resource which is available during the application's bootstrap phase. */
data class SplashFont(
	val path: String,
	val fileType: Files.FileType = Files.FileType.Internal,
)

/**
 * Optional font choices for splash headings and supporting copy.
 * A null face uses libGDX's bundled bitmap font and needs no application asset.
 */
data class SplashFontConfiguration(
	val title: SplashFont? = null,
	val body: SplashFont? = null,
)

/**
 * The application's own startup work, advanced a slice at a time.
 *
 * Meta can only spread out the loading it owns: [AssetProvider.update] for queued assets and
 * [de.fatox.meta.api.ui.UIRenderer.updateLoad] for generated UI resources. Work that belongs to the application
 * itself — its own faces, its atlases, a scene it wants standing before the first screen appears — has nowhere to
 * go but the loaded callback, where it costs one frozen frame for as long as it takes. That is the freeze this
 * replaces: [SplashScreen] calls [update] once per frame with a soft budget and keeps the panel animating until it
 * returns true.
 *
 * Calls happen on the GL thread after Meta's assets and UI resources are ready, so a step may create textures,
 * fonts and scene2d actors — which is the point, since that is the work with nowhere else to run. A step that
 * overruns its budget still costs one long frame, so divide the work into steps rather than trusting the budget.
 */
fun interface SplashStartupLoad {
	/** Advances startup work for up to [millis], then reports whether anything is left. */
	fun update(millis: Int): Boolean
}

/**
 * Every callback [SplashScreen] can drive, named at the call site.
 *
 * The constructor overloads cover combinations of two and three callbacks positionally, which is already as far as
 * that reads clearly. This is the same set as one argument, so a caller can supply any subset — and adding a
 * callback here does not widen that matrix again.
 */
data class SplashCallbacks(
	/** Discovery and queue construction, run in order on a low-priority worker. Neither may touch GL or scene2d. */
	val prepareAssets: (() -> Unit)? = null,
	val queueAssets: (() -> Unit)? = null,
	/** The application's own GL-thread startup work, advanced in slices. See [SplashStartupLoad]. */
	val startupLoad: SplashStartupLoad? = null,
	/** Runs on the GL thread once loading is done, one frame before the panel starts fading out. */
	val beforeFadeOut: (() -> Unit)? = null,
	/** Runs on the GL thread after the panel has faded out. Hand over to the first real screen here. */
	val onLoaded: () -> Unit,
)

/**
 * Lightweight startup screen which only uses Meta's shared [SpriteBatch].
 *
 * The single-callback constructor preserves the original GL-thread contract. Prefer the two-callback constructor:
 * [queueAssets] runs once on the GL thread and should queue work through [AssetProvider.load], then the splash advances
 * that queue in frame-adaptive slices and calls [onLoaded] on the GL thread when loading completes. If folder/XPK
 * discovery or queue construction is substantial, use the three-callback constructor: [prepareAssets] and
 * [queueAssets] run sequentially on a low-priority worker before GL-thread updates begin; [onLoaded] remains on the
 * GL thread. This worker mode requires queueing operations that do not touch OpenGL or scene2d.
 *
 * An application whose own startup work is expensive should pass [SplashCallbacks.startupLoad] as well, so that work
 * is advanced in slices between the UI resources and the fade-out instead of blocking [onLoaded]. Whatever is left in
 * [onLoaded] runs while the panel is gone, so keep it to handing over the first screen.
 *
 * Does not require the application to extend [Meta]. Everything it draws with comes from the injection graph, so a
 * game that only adopts Meta's UI layer can still show it.
 */
class SplashScreen private constructor(
	private val onLoaded: () -> Unit,
	private val assetQueue: AssetQueue?,
	private val assetPreparation: AssetPreparation?,
	private val presentation: SplashPresentation,
	private val fontConfiguration: SplashFontConfiguration,
	private val beforeFadeOut: (() -> Unit)? = null,
	private val startupLoad: SplashStartupLoad? = null,
) : ScreenAdapter() {
	/** Any combination of the callbacks, including [SplashCallbacks.startupLoad], which has no positional form. */
	constructor(
		callbacks: SplashCallbacks,
		presentation: SplashPresentation = SplashPresentation(),
		fonts: SplashFontConfiguration = SplashFontConfiguration(),
	) : this(
		callbacks.onLoaded,
		callbacks.queueAssets?.let { AssetQueue(it) },
		callbacks.prepareAssets?.let { AssetPreparation(it) },
		presentation,
		fonts,
		callbacks.beforeFadeOut,
		callbacks.startupLoad,
	)

	constructor(onLoaded: () -> Unit) : this(onLoaded, null, null, SplashPresentation(), SplashFontConfiguration())
	constructor(presentation: SplashPresentation, onLoaded: () -> Unit) :
		this(onLoaded, null, null, presentation, SplashFontConfiguration())
	constructor(presentation: SplashPresentation, fonts: SplashFontConfiguration, onLoaded: () -> Unit) :
		this(onLoaded, null, null, presentation, fonts)
	constructor(queueAssets: () -> Unit, onLoaded: () -> Unit) :
		this(onLoaded, AssetQueue(queueAssets), null, SplashPresentation(), SplashFontConfiguration())
	constructor(presentation: SplashPresentation, queueAssets: () -> Unit, onLoaded: () -> Unit) :
		this(onLoaded, AssetQueue(queueAssets), null, presentation, SplashFontConfiguration())
	constructor(
		presentation: SplashPresentation,
		fonts: SplashFontConfiguration,
		queueAssets: () -> Unit,
		onLoaded: () -> Unit,
	) : this(onLoaded, AssetQueue(queueAssets), null, presentation, fonts)
	constructor(prepareAssets: () -> Unit, queueAssets: () -> Unit, onLoaded: () -> Unit) :
		this(
			onLoaded,
			AssetQueue(queueAssets),
			AssetPreparation(prepareAssets),
			SplashPresentation(),
			SplashFontConfiguration(),
		)
	constructor(
		presentation: SplashPresentation,
		prepareAssets: () -> Unit,
		queueAssets: () -> Unit,
		onLoaded: () -> Unit,
	) : this(
		onLoaded,
		AssetQueue(queueAssets),
		AssetPreparation(prepareAssets),
		presentation,
		SplashFontConfiguration(),
	)
	constructor(
		presentation: SplashPresentation,
		fonts: SplashFontConfiguration,
		prepareAssets: () -> Unit,
		queueAssets: () -> Unit,
		onLoaded: () -> Unit,
	) : this(onLoaded, AssetQueue(queueAssets), AssetPreparation(prepareAssets), presentation, fonts)
	constructor(
		presentation: SplashPresentation,
		fonts: SplashFontConfiguration,
		prepareAssets: () -> Unit,
		queueAssets: () -> Unit,
		beforeFadeOut: () -> Unit,
		onLoaded: () -> Unit,
	) : this(onLoaded, AssetQueue(queueAssets), AssetPreparation(prepareAssets), presentation, fonts, beforeFadeOut)

	private val spriteBatch: SpriteBatch by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()
	private val fontProvider: FontProvider by lazyInject("default")
	private var ringTexture: Texture? = null
	private var pixelTexture: Texture? = null
	private var titleFont: BitmapFont? = null
	private var bodyFont: BitmapFont? = null
	private var detailFont: BitmapFont? = null
	private var textPixelScale = 1f
	/** The logical title size the current face was built at, so a resize can rebuild it. */
	private var titleFontSize = 0
	private var markText: SplashText? = null
	private var titleText: SplashText? = null
	private var subtitleText: SplashText? = null
	private var messageText: SplashText? = null
	private var statusText: SplashText? = null
	private var displayedStatus: String? = null
	private var panelX = 0f
	private var panelY = 0f
	private var panelWidth = 0f
	private var panelHeight = 0f
	private var elapsed = 0f
	private var phaseElapsed = 0f
	private var phase = SplashPhase.FADE_IN
	private var advancing = false
	private var transitionStarted = false
	private var transitionPreparationStarted = false
	private var skippedLoadingFrames = 0
	@Volatile private var preparationComplete = false
	@Volatile private var preparationFailure: Throwable? = null

	override fun show() {
		createTextures()
		createText()
		updateProjection()
		// Null when the application does not extend Meta: this screen only needs the shared SpriteBatch, the asset
		// provider and the UI renderer, all of which come from the injection graph, so a game that uses Meta's UI
		// layer without its Game class can show it. There is then no window handler to raise.
		Meta.instanceOrNull?.windowHandler?.focus()
	}

	override fun dispose() {
		ringTexture?.dispose()
		ringTexture = null
		pixelTexture?.dispose()
		pixelTexture = null
		titleFont?.dispose()
		titleFont = null
		bodyFont?.dispose()
		bodyFont = null
		detailFont?.dispose()
		detailFont = null
		markText = null
		titleText = null
		subtitleText = null
		messageText = null
		statusText = null
		displayedStatus = null
	}

	override fun hide() = dispose()

	override fun render(delta: Float) {
		val animationDelta = delta.coerceIn(0f, MAX_DELTA)
		elapsed += animationDelta
		phaseElapsed += animationDelta
		val visualAlpha = visualAlpha()

		Gdx.gl.apply {
			// Window dimensions are logical points on Retina/HiDPI displays; HdpiUtils maps them to framebuffer pixels.
			HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
			val background = presentation.palette.background
			glClearColor(
				background.r * visualAlpha,
				background.g * visualAlpha,
				background.b * visualAlpha,
				1f,
			)
			glClear(GL20.GL_COLOR_BUFFER_BIT)
		}

		when (presentation.style) {
			SplashStyle.PANEL -> drawLoadingPanel(visualAlpha)
			SplashStyle.QUIET -> drawQuietTitle(visualAlpha)
		}
		advanceLoading(delta)
	}

	override fun resize(width: Int, height: Int) {
		updateProjection()
	}

	private fun drawLoadingPanel(visualAlpha: Float) {
		val spinnerCenterX = panelX + panelWidth - PANEL_PADDING - SPINNER_SIZE * 0.5f
		val spinnerCenterY = panelY + panelHeight - PANEL_PADDING - SPINNER_SIZE * 0.5f
		val spinnerAngle = -elapsed * ROTATION_DEGREES_PER_SECOND
		val barX = panelX + PANEL_PADDING
		val barY = panelY + PROGRESS_BOTTOM
		val barWidth = panelWidth - PANEL_PADDING * 2f

		spriteBatch.use {
			spriteBatch.setColor(
				MetaColor.SURFACE.r,
				MetaColor.SURFACE.g,
				MetaColor.SURFACE.b,
				visualAlpha,
			)
			spriteBatch.draw(pixelTexture!!, panelX, panelY, panelWidth, panelHeight)
			drawPanelBorder(visualAlpha)
			spriteBatch.setColor(MetaColor.ACCENT.r, MetaColor.ACCENT.g, MetaColor.ACCENT.b, visualAlpha)
			spriteBatch.draw(pixelTexture!!, panelX, panelY + panelHeight - ACCENT_HEIGHT, panelWidth, ACCENT_HEIGHT)

			val markX = panelX + PANEL_PADDING
			val markY = panelY + panelHeight - PANEL_PADDING - MARK_SIZE
			spriteBatch.setColor(MetaColor.PRIMARY.r, MetaColor.PRIMARY.g, MetaColor.PRIMARY.b, visualAlpha)
			spriteBatch.draw(pixelTexture!!, markX, markY, MARK_SIZE, MARK_SIZE)

			spriteBatch.setColor(
				MetaColor.BORDER.r,
				MetaColor.BORDER.g,
				MetaColor.BORDER.b,
				DIVIDER_ALPHA * visualAlpha,
			)
			spriteBatch.draw(
				pixelTexture!!,
				panelX + PANEL_PADDING,
				panelY + DIVIDER_BOTTOM,
				panelWidth - PANEL_PADDING * 2f,
				1f,
			)

			spriteBatch.setColor(MetaColor.ACCENT.r, MetaColor.ACCENT.g, MetaColor.ACCENT.b, visualAlpha)
			spriteBatch.draw(
				ringTexture!!,
				spinnerCenterX - SPINNER_SIZE * 0.5f,
				spinnerCenterY - SPINNER_SIZE * 0.5f,
				SPINNER_SIZE * 0.5f,
				SPINNER_SIZE * 0.5f,
				SPINNER_SIZE,
				SPINNER_SIZE,
				1f,
				1f,
				spinnerAngle,
				0,
				0,
				RING_TEXTURE_SIZE,
				RING_TEXTURE_SIZE,
				false,
				false,
			)

			spriteBatch.setColor(MetaColor.BORDER.r, MetaColor.BORDER.g, MetaColor.BORDER.b, TRACK_ALPHA * visualAlpha)
			spriteBatch.draw(pixelTexture!!, barX, barY, barWidth, BAR_HEIGHT)
			spriteBatch.setColor(MetaColor.ACCENT.r, MetaColor.ACCENT.g, MetaColor.ACCENT.b, visualAlpha)
			if (assetQueue != null && phase == SplashPhase.LOADING) {
				spriteBatch.draw(
					pixelTexture!!,
					barX,
					barY,
					barWidth * assetProvider.progress.coerceIn(0f, 1f),
					BAR_HEIGHT,
				)
			} else {
				val segmentWidth = minOf(BAR_SEGMENT_WIDTH, barWidth)
				val travel = barWidth - segmentWidth
				val barProgress = (MathUtils.sin(elapsed * BAR_SPEED) + 1f) * 0.5f
				spriteBatch.draw(pixelTexture!!, barX + travel * barProgress, barY, segmentWidth, BAR_HEIGHT)
			}
			spriteBatch.color = Color.WHITE
			markText?.cache?.draw(spriteBatch, visualAlpha)
			titleText?.cache?.draw(spriteBatch, visualAlpha)
			subtitleText?.cache?.draw(spriteBatch, visualAlpha)
			messageText?.cache?.draw(spriteBatch, visualAlpha)
			statusText?.cache?.draw(spriteBatch, visualAlpha)
		}
	}

	/**
	 * A title and one indicator, centred, sized from the window.
	 *
	 * No surface, border, mark or status line: the point of this style is that nothing is on screen except the name
	 * of the thing being started and evidence that it is still starting. The bar sweeps at a fraction of the panel's
	 * speed — a fast indeterminate bar reads as activity, a slow one reads as patience.
	 */
	private fun drawQuietTitle(visualAlpha: Float) {
		val width = Gdx.graphics.width.toFloat()
		val height = Gdx.graphics.height.toFloat()
		val barWidth = (width * QUIET_BAR_WIDTH_FRACTION).coerceAtMost(width - QUIET_BAR_MIN_MARGIN * 2f)
		val barX = (width - barWidth) * 0.5f
		val barY = height * QUIET_BAR_HEIGHT_FRACTION
		val barHeight = (height * QUIET_BAR_THICKNESS_FRACTION).coerceAtLeast(1f)

		spriteBatch.use {
			val track = presentation.palette.track
			val accent = presentation.palette.accent
			spriteBatch.setColor(track.r, track.g, track.b, TRACK_ALPHA * visualAlpha)
			spriteBatch.draw(pixelTexture!!, barX, barY, barWidth, barHeight)
			spriteBatch.setColor(accent.r, accent.g, accent.b, visualAlpha)
			if (assetQueue != null && phase == SplashPhase.LOADING) {
				spriteBatch.draw(
					pixelTexture!!,
					barX,
					barY,
					barWidth * assetProvider.progress.coerceIn(0f, 1f),
					barHeight,
				)
			} else {
				val segmentWidth = barWidth * QUIET_BAR_SEGMENT_FRACTION
				val travel = barWidth - segmentWidth
				val sweep = (MathUtils.sin(elapsed * QUIET_BAR_SPEED) + 1f) * 0.5f
				spriteBatch.draw(pixelTexture!!, barX + travel * sweep, barY, segmentWidth, barHeight)
			}
			spriteBatch.color = Color.WHITE
			titleText?.cache?.draw(spriteBatch, visualAlpha)
		}
	}

	private fun drawPanelBorder(visualAlpha: Float) {
		spriteBatch.setColor(
			MetaColor.BORDER_STRONG.r,
			MetaColor.BORDER_STRONG.g,
			MetaColor.BORDER_STRONG.b,
			BORDER_ALPHA * visualAlpha,
		)
		spriteBatch.draw(pixelTexture!!, panelX, panelY, panelWidth, BORDER_WIDTH)
		spriteBatch.draw(pixelTexture!!, panelX, panelY + panelHeight - BORDER_WIDTH, panelWidth, BORDER_WIDTH)
		spriteBatch.draw(pixelTexture!!, panelX, panelY, BORDER_WIDTH, panelHeight)
		spriteBatch.draw(pixelTexture!!, panelX + panelWidth - BORDER_WIDTH, panelY, BORDER_WIDTH, panelHeight)
	}

	private fun advanceLoading(frameDelta: Float) {
		// Re-entrant renders are normal, and must not advance anything. Applying a display mode pumps the platform
		// window, and on LWJGL3 that runs a frame — so this screen's own render() lands on the stack inside whatever
		// callback asked for the change. Unguarded, the next phase or the next startup slice began while the current
		// one was still running, which reordered the work and lost the completion of the step that was interrupted.
		// The nested frame still draws the panel, which is the point of the window pumping in the first place.
		if (advancing) return
		advancing = true
		try {
			advancePhase(frameDelta)
		} finally {
			advancing = false
		}
	}

	private fun advancePhase(frameDelta: Float) {
		when (phase) {
			SplashPhase.FADE_IN -> if (phaseElapsed >= presentation.transition.fadeInDuration && Meta.canChangeScreen()) {
				if (assetPreparation == null) enterPhase(SplashPhase.QUEUEING) else startPreparation()
			}
			SplashPhase.PREPARING -> {
				preparationFailure?.let { throw it }
				if (preparationComplete) {
					if (assetQueue == null) enterPhase(SplashPhase.UI_LOADING) else enterPhase(SplashPhase.LOADING)
				}
			}
			SplashPhase.QUEUEING -> {
				assetQueue?.task?.invoke()
				if (assetQueue == null) enterPhase(SplashPhase.UI_LOADING) else enterPhase(SplashPhase.LOADING)
			}
			SplashPhase.LOADING -> {
				val budgetMillis = loadingBudgetMillis(frameDelta)
				if (assetProvider.update(budgetMillis)) enterPhase(SplashPhase.UI_LOADING)
			}
			SplashPhase.UI_LOADING -> {
				val budgetMillis = loadingBudgetMillis(frameDelta)
				if (uiRenderer.updateLoad(budgetMillis)) {
					enterPhase(if (startupLoad == null) SplashPhase.HOLD else SplashPhase.APP_LOADING)
				}
			}
			SplashPhase.APP_LOADING -> {
				val work = startupLoad
				val budgetMillis = loadingBudgetMillis(frameDelta)
				if (work == null || work.update(budgetMillis)) enterPhase(SplashPhase.HOLD)
			}
			SplashPhase.HOLD -> if (phaseElapsed >= presentation.transition.minimumHoldDuration) {
				if (!transitionPreparationStarted) {
					transitionPreparationStarted = true
					beforeFadeOut?.invoke()
				} else {
					enterPhase(SplashPhase.FADE_OUT)
				}
			}
			SplashPhase.FADE_OUT -> if (phaseElapsed >= presentation.transition.fadeOutDuration) {
				enterPhase(SplashPhase.COMPLETE)
				completeLoading()
			}
			SplashPhase.COMPLETE -> Unit
		}
	}

	private fun startPreparation() {
		enterPhase(SplashPhase.PREPARING)
		val preparation = assetPreparation ?: return
		// Resolved here, on the GL thread, and captured — so the worker below calls a method on an object that
		// already exists instead of reaching into the injection graph. `MetaInject` keeps its singletons in
		// unsynchronized maps and hands out `LazyThreadSafetyMode.NONE` delegates, so a worker resolving a service
		// while this thread resolves another can build two of something or publish one half-initialized.
		val fonts = fontProvider
		Thread({
			try {
				preparation.task.invoke()
				assetQueue?.task?.invoke()
			} catch (failure: Throwable) {
				preparationFailure = failure
			} finally {
				preparationComplete = true
			}
			// After the phase has been released, and deliberately so. Loading waits on `preparationComplete`, so work
			// done before it is still serial with every frame that follows; this runs alongside them instead.
			//
			// After the application's callbacks for a second reason: opening a face resolves its path through the
			// asset provider, and the application's preparation is what indexes the tree those paths live in.
			//
			// Reading and parsing a TrueType file needs no graphics device — Meta's icon face is 599 KB — so it
			// belongs here. Rasterizing and uploading stay on the GL thread, which is why only the file work moves.
			// A face the GL thread reaches first is simply opened there; the provider serializes the two.
			runCatching { fonts.prepareFaces() }
				.onFailure { splashLog.warn("Could not pre-open the configured fonts; they open on first use", it) }
		}, PREPARATION_THREAD_NAME).apply {
			isDaemon = true
			priority = Thread.MIN_PRIORITY
			start()
		}
	}

	private fun enterPhase(next: SplashPhase) {
		phase = next
		phaseElapsed = 0f
		skippedLoadingFrames = 0
		updateStatusText()
	}

	private fun loadingBudgetMillis(frameDelta: Float): Int {
		val budget = SplashLoadingPolicy.updateBudgetMillis(frameDelta)
		if (budget > 0) {
			skippedLoadingFrames = 0
			return budget
		}
		skippedLoadingFrames++
		if (skippedLoadingFrames < MAX_SKIPPED_LOADING_FRAMES) return 0
		skippedLoadingFrames = 0
		return MINIMUM_PROGRESS_BUDGET_MS
	}

	private fun visualAlpha(): Float {
		val fadeDuration = when (phase) {
			SplashPhase.FADE_IN -> presentation.transition.fadeInDuration
			SplashPhase.FADE_OUT -> presentation.transition.fadeOutDuration
			else -> 1f
		}
		val progress = (phaseElapsed / fadeDuration).coerceIn(0f, 1f)
		return when (phase) {
			SplashPhase.FADE_IN -> SplashLoadingPolicy.smoothStep(progress)
			SplashPhase.FADE_OUT -> 1f - SplashLoadingPolicy.smoothStep(progress)
			SplashPhase.COMPLETE -> 0f
			else -> 1f
		}
	}

	private fun completeLoading() {
		if (transitionStarted) return
		transitionStarted = true
		uiRenderer.armStartupTransition(
			presentation.transition.uiFadeInDuration,
			presentation.transition.uiFadeInDelayFrames,
		)
		onLoaded.invoke()
		uiRenderer.refreshStartupDisplay()
	}

	private fun createTextures() {
		if (ringTexture == null) {
			val ringPixmap = Pixmap(RING_TEXTURE_SIZE, RING_TEXTURE_SIZE, Pixmap.Format.RGBA8888)
			ringPixmap.setBlending(Pixmap.Blending.None)
			ringPixmap.setColor(0f, 0f, 0f, 0f)
			ringPixmap.fill()
			for (y in 0 until RING_TEXTURE_SIZE) {
				for (x in 0 until RING_TEXTURE_SIZE) {
					val alpha = SplashRingTexturePainter.alphaAt(x, y, RING_TEXTURE_SIZE)
					if (alpha > 0f) {
						ringPixmap.setColor(1f, 1f, 1f, alpha)
						ringPixmap.drawPixel(x, y)
					}
				}
			}
			ringTexture = Texture(ringPixmap).apply {
				setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
			}
			ringPixmap.dispose()
		}
		if (pixelTexture == null) {
			val pixelPixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
			pixelPixmap.setColor(Color.WHITE)
			pixelPixmap.fill()
			pixelTexture = Texture(pixelPixmap)
			pixelPixmap.dispose()
		}
	}

	private fun createText() {
		val wantedTitleSize = wantedTitleSize()
		if (titleFont != null && wantedTitleSize == titleFontSize) return
		disposeFonts()
		val pixelScale = (
			Gdx.graphics.backBufferWidth.toFloat() / Gdx.graphics.width.coerceAtLeast(1)
			).coerceAtLeast(1f)
		textPixelScale = pixelScale
		titleFontSize = wantedTitleSize
		val fonts = configuredFonts(pixelScale, wantedTitleSize)
		titleFont = fonts.title
		bodyFont = fonts.body
		detailFont = fonts.detail

		titleText = createText(titleFont!!, presentation.title, presentation.palette.title)
		if (presentation.style == SplashStyle.PANEL) {
			markText = createText(titleFont!!, presentation.mark, MetaColor.TEXT)
			subtitleText = createText(detailFont!!, presentation.subtitle, MetaColor.TEXT_MUTED)
			messageText = createText(bodyFont!!, presentation.message, MetaColor.TEXT)
		}
		updateStatusText()
	}

	/**
	 * The logical title size for the current style.
	 *
	 * Fixed for a panel, whose whole layout is built around a known size. Proportional to the window height for a
	 * quiet splash, because a fixed 25 px title in a 2560x1600 window is a caption, not a title.
	 */
	private fun wantedTitleSize(): Int =
		SplashTitleSizing.forWindow(Gdx.graphics.height, presentation.style)

	private fun disposeFonts() {
		titleFont?.dispose()
		titleFont = null
		bodyFont?.dispose()
		bodyFont = null
		detailFont?.dispose()
		detailFont = null
		markText = null
		titleText = null
		subtitleText = null
		messageText = null
		statusText = null
		displayedStatus = null
	}

	private fun configuredFonts(pixelScale: Float, titleSize: Int): SplashFonts {
		val configuredTitle = configuredFont(fontConfiguration.title, titleSize, pixelScale, "title")
		val configuredBody = configuredBodyFonts(fontConfiguration.body, pixelScale)
		return SplashFonts(configuredTitle, configuredBody.first, configuredBody.second)
	}

	private fun configuredFont(
		source: SplashFont?,
		logicalSize: Int,
		pixelScale: Float,
		role: String,
	): BitmapFont {
		if (source == null) return fallbackFont(logicalSize)
		val generator = try {
			FreeTypeFontGenerator(source.fileHandle())
		} catch (error: Exception) {
			logFontFallback(source, role, error)
			return fallbackFont(logicalSize)
		}
		try {
			return generateFont(generator, logicalSize, pixelScale)
		} catch (error: Exception) {
			logFontFallback(source, role, error)
			return fallbackFont(logicalSize)
		} finally {
			generator.dispose()
		}
	}

	private fun configuredBodyFonts(source: SplashFont?, pixelScale: Float): Pair<BitmapFont, BitmapFont> {
		if (source == null) return fallbackFont(BODY_FONT_SIZE) to fallbackFont(DETAIL_FONT_SIZE)
		val generator = try {
			FreeTypeFontGenerator(source.fileHandle())
		} catch (error: Exception) {
			logFontFallback(source, "body", error)
			return fallbackFont(BODY_FONT_SIZE) to fallbackFont(DETAIL_FONT_SIZE)
		}
		var generatedBody: BitmapFont? = null
		try {
			generatedBody = generateFont(generator, BODY_FONT_SIZE, pixelScale)
			val generatedDetail = generateFont(generator, DETAIL_FONT_SIZE, pixelScale)
			return generatedBody to generatedDetail
		} catch (error: Exception) {
			generatedBody?.dispose()
			logFontFallback(source, "body", error)
			return fallbackFont(BODY_FONT_SIZE) to fallbackFont(DETAIL_FONT_SIZE)
		} finally {
			generator.dispose()
		}
	}

	private fun logFontFallback(source: SplashFont, role: String, error: Exception) {
		Gdx.app.error(
			"SplashScreen",
			"Could not load configured $role splash font '${source.path}'; using the bundled bitmap font.",
			error,
		)
	}

	private fun fallbackFont(logicalSize: Int): BitmapFont = BitmapFont().apply {
		data.setScale(logicalSize / data.lineHeight)
		setUseIntegerPositions(true)
	}

	private fun generateFont(generator: FreeTypeFontGenerator, logicalSize: Int, pixelScale: Float): BitmapFont {
		val rasterScale = pixelScale * SPLASH_FONT_OVERSAMPLE
		val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
			size = (logicalSize * rasterScale).roundToInt().coerceAtLeast(logicalSize)
			minFilter = Texture.TextureFilter.Linear
			magFilter = Texture.TextureFilter.Linear
			hinting = FreeTypeFontGenerator.Hinting.Slight
			kerning = true
		}
		return generator.generateFont(parameter).apply {
			for (i in 0 until regions.size) {
				regions[i].texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
			}
			data.setScale(1f / rasterScale)
			setUseIntegerPositions(false)
		}
	}

	private fun createText(font: BitmapFont, value: String, color: Color): SplashText {
		val cache = BitmapFontCache(font, true)
		cache.color = color
		val layout = cache.setText(value, 0f, 0f)
		return SplashText(cache, layout.width, layout.height)
	}

	private fun updateStatusText() {
		if (presentation.style == SplashStyle.QUIET) return
		val font = detailFont ?: return
		val status = presentation.statusFor(phase)
		if (displayedStatus == status) return
		displayedStatus = status
		statusText = createText(font, status, MetaColor.TEXT_MUTED)
		layoutText()
	}

	private fun updateProjection() {
		spriteBatch.projectionMatrix.setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
		// Before laying out: a quiet title is sized from the window, so a resize has to rebuild the face at the new
		// size rather than reposition the old one.
		createText()
		val bounds = SplashPanelGeometry.bounds(
			Gdx.graphics.width,
			Gdx.graphics.height,
			presentation.transition,
		)
		panelX = bounds[0]
		panelY = bounds[1]
		panelWidth = bounds[2]
		panelHeight = bounds[3]
		layoutText()
	}

	private fun layoutText() {
		if (titleFont == null) return
		if (presentation.style == SplashStyle.QUIET) {
			val title = titleText ?: return
			// Centred on the window, sitting above the bar rather than on it. The baseline is what a font cache is
			// positioned by, so the height goes in here rather than into the y fraction.
			title.cache.setPosition(
				snapToPhysicalPixel((Gdx.graphics.width - title.width) * 0.5f, textPixelScale),
				snapToPhysicalPixel(Gdx.graphics.height * QUIET_TITLE_HEIGHT_POSITION + title.height * 0.5f, textPixelScale),
			)
			return
		}
		val markX = panelX + PANEL_PADDING
		val markY = panelY + panelHeight - PANEL_PADDING - MARK_SIZE
		markText?.let {
			it.cache.setPosition(
				snapToPhysicalPixel(markX + (MARK_SIZE - it.width) * 0.5f, textPixelScale),
				snapToPhysicalPixel(markY + (MARK_SIZE + it.height) * 0.5f, textPixelScale),
			)
		}
		val headingX = markX + MARK_SIZE + HEADING_GAP
		titleText?.cache?.setPosition(
			snapToPhysicalPixel(headingX, textPixelScale),
			snapToPhysicalPixel(panelY + panelHeight - TITLE_TOP, textPixelScale),
		)
		subtitleText?.cache?.setPosition(
			snapToPhysicalPixel(headingX, textPixelScale),
			snapToPhysicalPixel(panelY + panelHeight - SUBTITLE_TOP, textPixelScale),
		)
		messageText?.cache?.setPosition(
			snapToPhysicalPixel(panelX + PANEL_PADDING, textPixelScale),
			snapToPhysicalPixel(panelY + panelHeight * MESSAGE_HEIGHT_RATIO, textPixelScale),
		)
		statusText?.cache?.setPosition(
			snapToPhysicalPixel(panelX + PANEL_PADDING, textPixelScale),
			snapToPhysicalPixel(panelY + STATUS_BOTTOM, textPixelScale),
		)
	}

	internal companion object {
		const val PREPARATION_THREAD_NAME = "meta-asset-preparation"
		const val RING_TEXTURE_SIZE = 64
		const val SPINNER_SIZE = 42f
		const val ROTATION_DEGREES_PER_SECOND = 240f
		const val MAX_DELTA = 0.1f
		const val PANEL_PADDING = 30f
		const val BORDER_WIDTH = 1f
		const val BORDER_ALPHA = 0.72f
		const val ACCENT_HEIGHT = 3f
		const val MARK_SIZE = 58f
		const val HEADING_GAP = 18f
		const val TITLE_TOP = 42f
		const val SUBTITLE_TOP = 71f
		const val MESSAGE_HEIGHT_RATIO = 0.48f
		const val DIVIDER_BOTTOM = 84f
		const val STATUS_BOTTOM = 59f
		const val PROGRESS_BOTTOM = 27f
		const val BAR_HEIGHT = 3f
		const val BAR_SEGMENT_WIDTH = 92f
		const val BAR_SPEED = 2.8f
		const val TRACK_ALPHA = 0.65f
		const val DIVIDER_ALPHA = 0.65f
		const val TITLE_FONT_SIZE = 25

		/** Quiet style: the title is this fraction of the window height, so it scales with the window. */
		const val QUIET_TITLE_HEIGHT_FRACTION = 0.075f
		const val QUIET_TITLE_MIN_SIZE = 28
		const val QUIET_TITLE_MAX_SIZE = 180
		/** Where the title's centre sits, as a fraction of the window height. Slightly above the middle. */
		const val QUIET_TITLE_HEIGHT_POSITION = 0.54f
		const val QUIET_BAR_HEIGHT_FRACTION = 0.42f
		const val QUIET_BAR_WIDTH_FRACTION = 0.24f
		const val QUIET_BAR_THICKNESS_FRACTION = 0.0022f
		const val QUIET_BAR_MIN_MARGIN = 24f
		const val QUIET_BAR_SEGMENT_FRACTION = 0.28f
		/** Deliberately slower than the panel's: a slow sweep reads as patience rather than as activity. */
		const val QUIET_BAR_SPEED = 1.0f
		const val BODY_FONT_SIZE = 15
		const val DETAIL_FONT_SIZE = 12
		const val SPLASH_FONT_OVERSAMPLE = 2f
		const val MAX_SKIPPED_LOADING_FRAMES = 4
		const val MINIMUM_PROGRESS_BUDGET_MS = 1
	}

	private class AssetQueue(val task: () -> Unit)
	private class AssetPreparation(val task: () -> Unit)
	private class SplashFonts(val title: BitmapFont, val body: BitmapFont, val detail: BitmapFont)
	private class SplashText(val cache: BitmapFontCache, val width: Float, val height: Float)
}

internal fun SplashFont.fileHandle() = Gdx.files.getFileHandle(path, fileType)

internal enum class SplashPhase {
	FADE_IN, PREPARING, QUEUEING, LOADING, UI_LOADING, APP_LOADING, HOLD, FADE_OUT, COMPLETE
}

internal fun SplashPresentation.statusFor(phase: SplashPhase): String = when (phase) {
	SplashPhase.FADE_IN -> startingStatus
	SplashPhase.PREPARING -> preparationStatus
	SplashPhase.QUEUEING -> queueStatus
	SplashPhase.LOADING -> assetStatus
	SplashPhase.UI_LOADING -> interfaceStatus
	SplashPhase.APP_LOADING -> applicationStatus
	SplashPhase.HOLD, SplashPhase.FADE_OUT, SplashPhase.COMPLETE -> readyStatus
}

internal object SplashRingTexturePainter {
	private const val INNER_RADIUS = 17f
	private const val OUTER_RADIUS = 24f
	private const val EDGE_SOFTNESS = 1.25f
	private const val BASE_ALPHA = 0.24f
	private const val HIGHLIGHT_ALPHA = 0.48f
	private const val HIGHLIGHT_ANGLE_DEGREES = 105f
	private const val HIGHLIGHT_INNER_DEGREES = 42f
	private const val HIGHLIGHT_OUTER_DEGREES = 64f

	fun alphaAt(x: Int, y: Int, size: Int): Float {
		val center = (size - 1) * 0.5f
		val dx = x - center
		val dy = center - y
		val distance = sqrt(dx * dx + dy * dy)
		val ringMask = smoothStep(INNER_RADIUS - EDGE_SOFTNESS, INNER_RADIUS + EDGE_SOFTNESS, distance) *
			(1f - smoothStep(OUTER_RADIUS - EDGE_SOFTNESS, OUTER_RADIUS + EDGE_SOFTNESS, distance))
		if (ringMask <= 0f) return 0f

		val angle = (atan2(dy, dx) * MathUtils.radiansToDegrees + 360f) % 360f
		val angleDistance = shortestAngleDistance(angle, HIGHLIGHT_ANGLE_DEGREES)
		val highlightMask = 1f - smoothStep(HIGHLIGHT_INNER_DEGREES, HIGHLIGHT_OUTER_DEGREES, angleDistance)
		return ((BASE_ALPHA + HIGHLIGHT_ALPHA * highlightMask) * ringMask).coerceIn(0f, 1f)
	}

	private fun shortestAngleDistance(a: Float, b: Float): Float =
		abs(((a - b + 540f) % 360f) - 180f)

	private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
		val progress = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
		return progress * progress * (3f - 2f * progress)
	}
}

/** How large the title is drawn. Its own object so the scaling is testable without a window. */
internal object SplashTitleSizing {
	fun forWindow(windowHeight: Int, style: SplashStyle): Int {
		// A panel's layout is built around a known size, so it does not scale.
		if (style == SplashStyle.PANEL) return SplashScreen.TITLE_FONT_SIZE
		return (windowHeight * SplashScreen.QUIET_TITLE_HEIGHT_FRACTION)
			.roundToInt()
			.coerceIn(SplashScreen.QUIET_TITLE_MIN_SIZE, SplashScreen.QUIET_TITLE_MAX_SIZE)
	}
}

/** Where the loading panel sits in the window. Its own object so the arithmetic is testable without a window. */
internal object SplashPanelGeometry {
	/** Returns x, y, width, height. */
	fun bounds(windowWidth: Int, windowHeight: Int, transition: SplashTransitionConfiguration): FloatArray {
		val availableWidth = windowWidth.toFloat().coerceAtLeast(1f)
		val availableHeight = windowHeight.toFloat().coerceAtLeast(1f)
		if (!transition.banner) return floatArrayOf(0f, 0f, availableWidth, availableHeight)

		val width = transition.bootstrapWidth.toFloat().coerceAtMost(availableWidth)
		val height = transition.bootstrapHeight.toFloat().coerceAtMost(availableHeight)
		// Rounded so the panel edge lands on a pixel: a half-pixel origin softens the border and the text baselines
		// snapped inside it.
		return floatArrayOf(
			((availableWidth - width) * 0.5f).roundToInt().toFloat(),
			((availableHeight - height) * 0.5f).roundToInt().toFloat(),
			width,
			height,
		)
	}
}

internal object SplashLoadingPolicy {
	private const val SLOW_FRAME_SECONDS = 1f / 55f
	private const val UPDATE_BUDGET_MS = 8

	fun updateBudgetMillis(frameDelta: Float): Int {
		if (!frameDelta.isFinite() || frameDelta <= 0f) return UPDATE_BUDGET_MS
		return if (frameDelta <= SLOW_FRAME_SECONDS) UPDATE_BUDGET_MS else 0
	}

	fun smoothStep(progress: Float): Float {
		val value = progress.coerceIn(0f, 1f)
		return value * value * (3f - 2f * value)
	}
}
