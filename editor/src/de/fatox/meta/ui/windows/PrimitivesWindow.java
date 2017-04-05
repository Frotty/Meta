package de.fatox.meta.ui.windows;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import de.fatox.meta.Primitives;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.api.graphics.Renderer;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.components.MetaIconTextButton;

/**
 * Created by Frotty on 20.05.2016.
 */
@Singleton
public class PrimitivesWindow extends MetaWindow {
    private final MetaIconTextButton boxButton;

    @Inject
    private EntityManager<Meta3DEntity> entityManager;
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
                    Meta3DEntity entity = new Meta3DEntity(new Vector3(MathUtils.random(-250, 250), MathUtils.random(-5, 50f), MathUtils.random(-250, 250)),
                            primitives.getSphereFilled());
                    entityManager.addEntity(entity);
                    entity.actorModel.transform.rotate(MathUtils.random(0, 1), MathUtils.random(0, 1), MathUtils.random(0, 1), MathUtils.random(0, 360));
                }
                renderer.rebuildCache();
            }
        });

        contentTable.add(boxButton).size(64);
    }


}
