package de.fatox.meta.ui.components

class MetaValidTextField(description: String, private val errorLabel: MetaLabel) {
	val description: MetaLabel = MetaLabel(description, 14)

	val textField: MetaValidatableTextField = MetaValidatableTextField()

	fun addValidator(inputValidator: MetaInputValidator) {
		inputValidator.errorLabel = errorLabel
		textField.addValidator(inputValidator)
	}
}
