package de.fatox.meta.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.Meta
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.Inject

class SplashScreen(private val cb: () -> Unit) : ScreenAdapter() {
    @Inject
    private lateinit var spriteBatch: SpriteBatch
    @Inject
    private lateinit var uiRenderer: UIRenderer
    private var sprite: Sprite? = null
	private var sprite2: Sprite? = null
    private var f = 0f

    override fun show() {
        Meta.inject(this)
        val internal = Gdx.files.internal("textures/meta_logo.png")
		val internal2 = Gdx.files.internal("textures/spinner.png")
        sprite = Sprite(Texture(internal))
		val texture = Texture(internal2)
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
		sprite2 = Sprite(texture)
        val width = Gdx.graphics.width
        val height = Gdx.graphics.height

        sprite?.setPosition(width / 2 - sprite!!.width / 2, height / 2 - sprite!!.height / 2)
		sprite2?.setPosition(width / 2 - sprite2!!.width / 2, height / 2 - sprite2!!.height / 2)
    }

    override fun render(delta: Float) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.146f, 0.146f, 0.147f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
		sprite2?.rotation = sprite2?.rotation?.plus(delta * 128)!!
        spriteBatch.begin()
        sprite?.draw(spriteBatch)
		sprite2?.draw(spriteBatch)
        spriteBatch.end()
        if (f >= 1f) {
            f = -99999999f
            cb.invoke()
            uiRenderer.load()
        }
        f += 1
    }


}
