package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Touchable
import de.fatox.meta.test.MetaHeadlessUi
import de.fatox.meta.ui.MetaSkin
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MetaLoadingSkeletonTest {
	@Test
	fun `gradient clips at both edges and disabling animation preserves the base`() {
		MetaHeadlessUi.install()
		try {
			var alpha = 0f
			var packedColor = 5f
			var baseDraws = 0
			val bands = mutableListOf<Band>()
			val batch = Proxy.newProxyInstance(Batch::class.java.classLoader, arrayOf(Batch::class.java)) { _, method, args ->
				when (method.name) {
					"getPackedColor" -> packedColor
					"setPackedColor" -> { packedColor = args[0] as Float; null }
					"setColor" -> { alpha = args[3] as Float; packedColor = 0f; null }
					"draw" -> {
						if (args.size == 9) {
							assertEquals(0.19f, alpha)
							bands.add(Band(args[1] as Float, args[3] as Float, args[5] as Float, args[7] as Float))
						} else {
							assertEquals(0.09f, alpha)
							assertEquals(160f, args[3])
							assertEquals(20f, args[4])
							baseDraws++
						}
						null
					}
					else -> error("Unexpected batch operation ${method.name}")
				}
			} as Batch
			val skeleton = MetaLoadingSkeleton(160f, 20f, periodSeconds = 1f).apply { setPosition(8f, 12f) }
			assertEquals(Touchable.disabled, skeleton.touchable)
			assertEquals(160f, skeleton.prefWidth)
			assertEquals(20f, skeleton.prefHeight)
			skeleton.draw(batch, 0.5f)
			assertTrue(bands.isEmpty())

			repeat(3) {
				skeleton.act(0.25f)
				skeleton.draw(batch, 0.5f)
				assertEquals(5f, packedColor)
			}
			val (entering, middle, leaving) = bands
			assertEquals(8f, entering.x)
			assertEquals(168f, leaving.x + leaving.width, 0.001f)
			assertTrue(entering.width < middle.width && leaving.width < middle.width)
			assertTrue(entering.startU > middle.startU && leaving.endU < middle.endU)

			skeleton.animated = false
			skeleton.draw(batch, 0.5f)
			assertEquals(3, bands.size)
			assertEquals(5, baseDraws)
		} finally {
			MetaHeadlessUi.dispose()
		}
	}

	@Test
	fun `gradient has transparent edges and a bright smooth center in the atlas`() {
		MetaHeadlessUi.install()
		try {
			val region = checkNotNull(MetaSkin.sharedPacker().getRect(MetaSkin.LOADING_SKELETON_HIGHLIGHT))
			fun opacity(x: Int) = region.page.pixmap.getPixel(region.bounds.x + x, region.bounds.y) and 0xff
			assertEquals(0, opacity(0))
			assertTrue(opacity(16) in 110..145)
			assertEquals(255, opacity(31))
			assertEquals(0, opacity(63))
		} finally {
			MetaHeadlessUi.dispose()
		}
	}

	private data class Band(val x: Float, val width: Float, val startU: Float, val endU: Float)
}
