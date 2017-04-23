package de.fatox.meta.api.dao;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Created by Frotty on 10.03.2017.
 */
public class MetaData {
    public static final String GLOBAL_DATA_FOLDER_NAME = "./.meta/";

    private ObjectMap<String, FileHandle> fileHandleCache = new ObjectMap<>();
    private FileHandle globalRoot = Gdx.files.absolute(GLOBAL_DATA_FOLDER_NAME);
    private Json json = new Json();

    public void save(String key, Object obj) {
        save(globalRoot, key, obj);
    }

    public FileHandle save(FileHandle target, String key, Object obj) {
        String jsonString = json.toJson(obj);

        FileHandle fileHandle = getCachedHandle(target, key);

        fileHandle.writeBytes(jsonString.getBytes(), false);
        return fileHandle;
    }

    public <T> T get(String key, Class<T> type) {
        return get(globalRoot, key, type);
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
