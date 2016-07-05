package de.fatox.meta.api.graphics;

import com.badlogic.gdx.files.FileHandle;

/**
 * Created by Frotty on 02.07.2016.
 */
public class GLShaderHandle {
    private String name;
    private FileHandle vertexHandle;
    private FileHandle fragmentHandle;

    public GLShaderHandle(String name, FileHandle vertexHandle, FileHandle fragmentHandle) {
        this.name = name;
        this.vertexHandle = vertexHandle;
        this.fragmentHandle = fragmentHandle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileHandle getVertexHandle() {
        return vertexHandle;
    }

    public void setVertexHandle(FileHandle vertexHandle) {
        this.vertexHandle = vertexHandle;
    }

    public FileHandle getFragmentHandle() {
        return fragmentHandle;
    }

    public void setFragmentHandle(FileHandle fragmentHandle) {
        this.fragmentHandle = fragmentHandle;
    }
}
