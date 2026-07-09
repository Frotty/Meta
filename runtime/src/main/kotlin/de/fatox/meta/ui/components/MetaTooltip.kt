package de.fatox.meta.ui.components

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.Timer.Task
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import kotlin.math.max
import kotlin.math.min

object MetaTooltip {
	private data class AttachedTooltip(val attachment: Attachment, val listener: InputListener)

	private val attachments = ObjectMap<Actor, AttachedTooltip>(16)
	private val detachedTargets = Array<Actor>(16)
	private val orphanCleanupTask = object : Task() {
		override fun run() {
			removeDetachedTargets()
		}
	}

	private const val DEFAULT_SHOW_DELAY_SECONDS = 0f
	private const val DEFAULT_HIDE_DELAY_SECONDS = 0.04f
	private const val DEFAULT_MAX_WIDTH = 280f
	private const val ORPHAN_CLEANUP_DELAY_SECONDS = 0.5f
	private const val ORPHAN_CLEANUP_INTERVAL_SECONDS = 1f

	private class Attachment(
		private val target: Actor,
		private val tooltip: MetaTable,
		private val label: MetaLabel,
		private val labelCell: Cell<MetaLabel>,
		align: Int,
		showDelaySeconds: Float,
		hideDelaySeconds: Float,
		maxWidth: Float,
	) {
		private val anchor = Vector2()
		private val targetMin = Vector2()
		private val targetMax = Vector2()

		var align: Int = align
			private set
		var showDelaySeconds: Float = showDelaySeconds
			private set
		var hideDelaySeconds: Float = hideDelaySeconds
			private set

		private var showTask: Task? = null
		private var hideTask: Task? = null

		fun configure(
			text: String,
			align: Int,
			showDelaySeconds: Float,
			hideDelaySeconds: Float,
			maxWidth: Float,
		) {
			label.setText(text)
			this.align = align
			this.showDelaySeconds = showDelaySeconds
			this.hideDelaySeconds = hideDelaySeconds
			if (maxWidth > 0f) {
				label.setWrap(true)
				label.setAlignment(Align.left)
				label.width = maxWidth
				labelCell.width(maxWidth)
				labelCell.align(Align.left)
			} else {
				label.setWrap(false)
				label.setAlignment(Align.center)
				label.width = 0f
				labelCell.width(label.prefWidth)
			}
			tooltip.pack()
		}

		fun setAnchor(x: Float, y: Float) {
			if (target.stage == null) return
			target.localToStageCoordinates(anchor.set(x, y))
		}

		fun requestShow() {
			if (!canShow()) return
			cancelHideTask()
			cancelShowTask()
			if (showDelaySeconds <= 0f) {
				showNow()
				return
			}
			val stage = target.stage ?: return
			showTask = Timer.schedule(object : Task() {
				override fun run() {
					if (!canShow() || target.stage != stage) return
					showNow()
				}
			}, showDelaySeconds)
		}

		fun requestHide() {
			cancelShowTask()
			cancelHideTask()
			if (hideDelaySeconds <= 0f) {
				hideNow()
				return
			}
			hideTask = Timer.schedule(object : Task() {
				override fun run() {
					hideNow()
				}
			}, hideDelaySeconds)
		}

		fun dispose() {
			cancelShowTask()
			cancelHideTask()
			hideNow()
		}

		private fun showNow() {
			val stage = target.stage ?: return
			if (tooltip.parent == null || tooltip.stage !== stage) {
				tooltip.remove()
				stage.addActor(tooltip)
			}
			positionTooltip(stage)
			tooltip.isVisible = true
			tooltip.toFront()
		}

		private fun hideNow() {
			tooltip.remove()
			tooltip.isVisible = false
		}

		private fun canShow(): Boolean {
			if (target is Disableable && target.isDisabled) return false
			return target.stage != null
		}

		private fun positionTooltip(stage: com.badlogic.gdx.scenes.scene2d.Stage) {
			target.localToStageCoordinates(targetMin.set(0f, 0f))
			target.localToStageCoordinates(targetMax.set(target.width, target.height))
			val left = min(targetMin.x, targetMax.x)
			val right = max(targetMin.x, targetMax.x)
			val bottom = min(targetMin.y, targetMax.y)
			val top = max(targetMin.y, targetMax.y)
			val targetCenterX = (left + right) * 0.5f
			val targetCenterY = (bottom + top) * 0.5f
			val gap = MetaSpacing.SM

			var x = targetCenterX - tooltip.width * 0.5f
			var y = top + gap

			when {
				align and Align.right != 0 && right + gap + tooltip.width <= stage.width - MetaSpacing.XS -> {
					x = right + gap
					y = targetCenterY - tooltip.height * 0.5f
				}
				align and Align.left != 0 && left - gap - tooltip.width >= MetaSpacing.XS -> {
					x = left - gap - tooltip.width
					y = targetCenterY - tooltip.height * 0.5f
				}
				top + gap + tooltip.height <= stage.height - MetaSpacing.XS -> {
					x = targetCenterX - tooltip.width * 0.5f
					y = top + gap
				}
				bottom - gap - tooltip.height >= MetaSpacing.XS -> {
					x = targetCenterX - tooltip.width * 0.5f
					y = bottom - gap - tooltip.height
				}
				right + gap + tooltip.width <= stage.width - MetaSpacing.XS -> {
					x = right + gap
					y = targetCenterY - tooltip.height * 0.5f
				}
				left - gap - tooltip.width >= MetaSpacing.XS -> {
					x = left - gap - tooltip.width
					y = targetCenterY - tooltip.height * 0.5f
				}
			}

			x = min(x, stage.width - tooltip.width - MetaSpacing.XS)
			y = min(y, stage.height - tooltip.height - MetaSpacing.XS)
			x = max(MetaSpacing.XS, x)
			y = max(MetaSpacing.XS, y)
			tooltip.setPosition(x, y)
		}

		private fun cancelShowTask() {
			showTask?.cancel()
			showTask = null
		}

		private fun cancelHideTask() {
			hideTask?.cancel()
			hideTask = null
		}
	}

	fun attach(
		target: Actor?,
		text: String,
		align: Int = Align.center,
		showDelaySeconds: Float = DEFAULT_SHOW_DELAY_SECONDS,
		hideDelaySeconds: Float = DEFAULT_HIDE_DELAY_SECONDS,
		maxWidth: Float = DEFAULT_MAX_WIDTH,
	) {
		if (target == null) return
		val existing = attachments[target]
		if (existing != null) {
			existing.attachment.configure(text, align, showDelaySeconds, hideDelaySeconds, maxWidth)
			return
		}

		val label = MetaLabel(text, MetaType.CAPTION, MetaColor.TEXT).apply {
			if (maxWidth > 0f) {
				setWrap(true)
				setAlignment(Align.left)
				width = maxWidth
			}
		}
		val tooltip = MetaTable().apply {
			background = MetaSkin.skin().getDrawable("meta.tooltip")
			isVisible = false
		}
		val labelCell = tooltip.add(label).pad(MetaSpacing.SM)
		lateinit var attachment: Attachment
		attachment = Attachment(
			target = target,
			tooltip = tooltip,
			label = label,
			labelCell = labelCell,
			align = align,
			showDelaySeconds = showDelaySeconds,
			hideDelaySeconds = hideDelaySeconds,
			maxWidth = maxWidth,
		)
		attachment.configure(text, align, showDelaySeconds, hideDelaySeconds, maxWidth)
		val listener = object : InputListener() {
			override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
				if (pointer != -1) return
				if (target is Disableable && target.isDisabled) return
				attachment.setAnchor(x, y)
				attachment.requestShow()
			}

			override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
				if (pointer != -1) return
				if (toActor != null && (toActor === target || toActor.isDescendantOf(target))) return
				attachment.requestHide()
			}
		}
		target.addListener(listener)
		attachments.put(target, AttachedTooltip(attachment, listener))
		scheduleOrphanCleanupIfNeeded()
	}

	fun remove(target: Actor?) {
		if (target == null) return
		val attached = attachments.remove(target) ?: return
		attached.attachment.dispose()
		target.removeListener(attached.listener)
		if (attachments.size == 0) orphanCleanupTask.cancel()
	}

	// Keep parity with VisUI API shape.
	fun removeTooltip(target: Actor) = remove(target)

	private fun Actor.isDescendantOf(target: Actor): Boolean {
		var current: Actor? = this
		while (current != null) {
			if (current === target) return true
			current = current.parent
		}
		return false
	}

	private fun removeDetachedTargets() {
		if (attachments.isEmpty) return

		detachedTargets.clear()
		for (entry in attachments) {
			if (entry.key.stage == null) {
				detachedTargets.add(entry.key)
			}
		}

		for (i in 0 until detachedTargets.size) {
			remove(detachedTargets[i])
		}

		detachedTargets.clear()

		if (attachments.isEmpty) orphanCleanupTask.cancel()
	}

	private fun scheduleOrphanCleanupIfNeeded() {
		if (orphanCleanupTask.isScheduled) return
		Timer.schedule(orphanCleanupTask, ORPHAN_CLEANUP_DELAY_SECONDS, ORPHAN_CLEANUP_INTERVAL_SECONDS)
	}
}
