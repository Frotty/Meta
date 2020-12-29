package de.fatox.meta.ui.components

import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisValidatableTextField

class MetaValidTextField(description: String?, errorLabel: VisLabel) {
    val description: VisLabel
    private val errorLabel: VisLabel
    val textField: VisValidatableTextField
    fun addValidator(inputValidator: MetaInputValidator) {
        inputValidator.setErrorLabel(errorLabel)
        textField.addValidator(inputValidator)
    }

    init {
        this.description = VisLabel(description)
        textField = VisValidatableTextField()
        this.errorLabel = errorLabel
    }
}