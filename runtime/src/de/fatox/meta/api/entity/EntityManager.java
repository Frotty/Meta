package de.fatox.meta.api.entity;

public interface EntityManager<ENTITY extends Entity> {

    Iterable<ENTITY> getEntities();

    Iterable<ENTITY> getStaticEntities();

    void addEntity(ENTITY entity);

    void addStaticEntity(ENTITY entity);

    void removeEntity(ENTITY entity);

    void clear();
}
