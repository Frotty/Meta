package de.fatox.meta.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.MetaAssetProvider;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.injection.Inject;

public class MetaEntityManager implements EntityManager<Meta3DEntity> {
    private Array<Meta3DEntity> entities = new Array<>();

    @Inject
    private MetaAssetProvider assetProvider;

    public MetaEntityManager() {
        Meta.inject(this);
        Pixmap pxmp = new Pixmap(32, 32, Pixmap.Format.RGB888);
        pxmp.setColor(Color.RED);
        pxmp.drawPixel(0, 0);
        Texture whiteTex = new Texture(pxmp);
        ModelBuilder modelBuilder = new ModelBuilder();
        Model model = assetProvider.get("models/cryofan.g3db", Model.class);
//        model.materials.get(0).set(ColorAttribute.createDiffuse(Color.CORAL));
        addEntity(new Meta3DEntity(new Vector3(0, 0, 0), model) {
            @Override
            public int getId() {
                return 0;
            }

            @Override
            public Vector3 getCenter() {
                return new Vector3(0, 0, 0);
            }

            @Override
            public void update() {

            }

            @Override
            public void draw() {

            }
        });
    }

    @Override
    public Iterable<Meta3DEntity> getEntities() {
        return entities;
    }

    @Override
    public void addEntity(Meta3DEntity entity) {
        entities.add(entity);
    }

    @Override
    public void removeEntity(Meta3DEntity entity) {

    }
}
