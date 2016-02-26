package de.fatox.meta.api.ide.persist;

import java.lang.reflect.Field;

public interface PersistanceManager {

    void injectField(Field field);

    void deleteField(Field field);
}
