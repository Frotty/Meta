package de.fatox.meta.ui.components

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisValidatableTextField

class MetaValidTextField(description: String?, private val errorLabel: VisLabel) {
	val description: VisLabel = VisLabel(description)

	val textField: VisValidatableTextField = VisValidatableTextField()

	fun addValidator(inputValidator: MetaInputValidator) {
		inputValidator.errorLabel = errorLabel
		textField.addValidator(inputValidator)
	}
}