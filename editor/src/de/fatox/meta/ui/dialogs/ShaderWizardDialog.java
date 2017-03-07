package de.fatox.meta.ui.dialogs;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.api.graphics.GLShaderHandle;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.error.MetaError;
import de.fatox.meta.error.MetaErrorHandler;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.components.AssetSelectButton;
import de.fatox.meta.ui.components.MetaInputValidator;
import de.fatox.meta.ui.components.MetaValidTextField;
import de.fatox.meta.ui.windows.MetaDialog;
import de.fatox.meta.util.StringUtil;

/**
 * Created by Frotty on 29.06.2016.
 */
@Singleton
public class ShaderWizardDialog extends MetaDialog {
    private final VisTextButton cancelBtn;
    private final VisTextButton createBtn;
    @Inject
    private ShaderLibrary shaderLibrary;

    private MetaValidTextField shaderNameTF;
    private ButtonGroup<VisCheckBox> renderTargetGroup = new ButtonGroup<>();
    private AssetSelectButton vertexSelect;
    private AssetSelectButton fragmentSelect;

    public ShaderWizardDialog() {
        super("Shader Wizard", true);

        cancelBtn = addButton(new VisTextButton("Cancel"), Align.left, false);
        createBtn = addButton(new VisTextButton("Create"), Align.right, true);
        shaderNameTF = new MetaValidTextField("Shader name:", statusLabel);
        shaderNameTF.addValidator(new MetaInputValidator() {
            @Override
            public void validateInput(String input, MetaErrorHandler errors) {
                if (StringUtil.isBlank(input)){
                    errors.add(new MetaError("Invalid Shader name", "") {
                        @Override
                        public void gotoError() {

                        }
                    });
                } else {
                    checkButton();
                }
            }
        });

        renderTargetGroup.setMaxCheckCount(1);
        renderTargetGroup.setMinCheckCount(1);
        createBtn.setDisabled(true);
        setDefaultSize(300, 450);
        setupTable();
    }


    private void checkButton() {
        if(! StringUtil.isBlank(shaderNameTF.getTextField().getMessageText()) && vertexSelect.hasFile() && fragmentSelect.hasFile()) {
            createBtn.setDisabled(false);
        } else {
            createBtn.setDisabled(true);
        }
    }

    private void setupTable() {
        VisTable visTable = new VisTable();
        visTable.defaults().pad(4);
        visTable.add(shaderNameTF.getDescription()).growX();
        visTable.add(shaderNameTF.getTextField()).growX();
        visTable.row();

        VisLabel visLabel = new VisLabel("Render Target:");
        visLabel.setAlignment(Align.center);
        visTable.add(visLabel).colspan(2).pad(4);
        visTable.row();

        VisCheckBox geometryButton = new VisCheckBox("Geometry", true);
        VisCheckBox fullscreenButton = new VisCheckBox("Fullscreen", false);

        visTable.add(geometryButton);
        visTable.add(fullscreenButton);
        visTable.row();

        VisLabel visLabel2 = new VisLabel("Shader Files:");
        visLabel2.setAlignment(Align.center);
        visTable.add(visLabel2).colspan(2).pad(4);
        visTable.row();


        vertexSelect = new AssetSelectButton("Vertex Shader");
        vertexSelect.setSelectListener((file) -> checkButton());
        visTable.add(vertexSelect.getTable()).colspan(2).growX();
        visTable.row();

        fragmentSelect = new AssetSelectButton("Fragment Shader");
        fragmentSelect.setSelectListener((file) -> checkButton());
        visTable.add(fragmentSelect.getTable()).colspan(2).growX();
        visTable.row();

        renderTargetGroup.add(geometryButton);
        renderTargetGroup.add(fullscreenButton);

        contentTable.add(visTable).top().growX();

        setDialogListener((Object object) -> {
            if((boolean)object) {
                shaderLibrary.addShader(new GLShaderHandle(shaderNameTF.getTextField().getText(), vertexSelect.getFile(), fragmentSelect.getFile()));
            }
            close();
        });
    }

}
