package de.fatox.meta.api.dao;

/**
 * Created by Frotty on 08.04.2017.
 */
public class RenderBufferData {
    public enum IN {
        GEOMETRY,
        FULLSCREEN
    }
    public IN inType = IN.GEOMETRY;
    public boolean hasDpeth = false;
    public String metaShaderPath;

    public RenderBufferData() {
    }

    public RenderBufferData(String metaShaderPath) {
        this.metaShaderPath = metaShaderPath;
    }

}
