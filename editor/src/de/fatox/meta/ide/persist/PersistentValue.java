package de.fatox.meta.ide.persist;

public interface PersistentValue<VALUETYPE> {
    VALUETYPE get();

    void set(VALUETYPE value);
}
