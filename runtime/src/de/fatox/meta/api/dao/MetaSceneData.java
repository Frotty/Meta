package de.fatox.meta.api.dao;

import com.badlogic.gdx.math.Vector3;

/**
 * Created by Frotty on 15.06.2016.
 */
public class MetaSceneData {
    public String name;
    public String compositionPath;
    public Vector3 cameraPosition = Vector3.Y;
    public boolean showGrid = true;

    public MetaSceneData() {
    }

    public MetaSceneData(String name, String compositionPath) {
        this.name = name;
        this.compositionPath = compositionPath;
    }

}
