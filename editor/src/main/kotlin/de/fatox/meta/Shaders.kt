package de.fatox.meta

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShaderProgram

object Shaders {
    /**
     * Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.
     */
    var normalShader: ShaderProgram? = null
        get() {
            if (field == null) {
                ShaderProgram.pedantic = false
                val vertexShader = Gdx.files.internal("shaders/spritebatch.vert").readString()
                val fragmentShader = Gdx.files.internal("shaders/spritebatch.frag").readString()
                field = ShaderProgram(vertexShader, fragmentShader)
                require(field!!.isCompiled) { "Error compiling shader: " + field!!.log }
            }
            return field
        }
        private set
    private var fxaaShader: ShaderProgram? = null

    /**
     * Returns a new instance of the default shader used by SpriteBatch for GL2 when no shader is specified.
     */
    val fXAAShader: ShaderProgram?
        get() {
            if (fxaaShader == null) {
                ShaderProgram.pedantic = false
                val vertexShader = Gdx.files.internal("shaders/fxaa.vert").readString()
                val fragmentShader = Gdx.files.internal("shaders/fxaa.frag").readString()
                fxaaShader = ShaderProgram(vertexShader, fragmentShader)
                require(fxaaShader!!.isCompiled) { "Error compiling shader: " + fxaaShader!!.log }
            }
            return fxaaShader
        }
    private var bgShader: ShaderProgram? = null
    val startMenuBgShader: ShaderProgram?
        get() {
            if (bgShader == null) {
                ShaderProgram.pedantic = false
                val vertexShader = Gdx.files.internal("shaders/startmenu.vert").readString()
                val fragmentShader = Gdx.files.internal("shaders/startmenu.frag").readString()
                bgShader = ShaderProgram(vertexShader, fragmentShader)
                require(bgShader!!.isCompiled) { "Error compiling shader: " + bgShader!!.log }
            }
            return bgShader
        }
}