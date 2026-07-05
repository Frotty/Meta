package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

/** Validatable text field using Meta's TTF font and generated field states. */
class MetaValidatableTextField @JvmOverloads constructor(
	text: String = "",
	size: Int = MetaType.BODY,
	fontProvider: FontProvider = inject(),
) : MetaTextField(text, size, fontProvider) {
	private val validators = Array<MetaInputValidator>()
	private val defaultStyle = textFieldStyle(size, fontProvider)
	private val errorStyle = textFieldStyle(size, fontProvider, MetaSkin.TEXT_FIELD_ERROR)

	var isInputValid: Boolean = true
		private set

	init {
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				validateInput()
			}
		})
	}

	fun addValidator(validator: MetaInputValidator) {
		validators.add(validator)
		validateInput()
	}

	fun validateInput(): Boolean {
		var valid = true
		for (i in 0 until validators.size) valid = validators[i].validateInput(text) && valid
		isInputValid = valid
		installMetaTextFieldStyle(if (valid) defaultStyle else errorStyle)
		return valid
	}
}
