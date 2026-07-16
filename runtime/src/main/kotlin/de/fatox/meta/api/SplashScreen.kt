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
	private var dotTexture: Texture? = null
	private var pixelTexture: Texture? = null
	private var elapsed = 0f
	private var phaseElapsed = 0f
	private var phase = Phase.FADE_IN
	private var transitionStarted = false
	@Volatile private var preparationComplete = false
	@Volatile private var preparationFailure: Throwable? = null

	override fun show() {
		createTextures()
		updateProjection()
		Meta.instance.windowHandler.focus()
	}

	override fun dispose() {
		dotTexture?.dispose()
		dotTexture = null
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
			for (i in 0 until SEGMENT_COUNT) {
				val progress = i.toFloat() / (SEGMENT_COUNT - 1)
				val segmentAngle = spinnerAngle + i * SEGMENT_ANGLE
				val segmentSize = MIN_DOT_SIZE + progress * (MAX_DOT_SIZE - MIN_DOT_SIZE)
				spriteBatch.color.set(
					MetaColor.ACCENT.r,
					MetaColor.ACCENT.g,
					MetaColor.ACCENT.b,
					(MIN_ALPHA + progress * (1f - MIN_ALPHA)) * visualAlpha,
				)
				spriteBatch.draw(
					dotTexture!!,
					centerX + MathUtils.cosDeg(segmentAngle) * SPINNER_RADIUS - segmentSize * 0.5f,
					centerY + MathUtils.sinDeg(segmentAngle) * SPINNER_RADIUS - segmentSize * 0.5f,
					segmentSize,
					segmentSize,
				)
			}

			val barX = centerX - BAR_WIDTH * 0.5f
			val barY = centerY - SPINNER_RADIUS - BAR_GAP
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
					if (assetQueue == null) enterPhase(Phase.HOLD) else enterPhase(Phase.LOADING)
				}
			}
			Phase.QUEUEING -> {
				assetQueue?.task?.invoke()
				if (assetQueue == null) enterPhase(Phase.HOLD) else enterPhase(Phase.LOADING)
			}
			Phase.LOADING -> {
				val budgetMillis = SplashLoadingPolicy.updateBudgetMillis(frameDelta)
				if (assetProvider.update(budgetMillis)) enterPhase(Phase.HOLD)
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
		uiRenderer.load()
	}

	private fun createTextures() {
		if (dotTexture == null) {
			val dotPixmap = Pixmap(DOT_TEXTURE_SIZE, DOT_TEXTURE_SIZE, Pixmap.Format.RGBA8888)
			dotPixmap.setColor(Color.WHITE)
			dotPixmap.fillCircle(DOT_TEXTURE_SIZE / 2, DOT_TEXTURE_SIZE / 2, DOT_TEXTURE_SIZE / 2 - 1)
			dotTexture = Texture(dotPixmap)
			dotPixmap.dispose()
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
		const val DOT_TEXTURE_SIZE = 16
		const val SEGMENT_COUNT = 12
		const val SEGMENT_ANGLE = 360f / SEGMENT_COUNT
		const val SPINNER_RADIUS = 25f
		const val MIN_DOT_SIZE = 4f
		const val MAX_DOT_SIZE = 8f
		const val ROTATION_DEGREES_PER_SECOND = 240f
		const val MIN_ALPHA = 0.12f
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
	}

	private class AssetQueue(val task: () -> Unit)
	private class AssetPreparation(val task: () -> Unit)
	private enum class Phase { FADE_IN, PREPARING, QUEUEING, LOADING, HOLD, FADE_OUT, COMPLETE }
}

internal object SplashLoadingPolicy {
	private const val TARGET_FRAME_SECONDS = 1f / 60f
	private const val MAX_UPDATE_BUDGET_MS = 6

	fun updateBudgetMillis(frameDelta: Float): Int {
		if (!frameDelta.isFinite() || frameDelta <= 0f) return MAX_UPDATE_BUDGET_MS
		val spareMillis = ((TARGET_FRAME_SECONDS - frameDelta) * 1000f).toInt()
		return spareMillis.coerceIn(0, MAX_UPDATE_BUDGET_MS)
	}

	fun smoothStep(progress: Float): Float {
		val value = progress.coerceIn(0f, 1f)
		return value * value * (3f - 2f * value)
	}
}
