package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTable

class MetaToastManager(private val stage: Stage) {
	private val root = Table()
	private var lastText = ""
	private var lastTextMs = 0L

	init {
		root.setFillParent(true)
		root.top().center().padTop(MetaSpacing.LG)
		stage.addActor(root)
	}

	fun show(message: String) {
		show(message, UIManager.DEFAULT_TOAST_SECONDS)
	}

	fun show(message: String, fadeOutDelay: Float) {
		val now = TimeUtils.millis()
		if (message == lastText && now - lastTextMs < DUPLICATE_SUPPRESSION_MS) return
		lastText = message
		lastTextMs = now

		show(MetaTable().apply {
			add(MetaLabel(message, MetaType.BODY, MetaColor.TEXT))
				.left()
				.minWidth(120f)
		}, fadeOutDelay)
	}

	fun show(table: Table, fadeOutDelay: Float) {
		if (root.stage == null) stage.addActor(root)
		val toast = wrapToast(table)
		root.add(toast).center().padTop(MetaSpacing.SM).row()
		toast.color.a = 0f
		toast.addAction(
			Actions.sequence(
				Actions.fadeIn(0.12f),
				Actions.delay(fadeOutDelay),
				Actions.fadeOut(0.25f),
				Actions.run {
					toast.remove()
					root.invalidate()
				},
			)
		)
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

	private fun wrapToast(content: Table): Table {
		return MetaTable().apply {
			background = MetaSkin.skin().getDrawable(MetaSkin.TOAST)
			add(content).pad(MetaSpacing.XS, MetaSpacing.SM, MetaSpacing.XS, MetaSpacing.SM)
		}
	}

	private companion object {
		const val DUPLICATE_SUPPRESSION_MS = 600L
	}
}
