package de.fatox.meta.api.graphics

import com.badlogic.gdx.graphics.g3d.Shader

interface ShaderInfo {
	val isDepth: Boolean

	val renderTargets: Array<RenderTarget>

	val shader: Shader

	val name: String

	class RenderTarget(val name: String)
}
