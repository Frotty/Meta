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
 * that queue in bounded per-frame slices and calls [onLoaded] on the GL thread when loading completes.
 */
class SplashScreen private constructor(
	private val onLoaded: () -> Unit,
	private val assetQueue: AssetQueue?,
) : ScreenAdapter() {
	constructor(onLoaded: () -> Unit) : this(onLoaded, null)
	constructor(queueAssets: () -> Unit, onLoaded: () -> Unit) : this(onLoaded, AssetQueue(queueAssets))

	private val spriteBatch: SpriteBatch by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()
	private var dotTexture: Texture? = null
	private var pixelTexture: Texture? = null
	private var elapsed = 0f
	private var loadingStarted = false
	private var transitionStarted = false

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
		elapsed += delta.coerceAtMost(MAX_DELTA)

		Gdx.gl.apply {
			// Window dimensions are logical points on Retina/HiDPI displays; HdpiUtils maps them to framebuffer pixels.
			HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
			glClearColor(MetaColor.BACKGROUND.r, MetaColor.BACKGROUND.g, MetaColor.BACKGROUND.b, 1f)
			glClear(GL20.GL_COLOR_BUFFER_BIT)
		}

		drawLoadingIndicator()

		if (!loadingStarted && Meta.canChangeScreen()) {
			loadingStarted = true
			val queue = assetQueue
			if (queue == null) {
				completeLoading()
			} else {
				queue.task.invoke()
			}
		}

		if (loadingStarted && !transitionStarted && assetQueue != null && assetProvider.update(ASSET_LOAD_BUDGET_MS)) {
			completeLoading()
		}
	}

	override fun resize(width: Int, height: Int) {
		updateProjection()
	}

	private fun drawLoadingIndicator() {
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
					MIN_ALPHA + progress * (1f - MIN_ALPHA),
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
			spriteBatch.color.set(MetaColor.BORDER.r, MetaColor.BORDER.g, MetaColor.BORDER.b, TRACK_ALPHA)
			spriteBatch.draw(pixelTexture!!, barX, barY, BAR_WIDTH, BAR_HEIGHT)
			spriteBatch.color.set(MetaColor.ACCENT)
			if (assetQueue != null && loadingStarted) {
				spriteBatch.draw(pixelTexture!!, barX, barY, BAR_WIDTH * assetProvider.progress.coerceIn(0f, 1f), BAR_HEIGHT)
			} else {
				val travel = BAR_WIDTH - BAR_SEGMENT_WIDTH
				val barProgress = (MathUtils.sin(elapsed * BAR_SPEED) + 1f) * 0.5f
				spriteBatch.draw(pixelTexture!!, barX + travel * barProgress, barY, BAR_SEGMENT_WIDTH, BAR_HEIGHT)
			}
			spriteBatch.color = Color.WHITE
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
		const val ASSET_LOAD_BUDGET_MS = 12
	}

	private class AssetQueue(val task: () -> Unit)
}
