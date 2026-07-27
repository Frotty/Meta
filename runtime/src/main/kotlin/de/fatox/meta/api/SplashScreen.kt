package de.fatox.meta.api

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
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
) : ScreenAdapter() {
	constructor(onLoaded: () -> Unit) : this(onLoaded, null, null)
	constructor(queueAssets: () -> Unit, onLoaded: () -> Unit) : this(onLoaded, AssetQueue(queueAssets), null)
	constructor(prepareAssets: () -> Unit, queueAssets: () -> Unit, onLoaded: () -> Unit) :
		this(onLoaded, AssetQueue(queueAssets), AssetPreparation(prepareAssets))

	private val spriteBatch: SpriteBatch by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()
	private var ringTexture: Texture? = null
	private var pixelTexture: Texture? = null
	private var elapsed = 0f
	private var phaseElapsed = 0f
	private var phase = Phase.FADE_IN
	private var transitionStarted = false
	private var skippedLoadingFrames = 0
	@Volatile private var preparationComplete = false
	@Volatile private var preparationFailure: Throwable? = null

	override fun show() {
		createTextures()
		updateProjection()
		Meta.instance.windowHandler.focus()
	}

	override fun dispose() {
		ringTexture?.dispose()
		ringTexture = null
		pixelTexture?.dispose()
		pixelTexture = null
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

		drawLoadingIndicator(visualAlpha)
		advanceLoading(delta)
	}

	override fun resize(width: Int, height: Int) {
		updateProjection()
	}

	private fun drawLoadingIndicator(visualAlpha: Float) {
		val centerX = Gdx.graphics.width * 0.5f
		val centerY = Gdx.graphics.height * 0.5f + INDICATOR_Y_OFFSET
		val spinnerAngle = -elapsed * ROTATION_DEGREES_PER_SECOND

		spriteBatch.use {
			spriteBatch.color.set(MetaColor.ACCENT.r, MetaColor.ACCENT.g, MetaColor.ACCENT.b, visualAlpha)
			spriteBatch.draw(
				ringTexture!!,
				centerX - SPINNER_SIZE * 0.5f,
				centerY - SPINNER_SIZE * 0.5f,
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

			val barX = centerX - BAR_WIDTH * 0.5f
			val barY = centerY - SPINNER_SIZE * 0.5f - BAR_GAP
			spriteBatch.color.set(MetaColor.BORDER.r, MetaColor.BORDER.g, MetaColor.BORDER.b, TRACK_ALPHA * visualAlpha)
			spriteBatch.draw(pixelTexture!!, barX, barY, BAR_WIDTH, BAR_HEIGHT)
			spriteBatch.color.set(MetaColor.ACCENT.r, MetaColor.ACCENT.g, MetaColor.ACCENT.b, visualAlpha)
			if (assetQueue != null && phase == Phase.LOADING) {
				spriteBatch.draw(pixelTexture!!, barX, barY, BAR_WIDTH * assetProvider.progress.coerceIn(0f, 1f), BAR_HEIGHT)
			} else {
				val travel = BAR_WIDTH - BAR_SEGMENT_WIDTH
				val barProgress = (MathUtils.sin(elapsed * BAR_SPEED) + 1f) * 0.5f
				spriteBatch.draw(pixelTexture!!, barX + travel * barProgress, barY, BAR_SEGMENT_WIDTH, BAR_HEIGHT)
			}
			spriteBatch.color = Color.WHITE
		}
	}

	private fun advanceLoading(frameDelta: Float) {
		when (phase) {
			Phase.FADE_IN -> if (phaseElapsed >= FADE_DURATION && Meta.canChangeScreen()) {
				if (assetPreparation == null) enterPhase(Phase.QUEUEING) else startPreparation()
			}
			Phase.PREPARING -> {
				preparationFailure?.let { throw it }
				if (preparationComplete) {
					if (assetQueue == null) enterPhase(Phase.UI_LOADING) else enterPhase(Phase.LOADING)
				}
			}
			Phase.QUEUEING -> {
				assetQueue?.task?.invoke()
				if (assetQueue == null) enterPhase(Phase.UI_LOADING) else enterPhase(Phase.LOADING)
			}
			Phase.LOADING -> {
				val budgetMillis = loadingBudgetMillis(frameDelta)
				if (assetProvider.update(budgetMillis)) enterPhase(Phase.UI_LOADING)
			}
			Phase.UI_LOADING -> {
				val budgetMillis = loadingBudgetMillis(frameDelta)
				if (uiRenderer.updateLoad(budgetMillis)) enterPhase(Phase.HOLD)
			}
			Phase.HOLD -> if (phaseElapsed >= MINIMUM_HOLD_DURATION) enterPhase(Phase.FADE_OUT)
			Phase.FADE_OUT -> if (phaseElapsed >= FADE_DURATION) {
				enterPhase(Phase.COMPLETE)
				completeLoading()
			}
			Phase.COMPLETE -> Unit
		}
	}

	private fun startPreparation() {
		enterPhase(Phase.PREPARING)
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

	private fun enterPhase(next: Phase) {
		phase = next
		phaseElapsed = 0f
		skippedLoadingFrames = 0
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
			Phase.FADE_IN -> SplashLoadingPolicy.smoothStep(progress)
			Phase.FADE_OUT, Phase.COMPLETE -> 1f - SplashLoadingPolicy.smoothStep(progress)
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

	private fun updateProjection() {
		spriteBatch.projectionMatrix.setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
	}

	private companion object {
		const val PREPARATION_THREAD_NAME = "meta-asset-preparation"
		const val RING_TEXTURE_SIZE = 64
		const val SPINNER_SIZE = 54f
		const val ROTATION_DEGREES_PER_SECOND = 240f
		const val MAX_DELTA = 0.1f
		const val INDICATOR_Y_OFFSET = 14f
		const val BAR_WIDTH = 180f
		const val BAR_HEIGHT = 3f
		const val BAR_SEGMENT_WIDTH = 54f
		const val BAR_GAP = 32f
		const val BAR_SPEED = 2.8f
		const val TRACK_ALPHA = 0.5f
		const val FADE_DURATION = 0.28f
		const val MINIMUM_HOLD_DURATION = 0.12f
		const val MAX_SKIPPED_LOADING_FRAMES = 4
		const val MINIMUM_PROGRESS_BUDGET_MS = 1
	}

	private class AssetQueue(val task: () -> Unit)
	private class AssetPreparation(val task: () -> Unit)
	private enum class Phase { FADE_IN, PREPARING, QUEUEING, LOADING, UI_LOADING, HOLD, FADE_OUT, COMPLETE }
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
