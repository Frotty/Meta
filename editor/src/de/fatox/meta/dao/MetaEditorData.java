package de.fatox.meta.dao;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 26.06.2016.
 */
public class MetaEditorData {
    public static final String DATA_FILE_NAME = "metadata.json";

    public Array<String> lastProjectFiles = new Array<>();
    @Expose
    private String[] lastProjects;

    @Inject
    private Gson gson;
    private FileHandle fileHandle;

    public MetaEditorData() {
        Meta.inject(this);
    }

    public void write() {
        fileHandle.writeBytes(gson.toJson(this).getBytes(),false);
    }

    public void addLastProject(String s) {
        if(! lastProjectFiles.contains(s, false)) {
            lastProjectFiles.add(s);
        }
        lastProjects = lastProjectFiles.toArray(String.class);
        write();
    }

    public void setFileHandle(FileHandle fileHandle) {
        this.fileHandle = fileHandle;
    }

    public Array<String> getLastProjectFiles() {
        if(lastProjectFiles.size == 0) {
            lastProjectFiles.addAll(lastProjects);
        }
        return lastProjectFiles;
    }
}