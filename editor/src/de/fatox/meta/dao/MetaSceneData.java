package de.fatox.meta.dao;

import com.google.gson.annotations.Expose;

/**
 * Created by Frotty on 15.06.2016.
 */
public class MetaSceneData {
    @Expose
    private String name;
    @Expose
    private MetaEntityData[] entityDatas;

    public MetaSceneData(String name) {
        this.name = name;
    }

    public MetaEntityData[] getEntityDatas() {
        return entityDatas;
    }

    public void setEntityDatas(MetaEntityData[] entityDatas) {
        this.entityDatas = entityDatas;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
