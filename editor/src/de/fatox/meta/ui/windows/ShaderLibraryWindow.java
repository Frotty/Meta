package de.fatox.meta.ui.windows;

import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.graphics.GLShaderHandle;
import de.fatox.meta.api.graphics.MetaGLShader;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.shader.MetaShaderLibrary;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaLabel;
import de.fatox.meta.ui.components.MetaTextButton;
import de.fatox.meta.ui.dialogs.ShaderWizardDialog;

/**
 * Created by Frotty on 28.06.2016.
 */
@Singleton
public class ShaderLibraryWindow extends MetaWindow {
    @Inject
    private AssetProvider assetProvider;
    @Inject
    private MetaShaderLibrary shaderLibrary;

    private final VisTable visTable;
    private VisScrollPane scrollPane;

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
        for(Shader shader : shaderLibrary.getActiveShaders()) {
            MetaGLShader metaGLShader = (MetaGLShader) shader;
            addShader(metaGLShader.shaderHandle);
        }
    }

    public void addShader(GLShaderHandle shader) {
        MetaTextButton metaTextButton = new MetaTextButton(shader.data.name + ".msh", 16);
        metaTextButton.row();
        metaTextButton.add(new MetaLabel(shader.getVertexHandle().name() + "/" + shader.getFragmentHandle().name(), 14));
        metaTextButton.row();
        metaTextButton.add(new MetaLabel("Targets: " + shader.targets.size, 14));

        visTable.add(metaTextButton).growX();
        visTable.row();
    }

    private void createToolbar() {
        VisImageButton visImageButton = new VisImageButton(assetProvider.getDrawable("ui/appbar.page.add.png"));
        visImageButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.showDialog(ShaderWizardDialog.class);
            }
        });
        visImageButton.getImage().setScaling(Scaling.fill);
        visImageButton.getImage().setSize(24, 24);

        contentTable.row().size(26);
        contentTable.add(visImageButton).size(24).top().left();
        contentTable.row().height(1);
        contentTable.add(new Separator()).growX();
        contentTable.row();

    }
}
