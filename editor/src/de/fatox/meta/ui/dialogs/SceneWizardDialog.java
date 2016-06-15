package de.fatox.meta.ui.dialogs;

import de.fatox.meta.ide.SceneManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.windows.MetaDialog;

/**
 * Created by Frotty on 13.06.2016.
 */
public class SceneWizardDialog extends MetaDialog {
    @Inject
    private SceneManager sceneManager;

    public SceneWizardDialog(String title) {
        super(title, "Cancel", "Finish");
    }

    @Override
    public void onResult(Object object) {
        if((boolean) object) {
            sceneManager.createNew("Test");
        }
    }
}
