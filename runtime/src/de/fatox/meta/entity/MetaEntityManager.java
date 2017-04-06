package de.fatox.meta.entity;

import com.badlogic.gdx.utils.Array;
import de.fatox.meta.api.entity.EntityManager;

public class MetaEntityManager implements EntityManager<Meta3DEntity> {
    private Array<Meta3DEntity> entities = new Array<>();
    private Array<Meta3DEntity> staticEntities = new Array<>();

    public MetaEntityManager() {

    }

    @Override
    public Iterable<Meta3DEntity> getEntities() {
        return entities;
    }

    @Override
    public Iterable<Meta3DEntity> getStaticEntities() {
        return staticEntities;
    }

    @Override
    public void addEntity(Meta3DEntity entity) {
        entities.add(entity);
    }

    @Override
    public void addStaticEntity(Meta3DEntity entity) {
        staticEntities.add(entity);
    }

    @Override
    public void removeEntity(Meta3DEntity entity) {

    }

    @Override
    public void clear() {
        entities.clear();
        staticEntities.clear();
    }
}
