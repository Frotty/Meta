package de.fatox.meta.api.dao;

import com.google.gson.annotations.Expose;

/**
 * Created by Frotty on 18.07.2016.
 */
public class MetaShaderData {
    @Expose
    private GLShaderData glShaderData;
    @Expose
    private MetaUniformData[] uniforms;

}
