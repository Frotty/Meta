package de.fatox.meta.ui.windows;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import de.fatox.meta.Primitives;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.MetaEditorUI;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaIconTextButton;
import de.fatox.meta.ui.tabs.SceneTab;

/**
 * Created by Frotty on 20.05.2016.
 */
@Singleton
public class PrimitivesWindow extends MetaWindow {
    private final MetaIconTextButton boxButton;

    @Inject
    private MetaEditorUI metaEditorUI;
    @Inject
    private Primitives primitives;
    @Inject
    private Renderer renderer;


    public PrimitivesWindow() {
        super("Primitives", true, true);
        this.boxButton = new MetaIconTextButton("Box", assetProvider.getDrawable("ui/appbar.box.png"));
        boxButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                for (int i = 0; i < 100; i++) {
                    Meta3DEntity entity = new Meta3DEntity(new Vector3(MathUtils.random(-10, 10), MathUtils.random(-0.5f, 5.0f), MathUtils.random(-10, 10)),
                            primitives.getSphereFilled());

                    Tab currentTab = metaEditorUI.getCurrentTab();
                    if(currentTab instanceof SceneTab) {
                        SceneTab sceneTab = (SceneTab) currentTab;
                        sceneTab.getSceneHandle().entityManager.addStaticEntity(entity);
                    }
                    entity.actorModel.transform.scl(4);
                    entity.actorModel.transform.rotate(MathUtils.random(0f, 1f), MathUtils.random(0f, 1f), MathUtils.random(0f, 1f), MathUtils.random(0, 360));
                    entity.actorModel.materials.get(0).set(ColorAttribute.createDiffuse(MathUtils.random(0, 1f), MathUtils.random(0f, 1f), MathUtils.random(0f, 1f), 1));
                    entity.actorModel.materials.get(0).set(TextureAttribute.createNormal(assetProvider.get("models/crates_n.png", Texture.class)));
                }
                renderer.rebuildCache();
            }
        });

        contentTable.add(boxButton).size(64);
    }


}
