package de.fatox.meta.api.dao;

import com.badlogic.gdx.files.FileHandle;
import com.google.gson.annotations.Expose;
import de.fatox.meta.util.StringUtil;

/**
 * Created by Frotty on 02.06.2016.
 */
public class MetaProjectData {
    public static final String PROJECT_FILE_NAME = "metaproject.json";
    @Expose
    public String name;

    public FileHandle root;

    public MetaProjectData(String name, FileHandle root) {
        this.name = name;
        this.root = root;
    }

    public void setRoot(FileHandle root) {
        this.root = root;
    }


    public boolean isValid() {
        return ! StringUtil.isBlank(name);
    }
}
