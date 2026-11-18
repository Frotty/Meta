package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.test.GdxTestEnvironment
import de.fatox.meta.ui.MetaSkin
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MetaLoadingSkeletonTest {
	@Test
	fun `placeholder keeps its geometry and batch tint while pulsing`() {
		GdxTestEnvironment.ensure()
		MetaSkin.dispose()
		var draws = 0
		MetaSkin.initialize(Skin().apply {
			add(MetaSkin.COLOR_FILL, object : BaseDrawable() {
				override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
					assertEquals(160f, width)
					assertEquals(20f, height)
					draws++
				}
			}, Drawable::class.java)
		}, installDefaults = false)
		try {
			var alpha = 0f
			var packedColor = 5f
			val batch = Proxy.newProxyInstance(Batch::class.java.classLoader, arrayOf(Batch::class.java)) { _, method, args ->
				when (method.name) {
					"getPackedColor" -> packedColor
					"setPackedColor" -> { packedColor = args[0] as Float; null }
					"setColor" -> { alpha = args[3] as Float; packedColor = 0f; null }
					else -> error("Unexpected batch operation ${method.name}")
				}
			} as Batch
			val skeleton = MetaLoadingSkeleton(160f, 20f, periodSeconds = 1f)
			assertEquals(Touchable.disabled, skeleton.touchable)
			assertEquals(160f, skeleton.prefWidth)
			assertEquals(20f, skeleton.prefHeight)
			skeleton.draw(batch, 0.5f)
			val initialAlpha = alpha
			assertEquals(5f, packedColor)

			skeleton.act(0.5f)
			skeleton.draw(batch, 0.5f)
			assertTrue(alpha > initialAlpha)
			assertEquals(5f, packedColor)

			skeleton.animated = false
			skeleton.draw(batch, 0.5f)
			val staticAlpha = alpha
			skeleton.act(0.5f)
			skeleton.draw(batch, 0.5f)
			assertEquals(staticAlpha, alpha)
			assertEquals(4, draws)
		} finally {
			MetaSkin.dispose()
		}
	}
}
