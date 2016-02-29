package de.fatox.meta.api.entity;

public interface EntityManager<ENTITY extends Entity> {

    Iterable<ENTITY> getEntities();

    void addEntity(ENTITY entity);

    void removeEntity(ENTITY entity);
}
