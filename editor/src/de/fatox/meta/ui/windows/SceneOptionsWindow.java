package de.fatox.meta.ui.windows;

import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.shader.MetaShaderComposer;
import de.fatox.meta.shader.ShaderComposition;

/**
 * Created by Frotty on 20.05.2016.
 */
@Singleton
public class SceneOptionsWindow extends MetaWindow {
    @Inject
    private MetaShaderComposer metaShaderComposer;

    public VisSelectBox<String> rendererSelectBox = new VisSelectBox<String>();

    public SceneOptionsWindow() {
        super("Scene Options", true, true);
        setDefaultSize(120, 180);
        setup();
    }

    private void setup() {
        Array<String> renderers = new Array<>();

        for(ShaderComposition shaderComp : metaShaderComposer.getCompositions()) {
            renderers.add(shaderComp.data.name);
        }

        rendererSelectBox.setItems(renderers);
    }


}
