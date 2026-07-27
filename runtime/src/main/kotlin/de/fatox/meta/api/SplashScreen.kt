package de.fatox.meta.api

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.math.MathUtils
import de.fatox.meta.Meta
import de.fatox.meta.api.extensions.use
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaColor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Static, allocation-free copy for the startup panel. The splash deliberately uses libGDX's bundled bitmap font so
 * none of the application's fonts, skin, scene2d widgets, or atlases need to exist before it can explain its work.
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
	val readyStatus: String = "READY",
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
 */
class SplashScreen private constructor(
	private val onLoaded: () -> Unit,
	private val assetQueue: AssetQueue?,
	private val assetPreparation: AssetPreparation?,
	private val presentation: SplashPresentation,
) : ScreenAdapter() {
	constructor(onLoaded: () -> Unit) : this(onLoaded, null, null, SplashPresentation())
	constructor(presentation: SplashPresentation, onLoaded: () -> Unit) : this(onLoaded, null, null, presentation)
	constructor(queueAssets: () -> Unit, onLoaded: () -> Unit) :
		this(onLoaded, AssetQueue(queueAssets), null, SplashPresentation())
	constructor(presentation: SplashPresentation, queueAssets: () -> Unit, onLoaded: () -> Unit) :
		this(onLoaded, AssetQueue(queueAssets), null, presentation)
	constructor(prepareAssets: () -> Unit, queueAssets: () -> Unit, onLoaded: () -> Unit) :
		this(onLoaded, AssetQueue(queueAssets), AssetPreparation(prepareAssets), SplashPresentation())
	constructor(
		presentation: SplashPresentation,
		prepareAssets: () -> Unit,
		queueAssets: () -> Unit,
		onLoaded: () -> Unit,
	) : this(onLoaded, AssetQueue(queueAssets), AssetPreparation(prepareAssets), presentation)

	private val spriteBatch: SpriteBatch by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()
	private var ringTexture: Texture? = null
	private var pixelTexture: Texture? = null
	private var bitmapFont: BitmapFont? = null
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
	private var transitionStarted = false
	private var skippedLoadingFrames = 0
	@Volatile private var preparationComplete = false
	@Volatile private var preparationFailure: Throwable? = null

	override fun show() {
		createTextures()
		createText()
		updateProjection()
		Meta.instance.windowHandler.focus()
	}

	override fun dispose() {
		ringTexture?.dispose()
		ringTexture = null
		pixelTexture?.dispose()
		pixelTexture = null
		bitmapFont?.dispose()
		bitmapFont = null
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
			glClearColor(
				MetaColor.BACKGROUND.r * visualAlpha,
				MetaColor.BACKGROUND.g * visualAlpha,
				MetaColor.BACKGROUND.b * visualAlpha,
				1f,
			)
			glClear(GL20.GL_COLOR_BUFFER_BIT)
		}

		drawLoadingPanel(visualAlpha)
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
			spriteBatch.color.set(0f, 0f, 0f, SHADOW_ALPHA * visualAlpha)
			spriteBatch.draw(
				pixelTexture!!,
				panelX + SHADOW_OFFSET,
				panelY - SHADOW_OFFSET,
				panelWidth,
				panelHeight,
			)
			spriteBatch.color.set(
				MetaColor.SURFACE.r,
				MetaColor.SURFACE.g,
				MetaColor.SURFACE.b,
				visualAlpha,
			)
			spriteBatch.draw(pixelTexture!!, panelX, panelY, panelWidth, panelHeight)
			drawPanelBorder(visualAlpha)
			spriteBatch.color.set(MetaColor.ACCENT.r, MetaColor.ACCENT.g, MetaColor.ACCENT.b, visualAlpha)
			spriteBatch.draw(pixelTexture!!, panelX, panelY + panelHeight - ACCENT_HEIGHT, panelWidth, ACCENT_HEIGHT)

			val markX = panelX + PANEL_PADDING
			val markY = panelY + panelHeight - PANEL_PADDING - MARK_SIZE
			spriteBatch.color.set(MetaColor.PRIMARY.r, MetaColor.PRIMARY.g, MetaColor.PRIMARY.b, visualAlpha)
			spriteBatch.draw(pixelTexture!!, markX, markY, MARK_SIZE, MARK_SIZE)

			spriteBatch.color.set(
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

			spriteBatch.color.set(MetaColor.ACCENT.r, MetaColor.ACCENT.g, MetaColor.ACCENT.b, visualAlpha)
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

			spriteBatch.color.set(MetaColor.BORDER.r, MetaColor.BORDER.g, MetaColor.BORDER.b, TRACK_ALPHA * visualAlpha)
			spriteBatch.draw(pixelTexture!!, barX, barY, barWidth, BAR_HEIGHT)
			spriteBatch.color.set(MetaColor.ACCENT.r, MetaColor.ACCENT.g, MetaColor.ACCENT.b, visualAlpha)
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

	private fun drawPanelBorder(visualAlpha: Float) {
		spriteBatch.color.set(
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
		when (phase) {
			SplashPhase.FADE_IN -> if (phaseElapsed >= FADE_DURATION && Meta.canChangeScreen()) {
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
				if (uiRenderer.updateLoad(budgetMillis)) enterPhase(SplashPhase.HOLD)
			}
			SplashPhase.HOLD -> if (phaseElapsed >= MINIMUM_HOLD_DURATION) enterPhase(SplashPhase.FADE_OUT)
			SplashPhase.FADE_OUT -> if (phaseElapsed >= FADE_DURATION) {
				enterPhase(SplashPhase.COMPLETE)
				completeLoading()
			}
			SplashPhase.COMPLETE -> Unit
		}
	}

	private fun startPreparation() {
		enterPhase(SplashPhase.PREPARING)
		val preparation = assetPreparation ?: return
		Thread({
			try {
				preparation.task.invoke()
				assetQueue?.task?.invoke()
			} catch (failure: Throwable) {
				preparationFailure = failure
			} finally {
				preparationComplete = true
			}
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
		val progress = (phaseElapsed / FADE_DURATION).coerceIn(0f, 1f)
		return when (phase) {
			SplashPhase.FADE_IN -> SplashLoadingPolicy.smoothStep(progress)
			SplashPhase.FADE_OUT, SplashPhase.COMPLETE -> 1f - SplashLoadingPolicy.smoothStep(progress)
			else -> 1f
		}
	}

	private fun completeLoading() {
		if (transitionStarted) return
		transitionStarted = true
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
		if (bitmapFont != null) return
		val font = BitmapFont().apply {
			region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
			setUseIntegerPositions(true)
		}
		bitmapFont = font
		markText = createText(font, presentation.mark, MARK_TEXT_SCALE, MetaColor.TEXT)
		titleText = createText(font, presentation.title, TITLE_TEXT_SCALE, MetaColor.TEXT)
		subtitleText = createText(font, presentation.subtitle, NORMAL_TEXT_SCALE, MetaColor.TEXT_MUTED)
		messageText = createText(font, presentation.message, NORMAL_TEXT_SCALE, MetaColor.TEXT)
		updateStatusText()
	}

	private fun createText(font: BitmapFont, value: String, scale: Float, color: Color): SplashText {
		font.data.setScale(scale)
		val cache = BitmapFontCache(font, true)
		cache.color = color
		val layout = cache.setText(value, 0f, 0f)
		return SplashText(cache, layout.width, layout.height)
	}

	private fun updateStatusText() {
		val font = bitmapFont ?: return
		val status = presentation.statusFor(phase)
		if (displayedStatus == status) return
		displayedStatus = status
		statusText = createText(font, status, NORMAL_TEXT_SCALE, MetaColor.TEXT_MUTED)
		layoutText()
	}

	private fun updateProjection() {
		spriteBatch.projectionMatrix.setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
		val availableWidth = (Gdx.graphics.width - VIEWPORT_MARGIN * 2f).coerceAtLeast(1f)
		val availableHeight = (Gdx.graphics.height - VIEWPORT_MARGIN * 2f).coerceAtLeast(1f)
		panelWidth = minOf(PANEL_WIDTH, availableWidth)
		panelHeight = minOf(PANEL_HEIGHT, availableHeight)
		panelX = (Gdx.graphics.width - panelWidth) * 0.5f
		panelY = (Gdx.graphics.height - panelHeight) * 0.5f
		layoutText()
	}

	private fun layoutText() {
		if (bitmapFont == null) return
		val markX = panelX + PANEL_PADDING
		val markY = panelY + panelHeight - PANEL_PADDING - MARK_SIZE
		markText?.let {
			it.cache.setPosition(
				markX + (MARK_SIZE - it.width) * 0.5f,
				markY + (MARK_SIZE + it.height) * 0.5f,
			)
		}
		val headingX = markX + MARK_SIZE + HEADING_GAP
		titleText?.cache?.setPosition(headingX, panelY + panelHeight - TITLE_TOP)
		subtitleText?.cache?.setPosition(headingX, panelY + panelHeight - SUBTITLE_TOP)
		messageText?.cache?.setPosition(panelX + PANEL_PADDING, panelY + MESSAGE_BOTTOM)
		statusText?.cache?.setPosition(panelX + PANEL_PADDING, panelY + STATUS_BOTTOM)
	}

	private companion object {
		const val PREPARATION_THREAD_NAME = "meta-asset-preparation"
		const val RING_TEXTURE_SIZE = 64
		const val SPINNER_SIZE = 42f
		const val ROTATION_DEGREES_PER_SECOND = 240f
		const val MAX_DELTA = 0.1f
		const val VIEWPORT_MARGIN = 24f
		const val PANEL_WIDTH = 520f
		const val PANEL_HEIGHT = 270f
		const val PANEL_PADDING = 30f
		const val SHADOW_OFFSET = 10f
		const val SHADOW_ALPHA = 0.35f
		const val BORDER_WIDTH = 1f
		const val BORDER_ALPHA = 0.72f
		const val ACCENT_HEIGHT = 3f
		const val MARK_SIZE = 58f
		const val HEADING_GAP = 18f
		const val TITLE_TOP = 42f
		const val SUBTITLE_TOP = 71f
		const val MESSAGE_BOTTOM = 113f
		const val DIVIDER_BOTTOM = 84f
		const val STATUS_BOTTOM = 59f
		const val PROGRESS_BOTTOM = 27f
		const val BAR_HEIGHT = 3f
		const val BAR_SEGMENT_WIDTH = 92f
		const val BAR_SPEED = 2.8f
		const val TRACK_ALPHA = 0.65f
		const val DIVIDER_ALPHA = 0.65f
		const val MARK_TEXT_SCALE = 1.15f
		const val TITLE_TEXT_SCALE = 1.55f
		const val NORMAL_TEXT_SCALE = 0.85f
		const val FADE_DURATION = 0.28f
		const val MINIMUM_HOLD_DURATION = 0.12f
		const val MAX_SKIPPED_LOADING_FRAMES = 4
		const val MINIMUM_PROGRESS_BUDGET_MS = 1
	}

	private class AssetQueue(val task: () -> Unit)
	private class AssetPreparation(val task: () -> Unit)
	private class SplashText(val cache: BitmapFontCache, val width: Float, val height: Float)
}

internal enum class SplashPhase { FADE_IN, PREPARING, QUEUEING, LOADING, UI_LOADING, HOLD, FADE_OUT, COMPLETE }

internal fun SplashPresentation.statusFor(phase: SplashPhase): String = when (phase) {
	SplashPhase.FADE_IN -> startingStatus
	SplashPhase.PREPARING -> preparationStatus
	SplashPhase.QUEUEING -> queueStatus
	SplashPhase.LOADING -> assetStatus
	SplashPhase.UI_LOADING -> interfaceStatus
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

internal object SplashLoadingPolicy {
	private const val SLOW_FRAME_SECONDS = 1f / 55f
	private const val UPDATE_BUDGET_MS = 1

	fun updateBudgetMillis(frameDelta: Float): Int {
		if (!frameDelta.isFinite() || frameDelta <= 0f) return UPDATE_BUDGET_MS
		return if (frameDelta <= SLOW_FRAME_SECONDS) UPDATE_BUDGET_MS else 0
	}

	fun smoothStep(progress: Float): Float {
		val value = progress.coerceIn(0f, 1f)
		return value * value * (3f - 2f * value)
	}
}
