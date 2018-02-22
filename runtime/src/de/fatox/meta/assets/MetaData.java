package de.fatox.meta.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * Created by Frotty on 10.03.2017.
 * Handles MetaData needs.
 * Basically de-/serializes config classes from a .meta folder next to the executable
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

    public static final String GLOBAL_DATA_FOLDER_NAME = "./.meta/";

    private ObjectMap<String, FileHandle> fileHandleCache = new ObjectMap<>();
    private ObjectMap<String, CacheObj<? extends Object>> jsonCache = new ObjectMap<>();
    private FileHandle globalRoot = Gdx.files.absolute(GLOBAL_DATA_FOLDER_NAME);
    private Json json = new Json();

    public void save(String key, Object obj) {
        save(globalRoot, key, obj);
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
        return getCachedHandle(globalRoot, key);
    }

    /** Caches and returns this object loaded from json at the default location */
    public <T> T get(Class<T> type) {
        return getCachedJson(globalRoot, type.getClass().getSimpleName(), type);
    }

    /** Caches and returns this object loaded from json at the specified location */
    public <T> T get(String key, Class<T> type) {
        return getCachedJson(globalRoot, key, type);
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
            jsonHandle = json.fromJson(type, getCachedHandle(parent, key));
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
        return getCachedHandle(globalRoot, key);
    }

    public FileHandle getCachedHandle(FileHandle parent, String key) {
        FileHandle fileHandle;
        if (fileHandleCache.containsKey(key)) {
            fileHandle = fileHandleCache.get(key);
        } else {
            fileHandle = parent.child(key);
            fileHandleCache.put(key, fileHandle);
        }
        return fileHandle;
    }

    public boolean has(String name) {
        return has(globalRoot, name);
    }

    public boolean has(FileHandle fileHandle, String name) {
        return fileHandleCache.containsKey(name) || fileHandle.child(name).exists();
    }
}
