package de.fatox.meta.api

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import de.fatox.meta.api.extensions.use
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

class SplashScreen(private val cb: () -> Unit) : ScreenAdapter() {
	private val spriteBatch: SpriteBatch by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()

	private val sprite: Sprite by lazy(LazyThreadSafetyMode.NONE) {
		Sprite(Texture(Gdx.files.internal("textures/meta_logo2.png")))
	}
	private var f = 0f

	override fun show() {
		val width = Gdx.graphics.width
		val height = Gdx.graphics.height
		sprite.setPosition(width / 2 - sprite.width / 2, height / 2 - sprite.height / 2)
	}

	override fun dispose() {
		sprite.texture.dispose()
	}

	override fun render(delta: Float) {
		Gdx.gl.apply {
			glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
			glClearColor(0f, 0f, 0f, 0f)
			glClear(GL20.GL_COLOR_BUFFER_BIT)
		}

		spriteBatch.use {
			sprite.draw(spriteBatch)
		}

		if (f++ >= 1f) {
			f = -99999999f
			cb.invoke()
			uiRenderer.load()
		}
	}
}
