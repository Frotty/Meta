package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisImageButton;
import de.fatox.meta.MetaAssetProvider;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.dialogs.ShaderWizardDialog;

/**
 * Created by Frotty on 28.06.2016.
 */
public class ShaderLibraryWindow extends MetaWindow {
    @Inject
    private MetaAssetProvider assetProvider;
    public ShaderLibraryWindow() {
        super("Shader Library", true, true);
        setSize(240, 320);
        createToolbar();
        setPosition(1200, 328);
    }

    private void createToolbar() {
        VisImageButton visImageButton = new VisImageButton(assetProvider.getDrawable("ui/appbar.page.add.png"));
        visImageButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                new ShaderWizardDialog().show(getStage());
            }
        });


        top();
        add(visImageButton).size(24).top().left();
        row().height(1);
        add(new Separator()).growX();
        row();
        visImageButton.getImage().setScaling(Scaling.fill);
        visImageButton.getImage().setSize(24,24);
    }
}
