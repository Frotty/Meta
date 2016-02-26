package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public interface FullscreenShader extends Shader {
	public abstract ShaderProgram getProgram();

}
