package de.fatox.meta.ui.dialogs;

import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.error.MetaError;
import de.fatox.meta.error.MetaErrorHandler;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.shader.MetaShaderComposer;
import de.fatox.meta.ui.components.MetaInputValidator;
import de.fatox.meta.ui.components.MetaValidTextField;
import de.fatox.meta.ui.windows.MetaDialog;
import de.fatox.meta.util.StringUtil;

/**
 * Created by Frotty on 29.06.2016.
 */
@Singleton
public class ShaderCompositionWizard extends MetaDialog {
    @Inject
    private MetaShaderComposer metaShaderComposer;
    @Inject
    private ProjectManager projectManager;

    private final VisTextButton cancelBtn;
    private final VisTextButton createBtn;

    private MetaValidTextField compNameTF;

    public ShaderCompositionWizard() {
        super("Composition Wizard", true);

        cancelBtn = addButton(new VisTextButton("Cancel"), Align.left, false);
        createBtn = addButton(new VisTextButton("Create"), Align.right, true);
        compNameTF = new MetaValidTextField("Composition name:", statusLabel);
        compNameTF.addValidator(new MetaInputValidator() {
            @Override
            public void validateInput(String input, MetaErrorHandler errors) {
                if (StringUtil.isBlank(input)) {
                    errors.add(new MetaError("Invalid composition name", "") {
                        @Override
                        public void gotoError() {

                        }
                    });
                } else {
                    checkButton();
                }
            }
        });
        createBtn.setDisabled(true);
        setDefaultSize(400, 120);
        setupTable();
    }


    private void checkButton() {
        if (!StringUtil.isBlank(compNameTF.getTextField().getText())) {
            createBtn.setDisabled(false);
        } else {
            createBtn.setDisabled(true);
        }
    }

    private void setupTable() {
        VisTable visTable = new VisTable();
        visTable.defaults().pad(4);
        visTable.add(compNameTF.getDescription()).growX();
        visTable.add(compNameTF.getTextField()).growX();
        visTable.row();

        getContentTable().add(visTable).top().growX();

        setDialogListener((Object object) -> {
            if (object != null) {
                if ((boolean) object) {
                    metaShaderComposer.newShaderComposition(compNameTF.getTextField().getText());
//                    ShaderComposerWindow window = uiManager.getWindow(ShaderComposerWindow.class);
//                    if(window != null) {
//                        window.addComposition(shaderComposition);
//                    }

                }
            }
            close();
        });
    }

}
