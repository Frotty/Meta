package de.fatox.meta.assets

import com.badlogic.gdx.graphics.Pixmap
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.roundToInt

private const val LINEAR_COLOR_MAX = 4095
private val identityColor = IntArray(256) { it }

// Average color in linear light to keep bright texture details from darkening as the mip chain shrinks.
private val srgbToLinear = IntArray(256) { channel ->
	val srgb = channel / 255.0
	val linear = if (srgb <= 0.04045) srgb / 12.92 else ((srgb + 0.055) / 1.055).pow(2.4)
	(linear * LINEAR_COLOR_MAX).roundToInt()
}

private val linearToSrgb = IntArray(LINEAR_COLOR_MAX + 1) { channel ->
	val linear = channel.toDouble() / LINEAR_COLOR_MAX
	val srgb = if (linear <= 0.0031308) linear * 12.92 else 1.055 * linear.pow(1.0 / 2.4) - 0.055
	(srgb * 255).roundToInt()
}

internal fun downsampleMip(source: Pixmap, linearLight: Boolean): Pixmap {
	val sourceWidth = source.width
	val sourceHeight = source.height
	val width = (sourceWidth / 2).coerceAtLeast(1)
	val height = (sourceHeight / 2).coerceAtLeast(1)
	val target = Pixmap(width, height, source.format)
	val decode = if (linearLight) srgbToLinear else identityColor
	val encode = if (linearLight) linearToSrgb else identityColor
	if (source.format == Pixmap.Format.RGBA8888) {
		val input = source.pixels.duplicate().order(ByteOrder.BIG_ENDIAN)
		val output = target.pixels.duplicate().order(ByteOrder.BIG_ENDIAN)
		downsampleMip(sourceWidth, sourceHeight, width, height, decode, encode,
			read = { x, y -> input.getInt((y * sourceWidth + x) * 4) },
			write = { x, y, rgba -> output.putInt((y * width + x) * 4, rgba) },
		)
	} else {
		target.setBlending(Pixmap.Blending.None)
		downsampleMip(sourceWidth, sourceHeight, width, height, decode, encode, source::getPixel, target::drawPixel)
	}
	return target
}

private inline fun downsampleMip(
	sourceWidth: Int,
	sourceHeight: Int,
	width: Int,
	height: Int,
	decode: IntArray,
	encode: IntArray,
	read: (Int, Int) -> Int,
	write: (Int, Int, Int) -> Unit,
) {
	// Use integer coverage so opposite edges and rotated sprites get exactly the same weights.
	// Even axes reduce to a 2:1 box. Odd axes share the middle texels and include both edges.
	val xUnits = if (sourceWidth % width == 0) 1 else width
	val yUnits = if (sourceHeight % height == 0) 1 else height
	val footprintWidth = if (xUnits == 1) sourceWidth / width else sourceWidth
	val footprintHeight = if (yUnits == 1) sourceHeight / height else sourceHeight
	val area = footprintWidth.toLong() * footprintHeight
	for (y in 0 until height) {
		val top = y * footprintHeight
		val bottom = top + footprintHeight
		val firstY = if (yUnits == 1) top else top / yUnits
		val lastY = if (yUnits == 1) bottom - 1 else (bottom - 1) / yUnits
		for (x in 0 until width) {
			val left = x * footprintWidth
			val right = left + footprintWidth
			val firstX = if (xUnits == 1) left else left / xUnits
			val lastX = if (xUnits == 1) right - 1 else (right - 1) / xUnits
			var alpha = 0L
			var red = 0L
			var green = 0L
			var blue = 0L
			var bleedRed = 0L
			var bleedGreen = 0L
			var bleedBlue = 0L
			for (sourceY in firstY..lastY) {
				val overlapY = if (yUnits == 1) 1 else minOf(bottom, (sourceY + 1) * yUnits) - maxOf(top, sourceY * yUnits)
				for (sourceX in firstX..lastX) {
					val overlapX = if (xUnits == 1) 1 else minOf(right, (sourceX + 1) * xUnits) - maxOf(left, sourceX * xUnits)
					val coverage = overlapX.toLong() * overlapY
					val rgba = read(sourceX, sourceY)
					val r = decode[rgba ushr 24]
					val g = decode[rgba ushr 16 and 255]
					val b = decode[rgba ushr 8 and 255]
					val weight = (rgba and 255) * coverage
					alpha += weight
					red += r * weight
					green += g * weight
					blue += b * weight
					bleedRed += r * coverage
					bleedGreen += g * coverage
					bleedBlue += b * coverage
				}
			}
			// Transparent texels cannot tint visible ones. Preserve atlas bleed in entirely clear footprints.
			val rgba = if (alpha == 0L) {
				packMipColor(bleedRed, bleedGreen, bleedBlue, area, 0, encode)
			} else {
				packMipColor(red, green, blue, alpha, ((alpha + area / 2) / area).toInt(), encode)
			}
			write(x, y, rgba)
		}
	}
}

private fun packMipColor(red: Long, green: Long, blue: Long, weight: Long, alpha: Int, encode: IntArray): Int {
	val half = weight / 2
	val r = encode[((red + half) / weight).toInt()]
	val g = encode[((green + half) / weight).toInt()]
	val b = encode[((blue + half) / weight).toInt()]
	return (r shl 24) or (g shl 16) or (b shl 8) or alpha
}
