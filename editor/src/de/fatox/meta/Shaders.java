package de.fatox.meta;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class Shaders {

    private static ShaderProgram normalShader;

    /**
     * Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.
     */
    static public ShaderProgram getNormalShader() {
        if (normalShader == null) {
            String vertexShader = Gdx.files.internal("shaders/spritebatch.vert").readString();
            String fragmentShader = Gdx.files.internal("shaders/spritebatch.frag").readString();

            normalShader = new ShaderProgram(vertexShader, fragmentShader);
            ShaderProgram.pedantic = true;
            if (!normalShader.isCompiled())
                throw new IllegalArgumentException("Error compiling shader: " + normalShader.getLog());

        }
        return normalShader;
    }

    private static ShaderProgram fxaaShader;

    /**
     * Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.
     */
    static public ShaderProgram getFXAAShader() {
        if (fxaaShader == null) {
            String vertexShader = Gdx.files.internal("shaders/fxaa.vert").readString();
            String fragmentShader = Gdx.files.internal("shaders/fxaa.frag").readString();

            fxaaShader = new ShaderProgram(vertexShader, fragmentShader);
            ShaderProgram.pedantic = true;
            if (!fxaaShader.isCompiled())
                throw new IllegalArgumentException("Error compiling shader: " + fxaaShader.getLog());

        }
        return fxaaShader;
    }


    private static ShaderProgram bgShader;

    public static ShaderProgram getStartMenuBgShader() {
        if (bgShader == null) {
            String vertexShader = Gdx.files.internal("shaders/startmenu.vert").readString();
            String fragmentShader = Gdx.files.internal("shaders/startmenu.frag").readString();

            bgShader = new ShaderProgram(vertexShader, fragmentShader);
            ShaderProgram.pedantic = true;
            if (!bgShader.isCompiled())
                throw new IllegalArgumentException("Error compiling shader: " + bgShader.getLog());

        }
        return bgShader;
    }

}
