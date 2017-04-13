package de.fatox.meta.api.dao;

import com.badlogic.gdx.utils.Array;

/**
 * Created by Frotty on 08.04.2017.
 */
public class MetaRenderData {
    public String name;
    public Array<RenderBufferData> renderBuffers;

    public MetaRenderData() {
    }

    public MetaRenderData(String name) {
        this.name = name;
        renderBuffers = new Array<>();
    }


}
