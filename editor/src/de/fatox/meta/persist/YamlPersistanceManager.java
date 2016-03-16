package de.fatox.meta.persist;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.reflect.*;
import de.fatox.meta.Meta;
import de.fatox.meta.ide.persist.Persist;
import de.fatox.meta.ide.persist.PersistanceManager;
import de.fatox.meta.MetaProject;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Provider;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

public class YamlPersistanceManager implements PersistanceManager {

    @Inject
    private Provider<MetaProject> currentProjectProvider;

    public YamlPersistanceManager(Provider<MetaProject> currentProjectProvider) {
        Meta.inject(this);
    }

    @Override
    public FileHandle injectObject(Object object, int id) {
        Persist annotation = object.getClass().getAnnotation(Persist.class);
        if (annotation != null) {
            String path = "projects/" + currentProjectProvider.get().projectName + "/";
            path += annotation.key() + object.getClass().getSimpleName() + "-" + id + ".yml";
            FileHandle fileHandle = Gdx.files.local(path);
            if (!fileHandle.exists()) {
                try {
                    saveToFile(object, id, fileHandle);
                } catch (ReflectionException e) {
                    e.printStackTrace();
                }
            }
            loadFromFile(object, fileHandle);
            return fileHandle;
        } else {
            throw new GdxRuntimeException("Cannot inject non-annotated objects");
        }
    }


    private void saveToFile(Object object, int id, FileHandle fileHandle) throws ReflectionException {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("className", object.getClass().getSimpleName());
        data.put("id", id);

        Map<String, Object> fieldData = new HashMap<String, Object>();
        for (Field field : ClassReflection.getDeclaredFields(object.getClass())) {
            Annotation annotation = field.getDeclaredAnnotation(Persist.class);
            if (annotation != null) {
                Persist persist = annotation.getAnnotation(Persist.class);
                if (persist != null) {
                    field.setAccessible(true);
                    if (field.get(object) != null) {
                        // Field already has a value
                        fieldData.put(persist.key(), field.get(object));
                        continue;
                    }
                    // Assign default value
                    Class type = field.getType();
                    Method method = ClassReflection.getDeclaredMethod(type, "valueOf", String.class);
                    if (method != null) {
                        fieldData.put(persist.key(), method.invoke(type.getClass(), persist.defaultValue()));
                        continue;
                    }
                    fieldData.put(persist.key(), persist.defaultValue());
                }
            }
        }
        data.put("fields", fieldData);

        Yaml yaml = new Yaml();
        String output = yaml.dump(data);
        System.out.println("out:" + output);
        fileHandle.writeBytes(output.getBytes(), false);
    }

    private void loadFromFile(Object object, FileHandle fileHandle) {
        Yaml yaml = new Yaml();
        String document = fileHandle.readString();
        Map map = (Map) yaml.load(document);
        Map fields = (Map) map.get("fields");
        for (Field field : ClassReflection.getDeclaredFields(object.getClass())) {
            Annotation annotation = field.getDeclaredAnnotation(Persist.class);
            if (annotation != null) {
                Persist persist = annotation.getAnnotation(Persist.class);
                if (persist != null) {
                    if (fields.containsKey(persist.key())) {
                        try {
                            field.setAccessible(true);
                            field.set(object, fields.get(persist.key()));
                        } catch (ReflectionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    @Override
    public void deleteObject(Object object) {

    }

}
