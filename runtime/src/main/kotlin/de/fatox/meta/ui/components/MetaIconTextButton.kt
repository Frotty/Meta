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
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.util.GoldenRatio

/**
 * Created by Frotty on 04.06.2016.
 */
class MetaIconTextButton private constructor(
	text: String,
	iconActor: Actor,
	size: Int = 12,
	maxWidth: Int? = null,
) : Button(MetaSkin.skin().get(MetaSkin.BUTTON, Button.ButtonStyle::class.java)), MetaFocusable {
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
		size: Int = 12,
		maxWidth: Int? = null,
	) : this(text, com.badlogic.gdx.scenes.scene2d.ui.Image(drawable).apply {
		touchable = Touchable.disabled
	}, size, maxWidth)

	constructor(
		text: String,
		icon: String,
		size: Int = 12,
		iconSize: Int = 24,
		maxWidth: Int? = null,
		iconColor: Color? = Color.WHITE,
	) : this(text, MetaIcon(icon, iconSize, iconColor).apply { touchable = Touchable.disabled }, size, maxWidth)

	init {
		iconActor.touchable = Touchable.disabled
		label.touchable = Touchable.disabled
		pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20)
		add(iconActor).center().grow()
		row()
		add(label).center().grow().pad(2f)
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
