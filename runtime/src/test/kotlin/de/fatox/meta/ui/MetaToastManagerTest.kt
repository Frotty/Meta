package de.fatox.meta.ui

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.viewport.Viewport
import de.fatox.meta.test.GdxTestEnvironment
import java.lang.reflect.Proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MetaToastManagerTest {
	@BeforeTest
	fun setUp() = GdxTestEnvironment.ensure()

	@Test
	fun `toast layer anchors children at top center`() {
		val stage = Stage(TestViewport(), noopBatch()).apply {
			viewport.update(800, 600, true)
		}
		val manager = MetaToastManager(stage)
		val toast = FixedWidget(120f, 24f)

		manager.rootForLayoutTest.add(toast).center().row()
		manager.rootForLayoutTest.validate()

		assertEquals(800f, manager.rootForLayoutTest.width)
		assertEquals(600f, manager.rootForLayoutTest.height)
		assertEquals(340f, toast.x)
		assertTrue(toast.y >= 600f - MetaSpacing.LG - toast.prefHeight - 1f, "toast y=${toast.y}")
	}

	private class FixedWidget(
		private val prefWidth: Float,
		private val prefHeight: Float,
	) : Widget() {
		override fun getPrefWidth(): Float = prefWidth
		override fun getPrefHeight(): Float = prefHeight
	}

	private fun noopBatch(): Batch {
		val projection = Matrix4()
		val transform = Matrix4()
		return Proxy.newProxyInstance(
			Batch::class.java.classLoader,
			arrayOf(Batch::class.java),
		) { _, method, _ ->
			when (method.name) {
				"getProjectionMatrix" -> projection
				"getTransformMatrix" -> transform
				"isBlendingEnabled" -> true
				"isDrawing" -> false
				"getBlendSrcFunc", "getBlendDstFunc", "getBlendSrcFuncAlpha", "getBlendDstFuncAlpha" -> 0
				else -> null
			}
		} as Batch
	}

	private class TestViewport : Viewport() {
		init {
			camera = OrthographicCamera()
			setWorldSize(800f, 600f)
		}

		override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
			setScreenBounds(0, 0, screenWidth, screenHeight)
			if (centerCamera) camera.position.set(worldWidth * 0.5f, worldHeight * 0.5f, 0f)
			camera.update()
		}
	}
}
