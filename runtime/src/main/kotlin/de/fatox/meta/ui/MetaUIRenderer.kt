package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.file.FileChooser
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.extensions.trace
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.audioVideoDataKey
import de.fatox.meta.injection.MetaInject.Companion.lazyInject


private val log = MetaLoggerFactory.logger {}

class MetaUIRenderer : UIRenderer {
	private var focusedActor: Actor? = null
	private val metaInput: MetaInputProcessor by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()
	private val visuiSkin: String by lazyInject("visuiSkin")
	private val spriteBatch: SpriteBatch by lazyInject()
	private val metaData: MetaData by lazyInject()
	private val shapeRenderer: ShapeRenderer = ShapeRenderer()

	private val stage: Stage = Stage(ScreenViewport(),  spriteBatch)
	private val audioVideoData = metaData[audioVideoDataKey]

	init {
		log.debug { "Injected MetaUi." }
	}

	override fun load() {
		log.trace { "load with UI enabled = ${audioVideoData.runWithUI}" }
		if (audioVideoData.runWithUI) {
			loadVisUI()
		}

		stage.root.addCaptureListener(object : InputListener() {
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
				if (!(event.target is TextField || event.target is ScrollPane)) stage.scrollFocus = null
				return false
			}
		})
		metaInput.addGlobalInputProcessor(stage)
	}

	private fun loadVisUI() {
		if (visuiSkin != "") {
			VisUI.load(assetProvider.getResource(visuiSkin, FileHandle::class.java))
		} else {
			VisUI.load()
		}
		FileChooser.setDefaultPrefsName("de.fatox.meta")
		VisUI.setDefaultTitleAlign(Align.center)
		log.debug { "Loaded VisUi." }
	}

	override fun addActor(actor: Actor) {
		try {
			stage.addActor(actor)
		} catch (e: Exception) {
			log.error(e) { "Failed to add actor: $actor!" }
		}
	}

	override fun update() {
		stage.act(Gdx.graphics.deltaTime)
	}

	override fun draw() {
		if (!audioVideoData.runWithUI) return

		stage.batch.setBlendFunction(-1, -1);
		Gdx.gl.glBlendFuncSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

		stage.draw()
		focusedActor?.let {
			shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
			shapeRenderer.projectionMatrix = stage.batch.projectionMatrix
			shapeRenderer.transformMatrix = stage.batch.transformMatrix
			val coordinates = it.localToStageCoordinates(Vector2(0f, 0f))
			shapeRenderer.color = Color.valueOf("256bdb")
			shapeRenderer.rect(coordinates.x, coordinates.y, it.width, it.height)
			shapeRenderer.end()
		}
	}

	override fun resize(width: Int, height: Int) {
		stage.viewport.update(width, height, true)
	}

	override fun getCamera(): Camera {
		return stage.camera
	}

	override fun setFocusedActor(actor: Actor) {
		focusedActor = actor
	}
}
