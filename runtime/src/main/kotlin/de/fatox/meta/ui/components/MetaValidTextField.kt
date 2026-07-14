package de.fatox.meta.ui.components

import de.fatox.meta.ui.MetaType

class MetaValidTextField(description: String, private val errorLabel: MetaLabel) {
	val description: MetaLabel = MetaLabel(description, MetaType.CAPTION)

	val textField: MetaValidatableTextField = MetaValidatableTextField()

	fun addValidator(inputValidator: MetaInputValidator) {
		inputValidator.errorLabel = errorLabel
		textField.addValidator(inputValidator)
	}
}
