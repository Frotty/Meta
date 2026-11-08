package de.fatox.meta.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.GdxNativesLoader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MipDownsamplingTest {
	@Test
	fun `mip texels follow quarter turns even with odd dimensions`() {
		for ((width, height) in listOf(8 to 4, 7 to 5, 5 to 1)) {
			withPixmap(width, height) { source ->
				withPixmap(height, width) { rotated ->
					for (y in 0 until height) {
						for (x in 0 until width) {
							val color = (x * 31 shl 24) or (y * 29 shl 16) or (x * y * 7 shl 8) or 255
							source.drawPixel(x, y, color)
							rotated.drawPixel(height - 1 - y, x, color)
						}
					}
					val mip = downsampleMip(source)
					val rotatedMip = downsampleMip(rotated)
					try {
						for (y in 0 until mip.height) {
							for (x in 0 until mip.width) {
								assertEquals(mip.getPixel(x, y), rotatedMip.getPixel(mip.height - 1 - y, x))
							}
						}
					} finally {
						rotatedMip.dispose()
						mip.dispose()
					}
				}
			}
		}
	}

	@Test
	fun `clear texels preserve bleed without tinting visible color`() {
		withPixmap(4, 2) { source ->
			source.setColor(0x0000ff00)
			source.fill()
			source.drawPixel(1, 0, 0xff0000ff.toInt())
			val mip = downsampleMip(source)
			try {
				assertEquals(0xff000040.toInt(), mip.getPixel(0, 0))
				assertEquals(0x0000ff00, mip.getPixel(1, 0))
			} finally {
				mip.dispose()
			}
		}
	}

	private inline fun withPixmap(width: Int, height: Int, test: (Pixmap) -> Unit) {
		GdxNativesLoader.load()
		val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		try {
			test(pixmap)
		} finally {
			pixmap.dispose()
		}
	}
}
