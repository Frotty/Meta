package de.fatox.meta.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Named;

/**
 * Created by Frotty on 10.03.2017.
 * Handles MetaData needs.
 * De-/serializes config classes from a .meta sub folder in the game's data folder inside user home
 * MetaData can be accessed via #.get and will be cached.
 */
public class MetaData {
    static class CacheObj<T> {
        T obj;
        long created = TimeUtils.millis();

        public CacheObj(T obj) {
            this.obj = obj;
        }
    }

    @Inject
	@Named("gameName")
	private String gameName;

    public static final String GLOBAL_DATA_FOLDER_NAME = ".meta";

    private final ObjectMap<String, FileHandle> fileHandleCache = new ObjectMap<>();
    private final ObjectMap<String, CacheObj<? extends Object>> jsonCache = new ObjectMap<>();
    private final FileHandle dataRoot;

	public MetaData() {
		Meta.inject(this);
		dataRoot = Gdx.files.external("." + gameName).child(GLOBAL_DATA_FOLDER_NAME);
		dataRoot.mkdirs();
	}

    private final Json json = new Json();

    public void save(String key, Object obj) {
        save(dataRoot, key, obj);
    }

    public FileHandle save(FileHandle target, String key, Object obj) {
        String jsonString = json.toJson(obj);

        FileHandle fileHandle = getCachedHandle(target, key);
        fileHandle.writeBytes(jsonString.getBytes(), false);
        CacheObj cacheObj = jsonCache.get(key);
        if (cacheObj != null) {
            cacheObj.created = TimeUtils.millis();
            cacheObj.obj = obj;
        }
        return fileHandle;
    }

    /** Loads and caches the filehandle descripted by the path, if it exists */
    public FileHandle get(String key) {
        return getCachedHandle(dataRoot, key);
    }

    /** Caches and returns this object loaded from json at the default location */
    public <T> T get(Class<T> type) {
        return getCachedJson(dataRoot, type.getClass().getSimpleName(), type);
    }

    /** Caches and returns this object loaded from json at the specified location */
    public <T> T get(String key, Class<T> type) {
        return getCachedJson(dataRoot, key, type);
    }

    private <T> T getCachedJson(FileHandle parent, String key, Class<T> type) {
        T jsonHandle;
        if (jsonCache.containsKey(key)) {
            CacheObj<T> cacheObj = (CacheObj<T>) jsonCache.get(key);
            long lastModified = getCachedHandle(parent, key).lastModified();
            if (cacheObj.created < lastModified) {
                cacheObj.obj = json.fromJson(type, getCachedHandle(parent, key));
                cacheObj.created = lastModified;
            }
            jsonHandle = cacheObj.obj;
        } else {
            FileHandle cachedHandle = getCachedHandle(parent, key);
            if (!cachedHandle.exists()) {
                try {
                    cachedHandle.writeBytes(json.toJson(ClassReflection.newInstance(type)).getBytes(), false);
                } catch (ReflectionException e) {
                    e.printStackTrace();
                }
            }
            jsonHandle = json.fromJson(type, cachedHandle);
            jsonCache.put(key, new CacheObj<>(jsonHandle));
        }
        return jsonHandle;
    }

    public <T> T get(FileHandle target, String key, Class<T> type) {
        FileHandle fileHandle = getCachedHandle(target, key);
        if (fileHandle != null && fileHandle.exists()) {
            return json.fromJson(type, fileHandle.readString());
        }
        return null;
    }

    public FileHandle getCachedHandle(String key) {
        return getCachedHandle(dataRoot, key);
    }

    public FileHandle getCachedHandle(FileHandle parent, String key) {
        FileHandle fileHandle;
        if (fileHandleCache.containsKey(key)) {
            fileHandle = fileHandleCache.get(key);
        } else {
            fileHandle = parent.child(key);
            if (!fileHandle.exists()) {
                FileHandle fileHandle2 = Gdx.files.external(GLOBAL_DATA_FOLDER_NAME + key);
                if (fileHandle2.exists()) {
                    fileHandle = fileHandle2;
                }
            }
            fileHandleCache.put(key, fileHandle);
        }
        return fileHandle;
    }

    public boolean has(String name) {
        return has(dataRoot, name);
    }

    public boolean has(FileHandle fileHandle, String name) {
        return fileHandleCache.containsKey(name) || fileHandle.child(name).exists();
    }
}
