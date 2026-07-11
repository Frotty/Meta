package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing

/**
 * A [ScrollPane] with a faster mouse-wheel step, Meta's thin generated scrollbar style and a small content gutter
 * before the scrollbar. Use this instead of raw `ScrollPane`/`VisScrollPane` so scrollable content never sits flush
 * against the thumb.
 * Uses the borderless `meta.scrollPane.flat` style by default; use `MetaSkin.SCROLL_PANE` explicitly when you need
 * a bordered container look.
 */
class MetaScrollPane : ScrollPane {
	private var previousScrollFocus: Actor? = null

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
		// Capture order is outer -> inner, so the deepest hovered pane gets the final focus claim.
		addCaptureListener(object : InputListener() {
			override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
				if (pointer == MOUSE_POINTER) claimScrollFocus(event)
			}

			override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
				// scene2d does not reliably synthesize a distinct enter callback when moving between nested groups.
				// Mouse-move events do bubble, so use their deepest target to correct ownership without the outer pane
				// reclaiming the same event.
				claimScrollFocus(event)
				return false
			}

			override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
				if (pointer != MOUSE_POINTER || event.target.nearestMetaScrollPane() !== this@MetaScrollPane ||
					event.stage.scrollFocus !== this@MetaScrollPane) return
				val previous = previousScrollFocus
				previousScrollFocus = null
				event.stage.scrollFocus = if (previous?.stage === event.stage) previous else null
			}
		})
	}

	private fun claimScrollFocus(event: InputEvent) {
		val hoveredPane = event.stage.hit(event.stageX, event.stageY, true).nearestMetaScrollPane()
		if (hoveredPane !== this || event.stage.scrollFocus === this) return
		previousScrollFocus = event.stage.scrollFocus
		event.stage.scrollFocus = this
	}

	private fun Actor?.nearestMetaScrollPane(): MetaScrollPane? {
		var current = this
		while (current != null) {
			if (current is MetaScrollPane) return current
			current = current.parent
		}
		return null
	}

	private companion object {
		const val MOUSE_WHEEL_STEP = 100f
		const val MOUSE_POINTER = -1
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
