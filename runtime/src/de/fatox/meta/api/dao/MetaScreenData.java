package de.fatox.meta.api.dao;

import com.google.gson.annotations.Expose;

/**
 * Created by Frotty on 28.06.2016.
 */
public class MetaScreenData {
    @Expose
    public String name;

    @Expose
    public ExposedArray<MetaWindowData> windowData = new ExposedArray<>(4);

    public MetaScreenData(String screenName) {
        this.name = screenName;
    }

}
