package de.fatox.meta.graphics.renderer

import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.glutils.ShaderProgram

abstract class FullscreenShader : Shader {
	abstract val program: ShaderProgram?
	override fun render(renderable: Renderable) {}
}