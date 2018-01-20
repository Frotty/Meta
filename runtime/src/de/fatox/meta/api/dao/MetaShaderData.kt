package de.fatox.meta.api.dao

import com.badlogic.gdx.utils.Array

/**
 * Created by Frotty on 18.07.2016.
 */
data class MetaShaderData(var glShaderData: GLShaderData = GLShaderData(),
                          var uniforms: Array<MetaUniformData>) {

}
