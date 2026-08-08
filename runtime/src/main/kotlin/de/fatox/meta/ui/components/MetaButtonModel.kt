package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe

/** Shared bidirectional checked/disabled signals and disabled tint handling for Meta's [Button]-based controls. */
internal class MetaButtonModel(
	private val button: Button,
	private val disabledTint: MetaDisabledTint,
) {
	val checkedValue: Signal<Boolean> = signal(button.isChecked)
	val disabledValue: Signal<Boolean> = signal(button.isDisabled)

	@Suppress("unused")
	private val checkedBinding = checkedValue.subscribe {
		val checked = checkedValue.peek()
		if (button.isChecked != checked) button.isChecked = checked
	}

	@Suppress("unused")
	private val disabledBinding = disabledValue.subscribe {
		val disabled = disabledValue.peek()
		if (button.isDisabled != disabled) button.isDisabled = disabled
	}

	fun syncChecked() {
		checkedValue.value = button.isChecked
	}

	fun syncDisabled() {
		button.touchable = if (button.isDisabled) Touchable.disabled else Touchable.enabled
		batch {
			disabledValue.value = button.isDisabled
			disabledTint.apply(button.isDisabled)
		}
	}

	/** Applies disabled tint to children added or replaced while the button is already disabled. */
	fun refreshDisabledAppearance() {
		if (button.isDisabled) disabledTint.apply(true)
	}
}
