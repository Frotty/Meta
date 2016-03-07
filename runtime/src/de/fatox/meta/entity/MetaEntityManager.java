package de.fatox.meta.entity;

import com.badlogic.gdx.math.Vector3;
import de.fatox.meta.api.entity.Entity;
import de.fatox.meta.api.entity.EntityManager;

public class MetaEntityManager implements EntityManager<Entity<Vector3>> {
    @Override
    public Iterable<Entity<Vector3>> getEntities() {
        return null;
    }

    @Override
    public void addEntity(Entity<Vector3> entity) {

    }

    @Override
    public void removeEntity(Entity<Vector3> entity) {

    }
}
