package de.fatox.meta.api.dao;

/**
 * Created by Frotty on 18.07.2016.
 */
public class GLShaderData {
    public String name;
    public String vertexFilePath;
    public String fragmentFilePath;

    public GLShaderData() {
    }

    public GLShaderData(String name, String vertPath, String fragPath) {
        this.name = name;
        this.vertexFilePath = vertPath;
        this.fragmentFilePath = fragPath;
    }
}
