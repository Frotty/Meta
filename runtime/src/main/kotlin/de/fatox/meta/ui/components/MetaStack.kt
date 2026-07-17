package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.ObjectMap
import kotlin.math.max

/** Layered layout for backgrounds, overlays, badges and other actors sharing the same bounds. */
class MetaStack(
	horizontalAlign: MetaFlexAlign = MetaFlexAlign.STRETCH,
	verticalAlign: MetaFlexAlign = MetaFlexAlign.STRETCH,
) : WidgetGroup() {
	private data class ItemSpec(
		val basisWidth: Float?,
		val basisHeight: Float?,
		val horizontalAlign: MetaFlexAlign?,
		val verticalAlign: MetaFlexAlign?,
	)

	private val itemSpecs = ObjectMap<Actor, ItemSpec>()
	private var measuredWidth = Float.NaN

	var horizontalAlign: MetaFlexAlign = horizontalAlign
		set(value) {
			field = value
			invalidate()
		}
	var verticalAlign: MetaFlexAlign = verticalAlign
		set(value) {
			field = value
			invalidate()
		}

	fun addItem(
		actor: Actor,
		basisWidth: Float? = null,
		basisHeight: Float? = null,
		horizontalAlign: MetaFlexAlign? = null,
		verticalAlign: MetaFlexAlign? = null,
	): MetaStack = apply {
		configure(actor, basisWidth, basisHeight, horizontalAlign, verticalAlign)
		addActor(actor)
		invalidateHierarchy()
	}

	fun configure(
		actor: Actor,
		basisWidth: Float? = null,
		basisHeight: Float? = null,
		horizontalAlign: MetaFlexAlign? = null,
		verticalAlign: MetaFlexAlign? = null,
	): MetaStack = apply {
		if (basisWidth != null) checkedNonNegative(basisWidth, "Stack item width")
		if (basisHeight != null) checkedNonNegative(basisHeight, "Stack item height")
		val resolvedWidth = basisWidth ?: if (actor is Layout) null else actor.width
		val resolvedHeight = basisHeight ?: if (actor is Layout) null else actor.height
		itemSpecs.put(actor, ItemSpec(resolvedWidth, resolvedHeight, horizontalAlign, verticalAlign))
		invalidateHierarchy()
	}

	override fun removeActor(actor: Actor): Boolean {
		itemSpecs.remove(actor)
		return super.removeActor(actor).also { if (it) invalidateHierarchy() }
	}

	override fun clearChildren() {
		itemSpecs.clear()
		super.clearChildren()
		invalidateHierarchy()
	}

	override fun layout() {
		for (index in 0 until children.size) {
			val actor = children[index]
			val spec = itemSpecs[actor]
			val hAlign = spec?.horizontalAlign ?: horizontalAlign
			val vAlign = spec?.verticalAlign ?: verticalAlign
			val preferredWidth = spec?.basisWidth ?: preferredWidth(actor)
			val actorWidth = if (hAlign == MetaFlexAlign.STRETCH) width else preferredWidth.coerceAtMost(width)
			val preferredHeight = spec?.basisHeight ?: preferredHeightAtWidth(actor, actorWidth)
			val actorHeight = if (vAlign == MetaFlexAlign.STRETCH) height else preferredHeight.coerceAtMost(height)
			actor.setBounds(
				alignmentOffset(hAlign, width - actorWidth),
				alignmentOffset(vAlign, height - actorHeight, invert = true),
				actorWidth,
				actorHeight,
			)
			if (actor is Layout) actor.validate()
		}
	}

	override fun sizeChanged() {
		super.sizeChanged()
		if (width != measuredWidth) {
			measuredWidth = width
			invalidateHierarchy()
		}
	}

	override fun getMinWidth(): Float = axisMaximum(horizontal = true, minimum = true)
	override fun getMinHeight(): Float = axisMaximum(horizontal = false, minimum = true)
	override fun getPrefWidth(): Float = axisMaximum(horizontal = true, minimum = false)
	override fun getPrefHeight(): Float {
		var maximum = 0f
		for (index in 0 until children.size) {
			val actor = children[index]
			val spec = itemSpecs[actor]
			val hAlign = spec?.horizontalAlign ?: horizontalAlign
			val assignedWidth = if (hAlign == MetaFlexAlign.STRETCH && width > 0f) {
				width
			} else {
				(spec?.basisWidth ?: preferredWidth(actor)).coerceAtMost(width.takeIf { it > 0f } ?: Float.MAX_VALUE)
			}
			maximum = max(maximum, spec?.basisHeight ?: preferredHeightAtWidth(actor, assignedWidth))
		}
		return maximum
	}

	private fun axisMaximum(horizontal: Boolean, minimum: Boolean): Float {
		var maximum = 0f
		for (index in 0 until children.size) {
			val actor = children[index]
			val spec = itemSpecs[actor]
			val value = if (horizontal) {
				spec?.basisWidth ?: if (minimum) minimumWidth(actor) else preferredWidth(actor)
			} else {
				spec?.basisHeight ?: if (minimum) minimumHeight(actor) else preferredHeight(actor)
			}
			maximum = max(maximum, value)
		}
		return maximum
	}

	private fun preferredWidth(actor: Actor): Float = (actor as? Layout)?.prefWidth ?: actor.width
	private fun preferredHeight(actor: Actor): Float = (actor as? Layout)?.prefHeight ?: actor.height
	private fun preferredHeightAtWidth(actor: Actor, assignedWidth: Float): Float {
		val layout = actor as? Layout ?: return actor.height
		if (actor.width != assignedWidth) {
			actor.width = assignedWidth
			layout.invalidate()
		}
		return layout.prefHeight
	}

	private fun minimumWidth(actor: Actor): Float = (actor as? Layout)?.minWidth ?: actor.width
	private fun minimumHeight(actor: Actor): Float = (actor as? Layout)?.minHeight ?: actor.height

	private companion object {
		fun alignmentOffset(align: MetaFlexAlign, free: Float, invert: Boolean = false): Float {
			val offset = when (align) {
				MetaFlexAlign.START, MetaFlexAlign.STRETCH -> 0f
				MetaFlexAlign.CENTER -> free * 0.5f
				MetaFlexAlign.END -> free
			}
			return if (invert) free - offset else offset
		}

		fun checkedNonNegative(value: Float, label: String): Float {
			require(value.isFinite() && value >= 0f) { "$label must be finite and not negative" }
			return value
		}
	}
}
