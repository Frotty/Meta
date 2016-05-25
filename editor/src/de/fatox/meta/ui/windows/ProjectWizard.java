package de.fatox.meta.ui.windows;

import com.kotcrab.vis.ui.building.StandardTableBuilder;
import com.kotcrab.vis.ui.building.utilities.CellWidget;
import com.kotcrab.vis.ui.building.utilities.Padding;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisDialog;
import de.fatox.meta.Meta;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.error.MetaError;
import de.fatox.meta.error.MetaErrors;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.components.MetaInputValidator;
import de.fatox.meta.ui.components.MetaValidTextField;
import de.fatox.meta.util.StringUtil;

public class ProjectWizard extends VisDialog {
    @Inject
    private LanguageBundle languageBundle;

    private MetaValidTextField projectNameTF;


    public ProjectWizard(String title) {
        super(title);
        Meta.inject(this);

        createProjectNameTF();

        StandardTableBuilder tableBuilder = new StandardTableBuilder(new Padding(4,4));

        tableBuilder.append(CellWidget.of(new Separator()).fillX().expandX().wrap()).row();
        tableBuilder.append(projectNameTF.getContentTable());
        tableBuilder.build(this.getContentTable());
    }

    private void createProjectNameTF() {
        final ProjectWizard projectWizard = this;
        projectNameTF = new MetaValidTextField(languageBundle.get("newproj_dia_name_tf"));
        projectNameTF.addValidator(new MetaInputValidator() {
            @Override
            public void validateInput(String input, MetaErrors errors) {
                if(! StringUtil.isValidFolderName(input)) {
                    errors.add(new MetaError("Invalid Project Name", "Name can only contain alphanumeric characters") {
                        @Override
                        public void gotoError() {
                            projectWizard.getStage().setKeyboardFocus(projectWizard);
                        }
                    });
                }
            }
        });
    }
}
