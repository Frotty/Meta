package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaButtonTier
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType

/**
 * Created by Frotty on 04.06.2016.
 */
class MetaIconTextButton private constructor(
	text: String,
	iconActor: Actor,
	size: Int = MetaType.BODY,
	maxWidth: Int? = null,
	vertical: Boolean = false,
	tier: MetaButtonTier = MetaButtonTier.SECONDARY,
) : Button(MetaSkin.buttonStyle(tier)), MetaFocusable {
	private val label: MetaLabel = MetaLabel(text, size, Color.WHITE).apply {
		setAlignment(Align.center)
		if (maxWidth != null) setMaxWidth(maxWidth)
	}
	private val focusStyle = MetaButtonFocusStyle(this, style, MetaSkin::focusedButtonStyle)
	private val disabledTint = MetaDisabledTint(this)
	val textValue: Signal<CharSequence> = signal(text)
	val checkedValue: Signal<Boolean> = signal(isChecked)
	val disabledValue: Signal<Boolean> = signal(isDisabled)
	var text: CharSequence
		get() = label.text
		set(value) {
			label.setText(value)
			textValue.value = value
		}

	constructor(
		text: String,
		drawable: Drawable?,
		size: Int = MetaType.BODY,
		maxWidth: Int? = null,
		vertical: Boolean = false,
		tier: MetaButtonTier = MetaButtonTier.SECONDARY,
	) : this(text, com.badlogic.gdx.scenes.scene2d.ui.Image(drawable).apply {
		touchable = Touchable.disabled
	}, size, maxWidth, vertical, tier)

	constructor(
		text: String,
		icon: String,
		size: Int = MetaType.BODY,
		iconSize: Int = MetaIconButton.DEFAULT_ICON_SIZE.toInt(),
		maxWidth: Int? = null,
		iconColor: Color? = Color.WHITE,
		vertical: Boolean = false,
		tier: MetaButtonTier = MetaButtonTier.SECONDARY,
	) : this(text, MetaIcon(icon, iconSize, iconColor).apply { touchable = Touchable.disabled }, size, maxWidth, vertical, tier)

	init {
		iconActor.touchable = Touchable.disabled
		label.touchable = Touchable.disabled
		pad(MetaSpacing.SM, MetaSpacing.MD, MetaSpacing.SM, MetaSpacing.MD)
		add(iconActor).center()
		if (vertical) {
			row()
			add(label).center().growX().padTop(MetaSpacing.XS)
		} else {
			add(label).center().growX().padLeft(MetaSpacing.SM)
		}
		cursorPointer()
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
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
}
