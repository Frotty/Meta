package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.api.ui.MetaToastSpec
import de.fatox.meta.api.ui.MetaToastType
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.ui.components.MetaIcon
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextButton

class MetaToastManager(private val stage: Stage) {
	private val root = Table()
	internal val rootForLayoutTest: Table
		get() = root
	private var lastText = ""
	private var lastTextMs = 0L

	init {
		root.setFillParent(true)
		root.align(Align.top)
		root.padTop(MetaSpacing.LG)
		stage.addActor(root)
	}

	fun show(message: String) {
		show(message, UIManager.DEFAULT_TOAST_SECONDS)
	}

	fun show(message: String, fadeOutDelay: Float) {
		show(MetaToastSpec.notification(message, duration = fadeOutDelay))
	}

	fun show(spec: MetaToastSpec) {
		val duplicateKey = "${spec.type}:${spec.message}"
		val now = TimeUtils.millis()
		if (duplicateKey == lastText && now - lastTextMs < DUPLICATE_SUPPRESSION_MS) return
		lastText = duplicateKey
		lastTextMs = now
		present(buildToast(spec), spec.autoDismissSeconds)
	}

	fun show(table: Table, fadeOutDelay: Float) {
		present(wrapToast(table, MetaToastType.NOTIFICATION), fadeOutDelay)
	}

	private fun present(toast: Table, autoDismissSeconds: Float?) {
		if (root.stage == null) stage.addActor(root)
		root.add(toast).center().padTop(MetaSpacing.SM).row()
		toast.color.a = 0f
		if (autoDismissSeconds == null) {
			toast.addAction(Actions.fadeIn(0.12f))
		} else {
			toast.addAction(Actions.sequence(
				Actions.fadeIn(0.12f),
				Actions.delay(autoDismissSeconds),
				Actions.fadeOut(0.25f),
				Actions.run { removeToast(toast) },
			))
		}
		toFront()
	}

	fun clear() {
		root.clearChildren()
	}

	fun resize() = Unit

	fun toFront() {
		root.toFront()
	}

	fun dispose() {
		clear()
		root.remove()
	}

	private fun buildToast(spec: MetaToastSpec): Table {
		val label = MetaLabel(spec.message, MetaType.BODY, MetaColor.TEXT)
		val measuredWidth = label.prefWidth
		val textWidth = measuredWidth.coerceIn(MIN_TEXT_WIDTH, MAX_TEXT_WIDTH)
		if (measuredWidth > MAX_TEXT_WIDTH) label.setWrap(true)
		val content = MetaTable().apply {
			add(MetaIcon(iconFor(spec.type), 20, colorFor(spec.type).cpy())).size(24f).top().padRight(MetaSpacing.SM)
			add(label).width(textWidth).left()
			if (spec.primaryAction != null || spec.dismissLabel != null) {
				row()
				add()
				add(MetaTable().apply {
					defaults().height(MetaControlSize.COMPACT.height).padLeft(MetaSpacing.XS)
					spec.primaryAction?.let { toastAction ->
						add(MetaTextButton(toastAction.label, tier = toastAction.tier).onClick {
							try {
								toastAction.action()
							} finally {
								dismissToast(this@MetaToastManager.findToastAncestor(it.listenerActor))
							}
						})
					}
					spec.dismissLabel?.let { labelText ->
						add(MetaTextButton(labelText, tier = MetaButtonTier.TERTIARY).onClick {
							try {
								spec.onDismiss()
							} finally {
								dismissToast(this@MetaToastManager.findToastAncestor(it.listenerActor))
							}
						})
					}
				}).right().padTop(MetaSpacing.SM)
			}
		}
		return wrapToast(content, spec.type)
	}

	private fun wrapToast(content: Table, type: MetaToastType): Table {
		return MetaTable().apply {
			name = TOAST_ACTOR_NAME
			background = MetaSkin.skin().getDrawable(drawableFor(type))
			add(content).pad(MetaSpacing.XS, MetaSpacing.SM, MetaSpacing.XS, MetaSpacing.SM)
		}
	}

	private fun findToastAncestor(actor: com.badlogic.gdx.scenes.scene2d.Actor): Table? {
		var current: com.badlogic.gdx.scenes.scene2d.Actor? = actor
		while (current != null) {
			if (current.name == TOAST_ACTOR_NAME) return current as? Table
			current = current.parent
		}
		return null
	}

	private fun dismissToast(toast: Table?) {
		toast ?: return
		toast.clearActions()
		toast.addAction(Actions.sequence(Actions.fadeOut(0.18f), Actions.run { removeToast(toast) }))
	}

	private fun removeToast(toast: Table) {
		toast.remove()
		root.invalidate()
	}

	private fun drawableFor(type: MetaToastType): String = when (type) {
		MetaToastType.NOTIFICATION -> MetaSkin.TOAST
		MetaToastType.PRIMARY -> MetaSkin.TOAST_PRIMARY
		MetaToastType.WARNING -> MetaSkin.TOAST_WARNING
		MetaToastType.ERROR -> MetaSkin.TOAST_ERROR
		MetaToastType.MUTED -> MetaSkin.TOAST_MUTED
	}

	private fun iconFor(type: MetaToastType): String = when (type) {
		MetaToastType.NOTIFICATION -> "ri-notification-3-line"
		MetaToastType.PRIMARY -> "ri-information-line"
		MetaToastType.WARNING -> "ri-alert-line"
		MetaToastType.ERROR -> "ri-error-warning-line"
		MetaToastType.MUTED -> "ri-information-line"
	}

	private fun colorFor(type: MetaToastType) = when (type) {
		MetaToastType.NOTIFICATION, MetaToastType.PRIMARY -> MetaColor.ACCENT
		MetaToastType.WARNING -> MetaColor.WARNING
		MetaToastType.ERROR -> MetaColor.NEGATIVE
		MetaToastType.MUTED -> MetaColor.TEXT_MUTED
	}

	private companion object {
		const val DUPLICATE_SUPPRESSION_MS = 600L
		const val MIN_TEXT_WIDTH = 120f
		const val MAX_TEXT_WIDTH = 360f
		const val TOAST_ACTOR_NAME = "meta.toast.actor"
	}
}
