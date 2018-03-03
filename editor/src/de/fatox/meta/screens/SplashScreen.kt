package de.fatox.meta.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.Meta
import de.fatox.meta.injection.Inject

class SplashScreen(private val cb: () -> Unit) : ScreenAdapter() {
    @Inject
    private val spriteBatch: SpriteBatch? = null
    private var sprite: Sprite? = null
    private var f = 0f

    override fun show() {
        Meta.inject(this)
        val internal = Gdx.files.internal("textures/meta_logo.png")
        sprite = Sprite(Texture(internal))
        val width = Gdx.graphics.width
        val height = Gdx.graphics.height

        sprite!!.setPosition(width / 2 - sprite!!.width / 2, height / 2 - sprite!!.height / 2)
    }

    override fun render(delta: Float) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.146f, 0.146f, 0.147f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        spriteBatch!!.begin()
        sprite!!.draw(spriteBatch)
        spriteBatch.end()
        if (f >= 1) {
            cb.invoke()
            f = -999999999999f
        }
        f += delta
    }


}
