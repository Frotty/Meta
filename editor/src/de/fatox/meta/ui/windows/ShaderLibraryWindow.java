package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.*;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.graphics.GLShaderHandle;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.components.AssetSelectButton;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaTextButton;
import de.fatox.meta.ui.dialogs.ShaderWizardDialog;

/**
 * Created by Frotty on 28.06.2016.
 */
@Singleton
public class ShaderLibraryWindow extends MetaWindow {
    private final VisTable visTable;
    private VisScrollPane scrollPane;

    @Inject
    private AssetProvider assetProvider;

    public ShaderLibraryWindow() {
        super("Shader Library", true, true);
        setSize(240, 320);
        createToolbar();
        setPosition(1200, 328);

        visTable = new VisTable();
        visTable.top();
        visTable.defaults().pad(4);
        scrollPane = new VisScrollPane(visTable);

        contentTable.add(scrollPane).top().grow();
    }

    public void addShader(GLShaderHandle shader) {
        MetaTextButton metaTextButton = new MetaTextButton("", 12);
        VisTable visTable = new VisTable();
        visTable.add(new VisLabel("Shader: " + shader.getName())).growX();
        visTable.row();
        visTable.add(new AssetSelectButton("").getTable());
    }

    private void createToolbar() {
        VisImageButton visImageButton = new VisImageButton(assetProvider.getDrawable("ui/appbar.page.add.png"));
        visImageButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.showDialog(ShaderWizardDialog.class);
            }
        });

        contentTable.row().size(26);
        contentTable.add(visImageButton).size(24).top().left();
        contentTable.row().height(1);
        contentTable.add(new Separator()).growX();
        contentTable.row();
        visImageButton.getImage().setScaling(Scaling.fill);
        visImageButton.getImage().setSize(24, 24);
    }
}
