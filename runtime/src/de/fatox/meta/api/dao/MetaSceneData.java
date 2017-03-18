package de.fatox.meta.api.dao;

import com.google.gson.annotations.Expose;

/**
 * Created by Frotty on 15.06.2016.
 */
public class MetaSceneData {
    @Expose
    private String name;

    public MetaSceneData() {
    }

    public MetaSceneData(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
