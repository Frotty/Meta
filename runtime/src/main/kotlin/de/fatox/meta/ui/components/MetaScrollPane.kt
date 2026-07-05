package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing

/**
 * A [ScrollPane] with a faster mouse-wheel step, Meta's thin generated scrollbar style and a small content gutter
 * before the scrollbar. Use this instead of raw `ScrollPane`/`VisScrollPane` so scrollable content never sits flush
 * against the thumb.
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
	}

	private companion object {
		const val MOUSE_WHEEL_STEP = 100f
		const val SCROLLBAR_GUTTER = MetaSpacing.SM

		fun defaultStyleName(): String =
			if (MetaSkin.skin().has(MetaSkin.SCROLL_PANE, ScrollPaneStyle::class.java)) MetaSkin.SCROLL_PANE else "list"

		fun wrapContent(widget: Actor?): Actor? {
			if (widget == null) return null
			return Table(MetaSkin.skin()).apply {
				add(widget).grow().padRight(SCROLLBAR_GUTTER)
			}
		}
	}
}
