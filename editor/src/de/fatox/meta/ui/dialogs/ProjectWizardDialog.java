package de.fatox.meta.ui.dialogs;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import de.fatox.meta.api.model.MetaProjectData;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.error.MetaError;
import de.fatox.meta.error.MetaErrorHandler;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Named;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaInputValidator;
import de.fatox.meta.ui.components.MetaTextButton;
import de.fatox.meta.ui.components.MetaValidTextField;
import de.fatox.meta.ui.windows.MetaDialog;
import de.fatox.meta.util.StringUtil;
@Singleton
public class ProjectWizardDialog extends MetaDialog {
    private final VisTextButton createBtn;
    @Inject
    private LanguageBundle languageBundle;
    @Inject
    @Named("open")
    private FileChooser fileChooser;
    @Inject
    private ProjectManager projectManager;

    private MetaValidTextField projectNameTF;
    private MetaTextButton folderButton;
    private VisLabel folderLabel;
    private FileHandle rootfile;
    private boolean namevalid;
    private boolean locationValid;
    private VisCheckBox checkbox;
    private VisLabel checkboxLabel;


    public ProjectWizardDialog(String title) {
        super(title, true);

        addButton(new VisTextButton("Cancel"), Align.left, false);
        createBtn = addButton(new VisTextButton("Create"), Align.right, true);
        createProjectNameTF();
        createFolderButton();
        createExampleCheckbox();

        VisTable visTable = new VisTable();
        visTable.defaults().pad(4);
        visTable.add(projectNameTF.getDescription()).growX();
        visTable.add(projectNameTF.getTextField()).growX();
        visTable.row();
        visTable.add(folderLabel).growX();
        visTable.add(folderButton).growX();
        visTable.row();
        visTable.add(checkboxLabel).growX();
        visTable.add(checkbox).growX();
        getContentTable().add(visTable).top().growX();
        createBtn.setDisabled(true);

        setDialogListener(object -> {
            if ((boolean) object) {
                MetaProjectData metaProjectData = new MetaProjectData(projectNameTF.getTextField().getText());
                projectManager.saveProject(metaProjectData);
                projectManager.loadProject(projectManager.getCurrentProjectRoot());
            }
            close();
        });
    }

    private void createExampleCheckbox() {
        checkboxLabel = new VisLabel("Include Example:");
        checkbox = new VisCheckBox("", true);
        new Tooltip.Builder(languageBundle.get("newproj_dia_tooltip_example")).target(checkboxLabel).build();
    }

    private void checkValid() {
        if (locationValid && namevalid) {
            createBtn.setDisabled(false);
        }
    }

    private void createFolderButton() {
        folderLabel = new VisLabel(languageBundle.get("newproj_dia_proj_root"));
        folderButton = new MetaTextButton(languageBundle.get("newproj_dia_select_folder"));
        folderButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                fileChooser.setSelectionMode(FileChooser.SelectionMode.DIRECTORIES);
                fileChooser.fadeIn();
                fileChooser.setListener(new FileChooserAdapter() {
                    @Override
                    public void selected(Array<FileHandle> file) {
                        if (file.size == 1) {
                            rootfile = file.get(0);
                            folderButton.setText(StringUtil.truncate(file.get(0).pathWithoutExtension(), 30));
                            locationValid = true;
                        } else {
                            locationValid = false;
                        }
                        fileChooser.fadeOut();
                        checkValid();
                    }
                });
                getStage().addActor(fileChooser);
                fileChooser.fadeIn();
            }
        });
        new Tooltip.Builder(languageBundle.get("newproj_dia_tooltip_location")).target(folderLabel).build();
    }

    private void createProjectNameTF() {
        final ProjectWizardDialog projectWizard = this;
        projectNameTF = new MetaValidTextField(languageBundle.get("newproj_dia_name_tf"), statusLabel);
        projectNameTF.addValidator(new MetaInputValidator() {
            @Override
            public void validateInput(String input, MetaErrorHandler errors) {
                if (!StringUtil.isValidFolderName(input)) {
                    errors.add(new MetaError(languageBundle.get("newproj_dia_inalid_name"), "Name can only contain alphanumeric characters") {
                        @Override
                        public void gotoError() {
                        }
                    });
                    namevalid = false;
                }
                namevalid = true;
                checkValid();
            }
        });
        new Tooltip.Builder(languageBundle.get("newproj_dia_tooltip_name")).target(projectNameTF.getDescription()).build();
    }

}
