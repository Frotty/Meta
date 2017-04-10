package de.fatox.meta.ui.windows;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import de.fatox.meta.api.dao.RenderBufferData;
import de.fatox.meta.api.graphics.ShaderLibrary;
import de.fatox.meta.graphics.renderer.ShaderComposer;
import de.fatox.meta.graphics.renderer.ShaderComposition;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.dialogs.ShaderCompositionWizard;

/**
 * Created by Frotty on 29.07.2016.
 */
@Singleton
public class ShaderComposerWindow extends MetaWindow {
    @Inject
    private ShaderLibrary shaderLibrary;
    @Inject
    private ShaderComposer shaderComposer;

    private VisSelectBox<String> renderSelectbox;
    private VisTable bufferTable;
    private VisImageButton addButton;

    public ShaderComposerWindow() {
        super("Shader Composer", true, true);
        setupEmpty();
    }

    private void setupEmpty() {
        VisImageButton visImageButton = new VisImageButton(assetProvider.getDrawable("ui/appbar.page.add.png"));
        visImageButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiManager.showDialog(ShaderCompositionWizard.class);
            }
        });
        visImageButton.getImage().setScaling(Scaling.fill);
        visImageButton.getImage().setSize(24, 24);

        bufferTable = new VisTable();
        bufferTable.top().left();

        renderSelectbox = new VisSelectBox<>();
        renderSelectbox.setItems(new Array<>());
        renderSelectbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

            }
        });
        contentTable.left();
        contentTable.add(visImageButton).size(24).top().left().padRight(2);
        contentTable.add(renderSelectbox).width(256).left();
        contentTable.add().growX();
        contentTable.row().padTop(2);
        contentTable.add(bufferTable).colspan(3).grow();
    }

    private void setupNewBufferButton() {
        addButton = new VisImageButton(assetProvider.getDrawable("ui/appbar.layer.add.png"));
        addButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                RenderBufferData renderBufferData = new RenderBufferData();
                shaderComposer.getComposition(renderSelectbox.getSelected()).data.renderBuffers.add(renderBufferData);
            }
        });
        addButton.getImage().setAlign(Align.center);
        bufferTable.add(addButton).size(150, 100).left();
    }

    public void addComposition(ShaderComposition shaderComposition) {
        if(shaderComposition.data.name != null) {
            Array<String> items = new Array<>(renderSelectbox.getItems());
            items.add(shaderComposition.data.name);
            renderSelectbox.setItems(items);
            renderSelectbox.setSelected(shaderComposition.data.name);

            loadComposition(shaderComposition);
        }
    }

    private void loadComposition(ShaderComposition shaderComposition) {
        if(shaderComposition.data.renderBuffers.size == 0) {
            // Newly created composition
            setupNewBufferButton();
        } else {
            // Load existing
        }
    }
}
