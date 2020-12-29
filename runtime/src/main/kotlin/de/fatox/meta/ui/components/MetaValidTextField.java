package de.fatox.meta.ui.components;

import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

public class MetaValidTextField {
    private VisLabel descriptionLabel;
    private VisLabel errorLabel;

    private VisValidatableTextField textField;

    public MetaValidTextField(String description, VisLabel errorLabel) {
        descriptionLabel = new VisLabel(description);
        textField = new VisValidatableTextField();
        this.errorLabel = errorLabel;
    }

    public VisValidatableTextField getTextField() {
        return textField;
    }

    public VisLabel getDescription() {
        return descriptionLabel;
    }

    public void addValidator(MetaInputValidator inputValidator) {
        inputValidator.setErrorLabel(errorLabel);
        textField.addValidator(inputValidator);
    }
}
