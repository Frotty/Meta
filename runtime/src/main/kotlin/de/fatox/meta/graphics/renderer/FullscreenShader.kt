package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public abstract class FullscreenShader implements Shader {
	public abstract ShaderProgram getProgram();

	@Override
	public void render(Renderable renderable) {
	}
}
