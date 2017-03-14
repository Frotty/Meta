package de.fatox.meta.api.dao;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Created by Frotty on 10.03.2017.
 */
public class MetaData2 {
    public static final String DATA_FOLDER_NAME = "./.meta/";

    private ObjectMap<String, FileHandle> fileHandleCache = new ObjectMap<>();
    private FileHandle root = Gdx.files.absolute(DATA_FOLDER_NAME);
    private Json json = new Json();

    public void save(String key, Object obj) {
        String jsonString = json.toJson(obj);

        FileHandle fileHandle = getCachedRootHandle(key);

        fileHandle.writeBytes(jsonString.getBytes(), false);
    }

    public <T> T get(String key, Class<T> type) {
        FileHandle fileHandle = getCachedRootHandle(key);
        if(fileHandle != null && fileHandle.exists()) {
            return json.fromJson(type, fileHandle.readString());
        }
        return null;
    }

    public FileHandle getCachedRootHandle(String key) {
        FileHandle fileHandle;
        if(fileHandleCache.containsKey(key)) {
            fileHandle = fileHandleCache.get(key);
        } else {
            fileHandle = root.child(key);
            fileHandleCache.put(key, fileHandle);
        }
        return fileHandle;
    }

    public boolean has(String name) {
        return fileHandleCache.containsKey(name) || root.child(name).exists();
    }
}
