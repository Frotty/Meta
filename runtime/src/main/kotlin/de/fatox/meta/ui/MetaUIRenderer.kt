package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.ToastManager
import com.kotcrab.vis.ui.widget.file.FileChooser
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.extensions.trace
import de.fatox.meta.api.ui.FocusRenderer
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.audioVideoDataKey
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import kotlin.math.abs
import kotlin.math.roundToInt

private val log = MetaLoggerFactory.logger {}

/**
 * A DPI-aware suggested UI scale so controls stay a roughly constant *physical* size across monitors: ~1.0 on a
 * standard ~96 PPI desktop display, larger on 4K/HiDPI. It targets `ppi/96` but divides out the OS content scale
 * libGDX already applies in HdpiMode.Logical (the back-buffer/logical-width ratio), so a Retina Mac gets a modest
 * bump rather than double-scaling. Snapped to 0.25 steps, clamped to a sane range, and falls back to 1.0 when the
 * monitor reports no usable physical size. Use it as the default when the user hasn't picked a UI scale.
 */
fun suggestedUiScale(): Float {
	val g = Gdx.graphics
	val contentScale = if (g.width > 0) g.backBufferWidth.toFloat() / g.width else 1f
	// density ≈ ppi/160 on libGDX desktop, so density/0.6 ≈ ppi/96. <=0 means the monitor reported no size → no scale.
	val ppiScale = if (g.density > 0f) g.density / 0.6f else contentScale
	val raw = if (contentScale > 0f) ppiScale / contentScale else ppiScale
	return ((raw * 4f).roundToInt() / 4f).coerceIn(0.75f, 2.5f)
}

class MetaUIRenderer : UIRenderer {
	private var focusedActor: Actor? = null
	private val metaInput: MetaInputProcessor by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()
	private val visuiSkin: String by lazyInject("visuiSkin")
	private val spriteBatch: SpriteBatch by lazyInject()
	private val metaData: MetaData by lazyInject()
	private val focusRenderer: FocusRenderer by lazyInject()

	private val stage: Stage = Stage(ScreenViewport(), spriteBatch)
	private val audioVideoData = metaData[audioVideoDataKey]
	private val toastManager = MetaToastManager(stage)

	override val uiScale: Signal<Float> = signal(1f) { a, b -> abs(a - b) < 0.001f }

	// The stage's world size already is physical-pixels ÷ unitsPerPixel (= ÷ uiScale) — i.e. UI units.
	override val uiWidth: Float get() = stage.width
	override val uiHeight: Float get() = stage.height

	/** Applies the current [uiScale] to the viewport (fewer UI units per pixel = larger UI) and re-lays-out. */
	private fun applyViewport(width: Int, height: Int) {
		(stage.viewport as ScreenViewport).unitsPerPixel = 1f / uiScale.value.coerceAtLeast(0.25f)
		stage.viewport.update(width, height, true)
	}

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

		// HiDPI: every consumer gets DPI-correct UI by default (no per-game wiring). Re-apply live on any uiScale
		// change (e.g. a settings slider), and seed the default from the display. Games may override uiScale.value
		// afterwards with a user-chosen / persisted value.
		uiScale.subscribe { applyViewport(Gdx.graphics.width, Gdx.graphics.height) }
		uiScale.value = suggestedUiScale()
		applyViewport(Gdx.graphics.width, Gdx.graphics.height)
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
			// Newly added windows/dialogs land on top of the toast layer; lift toasts back above them.
			toastManager.toFront()
		} catch (e: Exception) {
			log.error(e) { "Failed to add actor: $actor!" }
		}
	}

	override fun update() {
		stage.act(Gdx.graphics.deltaTime)
	}

	override fun draw() {
		if (!audioVideoData.runWithUI) return

		stage.batch.setBlendFunction(-1, -1)
		Gdx.gl.glBlendFuncSeparate(
			GL20.GL_SRC_ALPHA,
			GL20.GL_ONE_MINUS_SRC_ALPHA,
			GL20.GL_ONE,
			GL20.GL_ONE_MINUS_SRC_ALPHA
		)

		val deltaTime = Gdx.graphics.deltaTime
		stage.draw()
		focusRenderer.draw(stage, focusedActor, deltaTime)
	}

	override fun resize(width: Int, height: Int) {
		applyViewport(width, height)
		toastManager.resize()
	}

	override fun getCamera(): Camera {
		return stage.camera
	}

	override fun getToastManager(): ToastManager {
		return toastManager
	}

	override fun setFocusedActor(actor: Actor?) {
		focusedActor = actor
	}

	override fun dispose() {
		focusedActor = null
		metaInput.removeGlobalInputProcessor(stage)
		focusRenderer.dispose()
		stage.dispose()
	}
}
