package de.fatox.meta.ui;

import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import de.fatox.meta.error.MetaErrors;

public abstract class MetaInputValidator implements InputValidator {
    private VisLabel errorLabel;

    @Override
    public boolean validateInput(String input) {
        MetaErrors errors = new MetaErrors();
        validateInput(input, errors);
        errorLabel.setText(errors.getLabelText());
        return !errors.hasErrors();
    }

    public abstract void validateInput(String input, MetaErrors errors);


    public void setErrorLabel(VisLabel errorLabel) {
        this.errorLabel = errorLabel;
    }

}

