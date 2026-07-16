package de.fatox.meta.ui.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
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
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.min

object MetaTooltip {
	private data class AttachedTooltip(val attachment: Attachment, val listener: InputListener)
	internal data class TextWidth(val width: Float, val wrap: Boolean)

	private val attachments = WeakHashMap<Actor, AttachedTooltip>()
	private val visibleTooltips = Array<Attachment>(4)
	private val layers = ObjectMap<Stage, Group>(2)

	private const val DEFAULT_SHOW_DELAY_SECONDS = 0f
	private const val DEFAULT_HIDE_DELAY_SECONDS = 0.04f
	private const val DEFAULT_MAX_WIDTH = 280f

	private class Attachment(
		target: Actor,
		val tooltip: MetaTable,
		private val label: MetaLabel,
		private val labelCell: Cell<MetaLabel>,
		align: Int,
		showDelaySeconds: Float,
		hideDelaySeconds: Float,
		maxWidth: Float,
	) {
		private val target = WeakReference(target)
		private val targetMin = Vector2()
		private val targetMax = Vector2()
		private val pointerPosition = Vector2()

		var align: Int = align
			private set
		var showDelaySeconds: Float = showDelaySeconds
			private set
		var hideDelaySeconds: Float = hideDelaySeconds
			private set

		private var showTask: Task? = null
		private var hideTask: Task? = null
		private var pointerInside = false

		fun pointerEntered() {
			pointerInside = true
			requestShow()
		}

		fun pointerExited() {
			pointerInside = false
			requestHide()
		}

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
			label.setWrap(false)
			label.setAlignment(Align.center)
			label.width = 0f
			label.invalidateHierarchy()

			val textWidth = resolveTextWidth(label.prefWidth, maxWidth)
			label.setWrap(textWidth.wrap)
			label.setAlignment(if (textWidth.wrap) Align.left else Align.center)
			label.width = textWidth.width
			labelCell.width(textWidth.width)
			labelCell.align(if (textWidth.wrap) Align.left else Align.center)
			tooltip.pack()
		}

		fun requestShow() {
			val target = target.get() ?: return
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
					val currentTarget = this@Attachment.target.get() ?: return
					if (!canShow() || currentTarget.stage != stage) return
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
			val target = target.get() ?: return
			val stage = target.stage ?: return
			// A resize/reflow can synthesize enter events for several actors before scene2d catches up with exits.
			// A pointer can only own one tooltip, so close every previous one before presenting this attachment.
			var i = visibleTooltips.size - 1
			while (i >= 0) {
				val visible = visibleTooltips[i]
				if (visible !== this) visible.hideNow()
				i--
			}
			val layer = layerFor(stage)
			if (tooltip.parent !== layer) {
				tooltip.remove()
				layer.addActor(tooltip)
			}
			positionTooltip(stage)
			tooltip.isVisible = true
			markVisible(this)
			bringLayerToFront(stage)
			tooltip.toFront()
		}

		private fun hideNow() {
			val stage = tooltip.stage
			tooltip.remove()
			tooltip.isVisible = false
			markHidden(this)
			if (stage != null) removeLayerIfEmpty(stage)
		}

		private fun canShow(): Boolean {
			val target = target.get() ?: return false
			if (target is Disableable && target.isDisabled) return false
			return pointerInside && target.stage != null
		}

		/** Keeps a shown tooltip honest when its target is moved, detached, or replaced without an exit event. */
		fun maintainVisibility(): Boolean {
			val target = target.get()
			val stage = target?.stage
			if (!canShow() || stage == null || tooltip.stage !== stage || !isPointerOverTarget(target, stage)) {
				hideNow()
				return false
			}
			positionTooltip(stage)
			return true
		}

		private fun isPointerOverTarget(target: Actor, stage: Stage): Boolean {
			pointerPosition.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
			stage.screenToStageCoordinates(pointerPosition)
			target.stageToLocalCoordinates(pointerPosition)
			return pointerPosition.x >= 0f && pointerPosition.x <= target.width &&
				pointerPosition.y >= 0f && pointerPosition.y <= target.height
		}

		private fun positionTooltip(stage: com.badlogic.gdx.scenes.scene2d.Stage) {
			val target = target.get() ?: return
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

		fun configuredText(): String = label.text.toString()
		fun isVisible(): Boolean = tooltip.isVisible && tooltip.stage != null
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
			setAlignment(Align.center)
		}
		val tooltip = MetaTable().apply {
			background = MetaSkin.skin().getDrawable("meta.tooltip")
			isVisible = false
		}
		val labelCell = tooltip.add(label).pad(MetaSpacing.SM)
		val attachment = Attachment(
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
				val currentTarget = event.listenerActor
				if (hasAttachedDescendant(event.target, currentTarget)) return
				attachment.pointerEntered()
			}

			override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
				if (pointer != -1) return
				val currentTarget = event.listenerActor
				if (toActor != null && (toActor === currentTarget || toActor.isDescendantOf(currentTarget))) return
				attachment.pointerExited()
				requestNearestAttachedAncestor(toActor, currentTarget)
			}
		}
		target.addListener(listener)
		attachments[target] = AttachedTooltip(attachment, listener)
	}

	fun remove(target: Actor?) {
		if (target == null) return
		val attached = attachments.remove(target) ?: return
		attached.attachment.dispose()
		target.removeListener(attached.listener)
	}

	// Kept as a convenience alias for existing Meta consumers.
	fun removeTooltip(target: Actor) = remove(target)

	private fun Actor.isDescendantOf(target: Actor): Boolean {
		var current: Actor? = this
		while (current != null) {
			if (current === target) return true
			current = current.parent
		}
		return false
	}

	/** Scene2D bubbles enter events, so the deepest registered tooltip target must win over attached ancestors. */
	private fun hasAttachedDescendant(eventTarget: Actor?, currentTarget: Actor): Boolean {
		var current = eventTarget
		while (current != null && current !== currentTarget) {
			if (attachments.containsKey(current)) return true
			current = current.parent
		}
		return false
	}

	/** When leaving a nested tooltip target, resume the closest attached ancestor without requiring pointer motion. */
	private fun requestNearestAttachedAncestor(actor: Actor?, excluded: Actor) {
		var current = actor
		while (current != null) {
			if (current !== excluded) {
				val attached = attachments[current]
				if (attached != null) {
					attached.attachment.pointerEntered()
					return
				}
			}
			current = current.parent
		}
	}

	internal fun bringVisibleToFront() {
		var i = 0
		while (i < visibleTooltips.size) {
			val attachment = visibleTooltips[i]
			if (!attachment.maintainVisibility()) {
				continue
			}
			val tooltip = attachment.tooltip
			val stage = tooltip.stage ?: continue
			if (tooltip.parent?.children?.peek() !== tooltip) tooltip.toFront()
			bringLayerToFront(stage)
			i++
		}
	}

	private fun layerFor(stage: Stage): Group {
		val existing = layers[stage]
		if (existing != null && existing.stage === stage) {
			existing.setSize(stage.width, stage.height)
			return existing
		}

		existing?.remove()
		val layer = Group().apply {
			touchable = Touchable.disabled
			setSize(stage.width, stage.height)
		}
		layers.put(stage, layer)
		stage.addActor(layer)
		return layer
	}

	private fun bringLayerToFront(stage: Stage) {
		val layer = layers[stage] ?: return
		layer.setSize(stage.width, stage.height)
		if (layer.parent?.children?.peek() !== layer) layer.toFront()
	}

	private fun removeLayerIfEmpty(stage: Stage) {
		val layer = layers[stage] ?: return
		if (layer.children.size > 0) return
		layer.remove()
		layers.remove(stage)
	}

	private fun markVisible(attachment: Attachment) {
		if (!visibleTooltips.contains(attachment, true)) visibleTooltips.add(attachment)
	}

	private fun markHidden(attachment: Attachment) {
		visibleTooltips.removeValue(attachment, true)
	}

	internal fun resolveTextWidth(contentWidth: Float, maxWidth: Float): TextWidth {
		if (maxWidth <= 0f || contentWidth <= maxWidth) return TextWidth(contentWidth, false)
		return TextWidth(maxWidth, true)
	}

	internal fun isAttached(target: Actor): Boolean = attachments.containsKey(target)
	internal fun configuredText(target: Actor): String? = attachments[target]?.attachment?.configuredText()
	internal fun isVisible(target: Actor): Boolean = attachments[target]?.attachment?.isVisible() == true
}
