package de.fatox.meta.api

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import de.fatox.meta.Meta
import de.fatox.meta.api.extensions.use
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaColor

class SplashScreen(private val cb: () -> Unit) : ScreenAdapter() {
	private val spriteBatch: SpriteBatch by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()

	private val sprite: Sprite by lazy(LazyThreadSafetyMode.NONE) {
		Sprite(Texture(Gdx.files.internal("textures/meta_logo2.png")))
	}
	private var transitionStarted = false

	override fun show() {
		centerSprite()
	}

	override fun dispose() {
		sprite.texture.dispose()
	}

	override fun render(delta: Float) {
		Gdx.gl.apply {
			// Window dimensions are logical points on Retina/HiDPI displays; HdpiUtils maps them to framebuffer pixels.
			HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
			glClearColor(MetaColor.BACKGROUND.r, MetaColor.BACKGROUND.g, MetaColor.BACKGROUND.b, 1f)
			glClear(GL20.GL_COLOR_BUFFER_BIT)
		}

		spriteBatch.use {
			sprite.draw(spriteBatch)
		}

		if (!transitionStarted && Meta.canChangeScreen()) {
			transitionStarted = true
			cb.invoke()
			uiRenderer.load()
		}
	}

	override fun resize(width: Int, height: Int) {
		centerSprite()
	}

	private fun centerSprite() {
		val width = Gdx.graphics.width
		val height = Gdx.graphics.height
		sprite.setPosition(width / 2 - sprite.width / 2, height / 2 - sprite.height / 2)
	}
}
