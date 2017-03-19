package de.fatox.meta.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.injection.Inject;

public class MetaEntityManager implements EntityManager<Meta3DEntity> {
    private Array<Meta3DEntity> entities = new Array<>();

    @Inject
    private AssetProvider assetProvider;

    public MetaEntityManager() {
        Meta.inject(this);
        ModelBuilder modelBuilder = new ModelBuilder();
        final Material material = new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY));
        final long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.ColorUnpacked;

        Model xyzCoordinates = modelBuilder.createXYZCoordinates(24, new Material(), attributes);
        Model grid = modelBuilder.createLineGrid(64,64, 32, 32, material, attributes);
        addEntity(new Meta3DEntity(new Vector3(0, 0, 0), grid));
        addEntity(new Meta3DEntity(new Vector3(0, 0, 0), xyzCoordinates));
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
