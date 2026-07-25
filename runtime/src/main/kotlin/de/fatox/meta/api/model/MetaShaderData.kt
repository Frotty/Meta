package de.fatox.meta.api.model

import com.badlogic.gdx.utils.Array

data class MetaShaderData(
	var glShaderData: GLShaderData = GLShaderData(),
	var uniforms: Array<MetaUniformData>,
)
