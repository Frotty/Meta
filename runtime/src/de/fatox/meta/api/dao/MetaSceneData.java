package de.fatox.meta.api.dao;

/**
 * Created by Frotty on 15.06.2016.
 */
public class MetaSceneData {
    public String name;

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
