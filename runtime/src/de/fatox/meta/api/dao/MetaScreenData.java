package de.fatox.meta.api.dao;

import com.badlogic.gdx.utils.Array;

/**
 * Created by Frotty on 28.06.2016.
 */
public class MetaScreenData {
    public String name;

    public Array<MetaWindowData> windowData = new Array<>(4);

    public MetaScreenData(String screenName) {
        this.name = screenName;
    }

}
