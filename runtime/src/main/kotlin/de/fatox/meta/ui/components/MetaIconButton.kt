package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing

/** A texture-backed icon on Meta's generated button background. */
class MetaIconButton(drawable: Drawable?) :
	Button(MetaSkin.skin().get(MetaSkin.ICON_BUTTON, ButtonStyle::class.java)),
	MetaFocusable {
	private val image = Image(drawable)
	private val focusStyle = MetaButtonFocusStyle(this, style, MetaSkin::focusedButtonStyle)
	private val disabledTint = MetaDisabledTint(this)

	val checkedValue: Signal<Boolean> = signal(isChecked)
	val disabledValue: Signal<Boolean> = signal(isDisabled)

	init {
		add(image).size(24f).pad(MetaSpacing.XS)
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
