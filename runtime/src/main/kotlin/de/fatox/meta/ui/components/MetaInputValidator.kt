package de.fatox.meta.ui.components;

import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import de.fatox.meta.error.MetaErrorHandler;

public abstract class MetaInputValidator implements InputValidator {
    private VisLabel errorLabel;

    @Override
    public boolean validateInput(String input) {
        MetaErrorHandler errors = new MetaErrorHandler();
        validateInput(input, errors);
        errorLabel.setText(errors.getLabelText());
        return !errors.hasErrors();
    }

    public abstract void validateInput(String input, MetaErrorHandler errors);


    public void setErrorLabel(VisLabel errorLabel) {
        this.errorLabel = errorLabel;
    }

}

