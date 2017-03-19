package de.fatox.meta.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
        Pixmap pxmp = new Pixmap(1, 1, Pixmap.Format.RGB888);
        pxmp.setColor(Color.LIGHT_GRAY);
        pxmp.drawPixel(0, 0);
        Texture whiteTex = new Texture(pxmp);
        ModelBuilder modelBuilder = new ModelBuilder();
        Model model = assetProvider.get("models/cryofan.g3db", Model.class);
        final Material material = new Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY));
        final Material material2 = new Material();
        final long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.ColorUnpacked;
        Model box2 = modelBuilder.createXYZCoordinates(32, material2, VertexAttributes.Usage.Position| VertexAttributes.Usage.ColorUnpacked);
        Model box = modelBuilder.createLineGrid(64,64, 32, 32, material, attributes);
        addEntity(new Meta3DEntity(new Vector3(0, 0, 0), box));
        addEntity(new Meta3DEntity(new Vector3(0, 0, 0), box2));
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
