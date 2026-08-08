package de.fatox.meta.ui.components

import de.fatox.meta.error.MetaError
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

	companion object {
		/** Creates a validator that reports [errorMessage] when [isValid] rejects the input. */
		@JvmStatic
		fun fromPredicate(errorMessage: String, isValid: (String) -> Boolean): MetaInputValidator =
			object : MetaInputValidator() {
				override fun validateInput(input: String, errors: MetaErrorHandler) {
					if (!isValid(input)) errors.add(MetaError(errorMessage, ""))
				}
			}

		/** Creates a validator that rejects blank input. */
		@JvmStatic
		fun required(errorMessage: String): MetaInputValidator = fromPredicate(errorMessage, String::isNotBlank)
	}
}
