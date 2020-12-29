package de.fatox.meta.ui.components

import com.kotcrab.vis.ui.util.InputValidator
import com.kotcrab.vis.ui.widget.VisLabel
import de.fatox.meta.error.MetaErrorHandler

abstract class MetaInputValidator : InputValidator {
    private var errorLabel: VisLabel? = null
    override fun validateInput(input: String): Boolean {
        val errors = MetaErrorHandler()
        validateInput(input, errors)
        errorLabel.setText(errors.labelText)
        return !errors.hasErrors()
    }

    abstract fun validateInput(input: String?, errors: MetaErrorHandler?)
    fun setErrorLabel(errorLabel: VisLabel?) {
        this.errorLabel = errorLabel
    }
}