package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Scaling
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing

/**
 * A dedicated "action" icon button (full button affordance, text-label-less variant). For plain clickable media, use
 * [MetaImageButton] which intentionally keeps a subtler surface treatment.
 */
open class MetaIconButton private constructor(
	iconActor: Actor,
	iconSize: Float,
	style: String = MetaSkin.ICON_BUTTON,
) :
	Button(MetaSkin.skin().get(style, ButtonStyle::class.java)),
	MetaFocusable {
	constructor(
		drawable: Drawable?,
		style: String = MetaSkin.ICON_BUTTON,
	) : this(Image(drawable).apply {
		setScaling(Scaling.fit)
		touchable = Touchable.disabled
	}, DEFAULT_ICON_SIZE, style)

	constructor(
		icon: String,
		style: String = MetaSkin.ICON_BUTTON,
		size: Int = DEFAULT_ICON_SIZE.toInt(),
		color: Color? = Color.WHITE,
	) : this(MetaIcon(icon, size, color).apply { touchable = Touchable.disabled }, size.toFloat(), style)

	private val iconContainer = Container<Actor>(iconActor).apply { touchable = Touchable.disabled }
	private val iconCell = add(iconContainer).size(iconSize).pad(MetaSpacing.XS)
	private val buttonStyle = ButtonStyle(MetaSkin.skin().get(style, ButtonStyle::class.java))
	private val focusStyle = MetaButtonFocusStyle(
		this,
		buttonStyle,
		if (style == MetaSkin.IMAGE_BUTTON) MetaSkin::focusedImageButtonStyle else MetaSkin::focusedButtonStyle,
	)
	private val selectedStyleFactory =
		if (style == MetaSkin.IMAGE_BUTTON) MetaSkin::selectedImageButtonStyle else MetaSkin::selectedButtonStyle
	private val disabledTint = MetaDisabledTint(this)

	val checkedValue: Signal<Boolean> = signal(isChecked)
	val disabledValue: Signal<Boolean> = signal(isDisabled)
	val selectedValue: Signal<Boolean> = signal(false)
	var momentary: Boolean = true
	var selected: Boolean = false
		set(value) {
			if (field == value) return
			field = value
			selectedValue.value = value
			installVisualStyle()
		}

	init {
		cursorPointer()
		// The momentary reset below calls setChecked from inside a ChangeEvent listener; with libGDX's default
		// programmaticChangeEvents that fired a SECOND nested ChangeEvent, running every consumer onChange twice
		// per click. Programmatic state changes still sync [checkedValue] via the setChecked override.
		setProgrammaticChangeEvents(false)
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				if (momentary && isChecked) {
					setChecked(false)
					return
				}
				checkedValue.value = isChecked
			}
		})
	}

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	override fun setDisabled(isDisabled: Boolean) {
		super.setDisabled(isDisabled)
		touchable = if (isDisabled) Touchable.disabled else Touchable.enabled
		batch {
			disabledValue.value = isDisabled
			disabledTint.apply(isDisabled)
		}
	}

	override fun setChecked(isChecked: Boolean) {
		super.setChecked(isChecked)
		checkedValue.value = this.isChecked
	}

	fun setIcon(drawable: Drawable?, scaling: Scaling = Scaling.fit, size: Float = DEFAULT_ICON_SIZE) {
		setIconActor(Image(drawable).apply {
			setScaling(scaling)
			touchable = Touchable.disabled
		}, size)
	}

	fun setIcon(icon: String, size: Int = DEFAULT_ICON_SIZE.toInt(), color: Color? = Color.WHITE) {
		setIconActor(MetaIcon(icon, size, color).apply { touchable = Touchable.disabled }, size.toFloat())
	}

	fun setIconSize(size: Float) {
		iconCell.size(size)
		(iconContainer.actor as? MetaIcon)?.setFontSize(size.toInt())
		invalidateHierarchy()
	}

	private fun installVisualStyle() {
		focusStyle.install(if (selected) selectedStyleFactory(buttonStyle) else buttonStyle)
	}

	private fun setIconActor(actor: Actor, size: Float) {
		actor.touchable = Touchable.disabled
		iconContainer.actor = actor
		iconCell.size(size)
		invalidateHierarchy()
	}

	companion object {
		const val DEFAULT_ICON_SIZE = 24f
	}
}
