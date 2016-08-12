package de.fatox.meta.ui.windows;

import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.ui.components.MetaTextButton;

/**
 * Created by Frotty on 29.07.2016.
 */
public class ShaderPipelineWindow extends MetaWindow {
    @Inject
    private ShaderLibrary shaderLibrary;
    private VisTable visTable;


    public ShaderPipelineWindow() {
        super("Shader Pipeline", true, true);

        setup();
    }

    private void setup() {
        visTable = new VisTable();
        visTable.row();
        visTable.add(new MetaTextButton("Select Shader")).size(200);

        shaderLibrary.getActiveShaders();
    }
}
