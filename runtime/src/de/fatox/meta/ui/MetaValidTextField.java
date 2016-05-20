package de.fatox.meta.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.building.OneRowTableBuilder;
import com.kotcrab.vis.ui.building.utilities.Padding;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

public class MetaValidTextField {
    private Table contentTable;
    private VisLabel descriptionLabel;
    private VisLabel errorLabel;
    private VisValidatableTextField textField;

    public MetaValidTextField(String description) {
        descriptionLabel = new VisLabel(description);
        textField = new VisValidatableTextField();
        errorLabel = new VisLabel("", Color.RED);

        OneRowTableBuilder tableBuilder = new OneRowTableBuilder(new Padding(4,0));
        tableBuilder
                .append(descriptionLabel)
                .append(textField)
                .append(errorLabel);
        contentTable = tableBuilder.build();
    }

    public Table getContentTable() {
        return contentTable;
    }

    public void addValidator(MetaInputValidator inputValidator) {
        inputValidator.setErrorLabel(errorLabel);
        textField.addValidator(inputValidator);
    }
}
