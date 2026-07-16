package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing

private val scrollLog = MetaLoggerFactory.logger {}

/**
 * A [ScrollPane] with a faster mouse-wheel step, Meta's thin generated scrollbar style and a small content gutter
 * before the scrollbar. Use this instead of raw `ScrollPane` so scrollable content never sits flush
 * against the thumb.
 * Uses the borderless `meta.scrollPane.flat` style by default; use `MetaSkin.SCROLL_PANE` explicitly when you need
 * a bordered container look.
 */
class MetaScrollPane : ScrollPane {
	constructor(widget: Actor?, style: ScrollPaneStyle) : super(wrapContent(widget), style) {
		configure()
	}

	constructor(widget: Actor?, styleName: String?) : super(wrapContent(widget), MetaSkin.skin(), styleName) {
		configure()
	}

	constructor(widget: Actor?) : super(wrapContent(widget), MetaSkin.skin(), defaultStyleName()) {
		configure()
	}

	override fun getMouseWheelY(): Float = MOUSE_WHEEL_STEP

	private fun configure() {
		setScrollbarsOnTop(false)
		setFadeScrollBars(false)
		setOverscroll(false, false)
		setFlickScroll(false)
		addCaptureListener(object : InputListener() {
			override fun scrolled(
				event: InputEvent,
				x: Float,
				y: Float,
				amountX: Float,
				amountY: Float,
			): Boolean {
				scrollLog.debug {
					"scroll-pane-wheel pane=${this@MetaScrollPane.scrollDebugState()} target=${event.target.scrollDebugPath()} " +
						"dx=$amountX dy=$amountY shift=${shiftPressed()} action=${if (shiftPressed()) "horizontal" else "delegate"}"
				}
				if (amountY == 0f ||
					!shiftPressed()
				) return false
				val hoveredPane = event.stage.hit(event.stageX, event.stageY, true)
					.nearestHorizontallyScrollableMetaScrollPane()
				if (hoveredPane !== this@MetaScrollPane) return false
				setScrollX(shiftedHorizontalScrollPosition(scrollX, maxX, amountY, MOUSE_WHEEL_STEP))
				// Capture listeners run from outer to inner. Stop here so neither an outer pane nor ScrollPane's normal
				// vertical wheel listener also consumes this browser-style horizontal gesture.
				event.stop()
				return true
			}
		})
	}

	private fun shiftPressed(): Boolean =
		Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)

	private fun Actor?.nearestHorizontallyScrollableMetaScrollPane(): MetaScrollPane? {
		var current = this
		while (current != null) {
			if (current is MetaScrollPane) {
				current.validate()
				if (!current.isScrollingDisabledX && current.maxX > 0f) return current
			}
			current = current.parent
		}
		return null
	}

	private companion object {
		const val MOUSE_WHEEL_STEP = 100f
		const val SCROLLBAR_GUTTER = MetaSpacing.SM

		fun defaultStyleName(): String =
			if (MetaSkin.skin().has(MetaSkin.SCROLL_PANE_FLAT, ScrollPaneStyle::class.java)) {
				MetaSkin.SCROLL_PANE_FLAT
			} else if (MetaSkin.skin().has(MetaSkin.SCROLL_PANE, ScrollPaneStyle::class.java)) {
				MetaSkin.SCROLL_PANE
			} else {
				"list"
			}

		fun wrapContent(widget: Actor?): Actor? {
			if (widget == null) return null
			return Table(MetaSkin.skin()).apply {
				add(widget).grow().padRight(SCROLLBAR_GUTTER)
			}
		}
	}
}

internal fun Actor?.nearestMetaScrollPane(): MetaScrollPane? {
	var current = this
	while (current != null) {
		if (current is MetaScrollPane) return current
		current = current.parent
	}
	return null
}

internal fun Actor?.nearestScrollableMetaScrollPane(): MetaScrollPane? {
	var current = this
	while (current != null) {
		if (current is MetaScrollPane) {
			current.validate()
			val canScrollX = !current.isScrollingDisabledX && current.maxX > 0f
			val canScrollY = !current.isScrollingDisabledY && current.maxY > 0f
			if (canScrollX || canScrollY) return current
		}
		current = current.parent
	}
	return null
}

internal fun Actor?.scrollDebugPath(): String {
	if (this == null) return "-"
	val result = StringBuilder(64)
	result.append(javaClass.simpleName)
	if (!name.isNullOrEmpty()) result.append('[').append(name).append(']')
	result.append('#').append(Integer.toHexString(System.identityHashCode(this)))
	var current = parent
	while (current != null) {
		if (current is Window) {
			result.append(" in ").append(current.javaClass.simpleName)
			break
		}
		current = current.parent
	}
	return result.toString()
}

internal fun MetaScrollPane.scrollDebugState(): String =
	"${scrollDebugPath()} pos=${scrollX.toInt()},${scrollY.toInt()} max=${maxX.toInt()},${maxY.toInt()} " +
		"disabled=$isScrollingDisabledX,$isScrollingDisabledY"

/** Stage-level ownership avoids child controls and nested panes fighting over enter/exit event ordering. */
internal fun updateMetaScrollFocus(stage: Stage, hitActor: Actor?) {
	val hoveredPane = hitActor.nearestScrollableMetaScrollPane()
	if (hoveredPane != null) {
		if (stage.scrollFocus !== hoveredPane) stage.scrollFocus = hoveredPane
	} else if (stage.scrollFocus is MetaScrollPane) {
		stage.scrollFocus = null
	}
}

internal fun shiftedHorizontalScrollPosition(currentX: Float, maxX: Float, amountY: Float, step: Float = 100f): Float =
	(currentX + amountY * step).coerceIn(0f, maxX.coerceAtLeast(0f))
