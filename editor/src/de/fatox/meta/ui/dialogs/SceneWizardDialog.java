package de.fatox.meta.ui.dialogs;

import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.error.MetaError;
import de.fatox.meta.error.MetaErrorHandler;
import de.fatox.meta.ide.SceneManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.components.MetaInputValidator;
import de.fatox.meta.ui.components.MetaValidTextField;
import de.fatox.meta.ui.windows.MetaDialog;
import de.fatox.meta.util.StringUtil;

/**
 * Created by Frotty on 13.06.2016.
 */
@Singleton
public class SceneWizardDialog extends MetaDialog {
    private final VisTextButton cancelBtn;
    private final VisTextButton createBtn;
    @Inject
    private SceneManager sceneManager;

    private MetaValidTextField sceneNameTF;

    public SceneWizardDialog() {
        super("Scene Wizard", true);

        cancelBtn = addButton(new VisTextButton("Cancel"), Align.left, false);
        createBtn = addButton(new VisTextButton("Create"), Align.right, true);
        sceneNameTF = new MetaValidTextField("Scene name:", statusLabel);
        sceneNameTF.addValidator(new MetaInputValidator() {
            @Override
            public void validateInput(String input, MetaErrorHandler errors) {
                if(StringUtil.isBlank(input)) {
                    errors.add(new MetaError("Scene name required", "") {
                        @Override
                        public void gotoError() {

                        }
                    });
                } else {
                    createBtn.setDisabled(false);
                }
            }
        });

        VisTable visTable = new VisTable();
        visTable.defaults().pad(4);
        visTable.add(sceneNameTF.getDescription()).growX();
        visTable.add(sceneNameTF.getTextField()).growX();
        visTable.row();
        contentTable.add(visTable).top().growX();
        createBtn.setDisabled(true);

        setDialogListener((Object object) -> {
            if((boolean) object) {
                sceneManager.createNew(sceneNameTF.getTextField().getText());
            }
            close();
        });
        setDefaultSize(200, 400);
    }

}
