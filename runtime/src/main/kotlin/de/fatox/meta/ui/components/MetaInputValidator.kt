package de.fatox.meta.ui.components

import de.fatox.meta.error.MetaErrorHandler

abstract class MetaInputValidator {
	var errorLabel: MetaLabel? = null

	fun validateInput(input: String): Boolean {
		val errors = MetaErrorHandler()
		validateInput(input, errors)
		errorLabel?.setText(errors.labelText)
		return !errors.hasErrors()
	}

	abstract fun validateInput(input: String, errors: MetaErrorHandler)
}
