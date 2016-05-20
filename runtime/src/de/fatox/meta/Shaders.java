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

    private static ShaderProgram dffShader;

    /**
     * Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.
     */
    static public ShaderProgram getDFFShader() {
        if (dffShader == null) {

            String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                    + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
                    + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                    + "uniform mat4 u_projTrans;\n" //
                    + "varying vec4 v_color;\n" //
                    + "varying vec2 v_texCoords;\n" //
                    + "\n" //
                    + "void main()\n" //
                    + "{\n" //
                    + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
                    + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
                    + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
                    + "}\n";
            String fragmentShader = "#ifdef GL_ES\n" //
                    + "#define LOWP lowp\n" //
                    + "precision mediump float;\n" //
                    + "#else\n" //
                    + "#define LOWP \n" //
                    + "#endif\n" //
                    + "varying LOWP vec4 v_color;\n" //
                    + "varying vec2 v_texCoords;\n" //
                    + "uniform sampler2D u_texture;\n" //
                    + "const float smoothing = 0.175;\n" //
                    + "void main()\n"//
                    + "{\n" //
                    + "float distance = texture2D(u_texture, v_texCoords).a;\n" //
                    + "float alpha = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance) * v_color.a;\n" //
                    + "gl_FragColor = vec4(v_color.rgb, alpha);\n" //
                    + "}";

            dffShader = new ShaderProgram(vertexShader, fragmentShader);
            if (!dffShader.isCompiled())
                throw new IllegalArgumentException("Error compiling shader: " + dffShader.getLog());
        }
        return dffShader;
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
