package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.ScreenUtils
import kotlin.math.roundToInt

/** Physical back-buffer rectangle corresponding to a logical screen-space actor rectangle. */
data class MetaFrameCaptureBounds(val x: Int, val y: Int, val width: Int, val height: Int)

fun metaFrameCaptureBounds(
	bottomLeftScreen: Vector2,
	topRightScreen: Vector2,
	logicalWidth: Int,
	logicalHeight: Int,
	backBufferWidth: Int,
	backBufferHeight: Int,
): MetaFrameCaptureBounds {
	require(logicalWidth > 0 && logicalHeight > 0) { "Logical framebuffer dimensions must be positive" }
	require(backBufferWidth > 0 && backBufferHeight > 0) { "Physical framebuffer dimensions must be positive" }
	val scaleX = backBufferWidth.toFloat() / logicalWidth
	val scaleY = backBufferHeight.toFloat() / logicalHeight
	val left = minOf(bottomLeftScreen.x, topRightScreen.x)
	val right = maxOf(bottomLeftScreen.x, topRightScreen.x)
	val screenTop = minOf(bottomLeftScreen.y, topRightScreen.y)
	val screenBottom = maxOf(bottomLeftScreen.y, topRightScreen.y)
	val x = (left * scaleX).roundToInt().coerceIn(0, backBufferWidth)
	val y = ((logicalHeight - screenBottom) * scaleY).roundToInt().coerceIn(0, backBufferHeight)
	val rightPixel = (right * scaleX).roundToInt().coerceIn(0, backBufferWidth)
	val topPixel = ((logicalHeight - screenTop) * scaleY).roundToInt().coerceIn(0, backBufferHeight)
	require(rightPixel > x && topPixel > y) { "Capture region does not intersect the framebuffer" }
	return MetaFrameCaptureBounds(x, y, rightPixel - x, topPixel - y)
}

/**
 * Copies the framebuffer region currently behind [actor] into this pixmap, scaling it to the target pixmap size.
 * [bottomLeft] and [topRight] are caller-owned scratch vectors so repeated viewfinder captures do not allocate them.
 */
fun Pixmap.captureFramebufferBehind(actor: Actor, bottomLeft: Vector2, topRight: Vector2) {
	actor.localToScreenCoordinates(bottomLeft.set(0f, 0f))
	actor.localToScreenCoordinates(topRight.set(actor.width, actor.height))
	val graphics = Gdx.graphics
	val bounds = metaFrameCaptureBounds(
		bottomLeft,
		topRight,
		graphics.width,
		graphics.height,
		graphics.backBufferWidth,
		graphics.backBufferHeight,
	)
	val source = Pixmap(bounds.width, bounds.height, Pixmap.Format.RGBA8888)
	try {
		val captured = ScreenUtils.getFrameBufferPixels(bounds.x, bounds.y, bounds.width, bounds.height, true)
		source.pixels.apply {
			position(0)
			limit(capacity())
		}
		BufferUtils.copy(captured, 0, source.pixels, captured.size)
		setFilter(Pixmap.Filter.BiLinear)
		drawPixmap(source, 0, 0, source.width, source.height, 0, 0, width, height)
	} finally {
		source.dispose()
	}
}
