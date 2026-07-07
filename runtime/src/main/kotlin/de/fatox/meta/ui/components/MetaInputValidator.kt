package de.fatox.meta.ui.components

import de.fatox.meta.error.MetaErrorHandler

abstract class MetaInputValidator {
	var errorLabel: MetaLabel? = null

	fun validateInput(input: String): Boolean {
		val labelText = validationErrorText(input)
		errorLabel?.setText(labelText)
		return labelText.isEmpty()
	}

	fun validationErrorText(input: String): String {
		val errors = MetaErrorHandler()
		validateInput(input, errors)
		return errors.labelText
	}

	abstract fun validateInput(input: String, errors: MetaErrorHandler)
}
