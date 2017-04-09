package de.fatox.meta.ui.windows;

import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;

/**
 * Created by Frotty on 29.07.2016.
 */
@Singleton
public class RenderBufferWindow extends MetaWindow {
    @Inject
    private ShaderLibrary shaderLibrary;
    private VisTable visTable;


    public RenderBufferWindow() {
        super("Render Buffers", true, true);

        setup();
    }

    private void setup() {
        VisImageButton addButton = new VisImageButton(assetProvider.getDrawable("ui/appbar.layer.add.png"));
        addButton.getImage().setAlign(Align.center);
        contentTable.left();
        contentTable.row().padTop(6);
        contentTable.add(addButton).size(150, 100).left();
    }
}
