package de.fatox.meta.api.model

import com.badlogic.gdx.scenes.scene2d.ui.Window
import de.fatox.meta.ui.windows.MetaDialog

/**
 * Created by Frotty on 28.06.2016.
 */
data class MetaWindowData(
	var name: String? = null,
	var x: Float = 0f,
	var y: Float = 0f,
	var width: Float = 0f,
	var height: Float = 0f,
	var displayed: Boolean = false,
	var dialog: Boolean = false,
	var viewportWidth: Float = 0f,
	var viewportHeight: Float = 0f,
	var horizontalAnchor: String = "",
	var verticalAnchor: String = "",
	var horizontalDistance: Float = 0f,
	var verticalDistance: Float = 0f,
) {

	constructor(metaWindow: Window) : this() {
		setFrom(metaWindow)
	}

	fun setFrom(
		metaWindow: Window,
		viewportWidth: Float = metaWindow.stage?.width ?: 0f,
		viewportHeight: Float = metaWindow.stage?.height ?: 0f,
	) {
		this.x = metaWindow.x
		this.y = metaWindow.y
		this.width = metaWindow.width
		this.height = metaWindow.height
		this.dialog = MetaDialog::class.java.isInstance(metaWindow)
		this.viewportWidth = viewportWidth
		this.viewportHeight = viewportHeight
		updateAnchors(metaWindow, viewportWidth, viewportHeight)
		if (!dialog) {
			displayed = true
		}
	}

	fun set(
		metaWindow: Window,
		viewportWidth: Float = metaWindow.stage?.width ?: this.viewportWidth,
		viewportHeight: Float = metaWindow.stage?.height ?: this.viewportHeight,
	) {
		if (metaWindow.isResizable) {
			metaWindow.setSize(width, height)
		}
		if (metaWindow.isMovable) {
			metaWindow.setPosition(
				resolveHorizontal(viewportWidth, metaWindow.width),
				resolveVertical(viewportHeight, metaWindow.height),
			)
		}
		metaWindow.invalidateHierarchy()
	}

	private fun updateAnchors(metaWindow: Window, viewportWidth: Float, viewportHeight: Float) {
		if (viewportWidth <= 0f || viewportHeight <= 0f || metaWindow.width <= 0f || metaWindow.height <= 0f) return

		val left = metaWindow.x
		val right = viewportWidth - metaWindow.x - metaWindow.width
		if (left <= right) {
			horizontalAnchor = ANCHOR_LEFT
			horizontalDistance = left
		} else {
			horizontalAnchor = ANCHOR_RIGHT
			horizontalDistance = right
		}

		val bottom = metaWindow.y
		val top = viewportHeight - metaWindow.y - metaWindow.height
		if (bottom <= top) {
			verticalAnchor = ANCHOR_BOTTOM
			verticalDistance = bottom
		} else {
			verticalAnchor = ANCHOR_TOP
			verticalDistance = top
		}
	}

	internal fun resolveHorizontal(viewportWidth: Float, windowWidth: Float): Float {
		val raw = if (viewportWidth > 0f && horizontalAnchor.isNotEmpty()) {
			when (horizontalAnchor) {
				ANCHOR_RIGHT -> viewportWidth - horizontalDistance - windowWidth
				else -> horizontalDistance
			}
		} else {
			x
		}
		if (viewportWidth <= 0f) return raw
		return clamp(raw, 0f, maxOf(0f, viewportWidth - windowWidth))
	}

	internal fun resolveVertical(viewportHeight: Float, windowHeight: Float): Float {
		val raw = if (viewportHeight > 0f && verticalAnchor.isNotEmpty()) {
			when (verticalAnchor) {
				ANCHOR_TOP -> viewportHeight - verticalDistance - windowHeight
				else -> verticalDistance
			}
		} else {
			y
		}
		if (viewportHeight <= 0f) return raw
		return clamp(raw, 0f, maxOf(0f, viewportHeight - windowHeight))
	}

	private fun clamp(value: Float, min: Float, max: Float): Float {
		return when {
			value < min -> min
			value > max -> max
			else -> value
		}
	}

	companion object {
		private const val ANCHOR_LEFT = "left"
		private const val ANCHOR_RIGHT = "right"
		private const val ANCHOR_BOTTOM = "bottom"
		private const val ANCHOR_TOP = "top"
	}
}
