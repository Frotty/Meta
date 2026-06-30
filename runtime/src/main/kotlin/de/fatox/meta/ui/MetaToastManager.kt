package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.TimeUtils
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.ToastManager
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.toast.Toast
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.ui.components.MetaLabel

/**
 * A [ToastManager] that (1) keeps toasts above Meta's window/dialog/backdrop layering, (2) auto-fades and
 * de-duplicates rapid repeats, and (3) styles the default string toasts like the rest of the Meta UI: a TTF
 * [MetaLabel] (not the skin's baked font), comfortable padding, the same `close-window` button windows use, and
 * sane spacing from the screen edge / between stacked toasts.
 *
 * VisUI's [ToastManager] adds its root group to the stage once and never re-fronts it, and its default `show(String)`
 * builds a cramped, baked-font table. All `show(...)` overloads funnel through `show(Toast, Float)`, so overriding
 * that keeps every entry point on top.
 */
class MetaToastManager(stage: Stage) : ToastManager(stage) {
	private var lastText = ""
	private var lastTextMs = 0L

	init {
		alignment = Align.bottomRight
		setScreenPadding(MetaSpacing.LG.toInt())   // breathing room from the screen edge
		setMessagePadding(MetaSpacing.SM.toInt())  // gap between stacked toasts
	}

	override fun show(message: String) {
		show(message, UIManager.DEFAULT_TOAST_SECONDS)
	}

	override fun show(message: String, fadeOutDelay: Float) {
		val now = TimeUtils.millis()
		if (message == lastText && now - lastTextMs < DUPLICATE_SUPPRESSION_MS) return
		lastText = message
		lastTextMs = now

		// Meta-styled toast instead of VisUI's cramped, baked-font default: the skin's "dark" toast style (bordered
		// panel + matching window close button) with a TTF label and comfortable padding.
		val skin = VisUI.getSkin()
		val style = if (skin.has("dark", Toast.ToastStyle::class.java)) {
			skin.get("dark", Toast.ToastStyle::class.java)
		} else {
			skin.get(Toast.ToastStyle::class.java)
		}
		val content = VisTable().apply {
			add(MetaLabel(message, MetaType.BODY, MetaColor.TEXT))
				.pad(MetaSpacing.SM, MetaSpacing.MD, MetaSpacing.SM, MetaSpacing.SM)
				.left()
				.minWidth(120f)
		}
		show(Toast(style, content), fadeOutDelay)
	}

	override fun show(toast: Toast, fadeOutDelay: Float) {
		alignment = Align.bottomRight
		super.show(toast, fadeOutDelay)
		toFront()
	}

	private companion object {
		const val DUPLICATE_SUPPRESSION_MS = 600L
	}
}
