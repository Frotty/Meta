package de.fatox.meta.api.ide.persist;

public interface PersistentValue<VALUETYPE> {
    VALUETYPE get();

    void set(VALUETYPE value);
}
