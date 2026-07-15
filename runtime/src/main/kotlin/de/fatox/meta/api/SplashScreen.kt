package de.fatox.meta.api

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import de.fatox.meta.Meta
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaColor

class SplashScreen(private val cb: () -> Unit) : ScreenAdapter() {
	private val uiRenderer: UIRenderer by lazyInject()
	private val shapeRenderer = ShapeRenderer()
	private var spinnerAngle = 0f
	private var transitionStarted = false

	override fun show() {
		updateProjection()
	}

	override fun dispose() {
		shapeRenderer.dispose()
	}

	override fun render(delta: Float) {
		spinnerAngle = (spinnerAngle - ROTATION_DEGREES_PER_SECOND * delta.coerceAtMost(MAX_DELTA)) % 360f

		Gdx.gl.apply {
			// Window dimensions are logical points on Retina/HiDPI displays; HdpiUtils maps them to framebuffer pixels.
			HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
			glClearColor(MetaColor.BACKGROUND.r, MetaColor.BACKGROUND.g, MetaColor.BACKGROUND.b, 1f)
			glClear(GL20.GL_COLOR_BUFFER_BIT)
			glEnable(GL20.GL_BLEND)
			glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
		}

		val centerX = Gdx.graphics.width * 0.5f
		val centerY = Gdx.graphics.height * 0.5f
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
		for (i in 0 until SEGMENT_COUNT) {
			val progress = i.toFloat() / (SEGMENT_COUNT - 1)
			val segmentAngle = spinnerAngle + i * SEGMENT_ANGLE
			shapeRenderer.color.set(
				MetaColor.ACCENT.r,
				MetaColor.ACCENT.g,
				MetaColor.ACCENT.b,
				MIN_ALPHA + progress * (1f - MIN_ALPHA),
			)
			shapeRenderer.circle(
				centerX + MathUtils.cosDeg(segmentAngle) * SPINNER_RADIUS,
				centerY + MathUtils.sinDeg(segmentAngle) * SPINNER_RADIUS,
				SEGMENT_RADIUS,
				SEGMENT_VERTICES,
			)
		}
		shapeRenderer.end()
		Gdx.gl.glDisable(GL20.GL_BLEND)

		if (!transitionStarted && Meta.canChangeScreen()) {
			transitionStarted = true
			cb.invoke()
			uiRenderer.load()
		}
	}

	override fun resize(width: Int, height: Int) {
		updateProjection()
	}

	private fun updateProjection() {
		shapeRenderer.projectionMatrix.setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
	}

	private companion object {
		const val SEGMENT_COUNT = 12
		const val SEGMENT_ANGLE = 360f / SEGMENT_COUNT
		const val SEGMENT_VERTICES = 12
		const val SPINNER_RADIUS = 18f
		const val SEGMENT_RADIUS = 3f
		const val ROTATION_DEGREES_PER_SECOND = 300f
		const val MIN_ALPHA = 0.12f
		const val MAX_DELTA = 0.1f
	}
}
