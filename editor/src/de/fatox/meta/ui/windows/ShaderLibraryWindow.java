package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.*;
import de.fatox.meta.api.graphics.GLShaderHandle;
import de.fatox.meta.assets.MetaAssetProvider;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.components.AssetSelectButton;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaTextButton;

/**
 * Created by Frotty on 28.06.2016.
 */
public class ShaderLibraryWindow extends MetaWindow {
    private final VisTable visTable;
    private VisScrollPane scrollPane;

    @Inject
    private MetaAssetProvider assetProvider;

    public ShaderLibraryWindow() {
        super("Shader Library", true, true);
        setSize(240, 320);
        createToolbar();
        setPosition(1200, 328);

        visTable = new VisTable();
        visTable.top();
        visTable.defaults().pad(4);
        scrollPane = new VisScrollPane(visTable);

        add(scrollPane).top().grow();
    }

    public void addShader(GLShaderHandle shader) {
        MetaTextButton metaTextButton = new MetaTextButton("");
        VisTable visTable = new VisTable();
        visTable.add(new VisLabel("Shader: " + shader.getName())).growX();
        visTable.row();
        visTable.add(new AssetSelectButton("").getTable());
        metaTextButton.getLabelCell().setActor(visTable);
    }

    private void createToolbar() {
        VisImageButton visImageButton = new VisImageButton(assetProvider.getDrawable("ui/appbar.page.add.png"));
        visImageButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
//                new ShaderWizardDialog().show(getStage());
            }
        });


        top();
        add(visImageButton).size(24).top().left();
        row().height(1);
        add(new Separator()).growX();
        row();
        visImageButton.getImage().setScaling(Scaling.fill);
        visImageButton.getImage().setSize(24, 24);
    }
}
