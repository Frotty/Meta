package de.fatox.meta.api.dao;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by Frotty on 15.06.2016.
 */
public class MetaSceneData {
    public String name;
    public Vector3 cameraPosition;

    public MetaSceneData() {
    }

    public MetaSceneData(String name) {
        this.name = name;
    }

}
