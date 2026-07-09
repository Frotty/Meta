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
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.extensions.trace
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.ui.FocusRenderer
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.audioVideoDataKey
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.components.MetaFileChooser
import de.fatox.meta.ui.components.MetaTooltip
import kotlin.math.abs

private val log = MetaLoggerFactory.logger {}

/**
 * Suggested default UI scale. On desktop (the only target for this library today) libGDX's HdpiMode.Logical
 * already divides out real OS-level HiDPI scaling (Retina, Windows 125/150%, ...) into the back-buffer/logical-
 * width ratio, so no further per-monitor guess is needed here. We deliberately do NOT factor in
 * `Gdx.graphics.density`: on desktop it's derived from the monitor's EDID-reported physical size in millimeters
 * (`glfwGetMonitorPhysicalSize`), which is notoriously unreliable — many monitors, laptop panels, and virtually
 * all VMs/remote-desktop sessions misreport it, which previously inflated the "auto" scale above 100% even on
 * small/low-DPI screens. Use this as the default when the user hasn't picked a UI scale; they can still scale up
 * or down manually via the settings slider.
 */
fun suggestedUiScale(): Float = 1f

class MetaUIRenderer : UIRenderer {
	private var focusedActor: Actor? = null
	private val metaInput: MetaInputProcessor by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()
	private val visuiSkin: String by lazyInject("visuiSkin")
	private val spriteBatch: SpriteBatch by lazyInject()
	private val metaData: MetaData by lazyInject()
	private val focusRenderer: FocusRenderer by lazyInject()
	private val fontProvider: FontProvider by lazyInject()

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
		uiScale.subscribe {
			applyViewport(Gdx.graphics.width, Gdx.graphics.height)
			// Fonts are rasterized per scale: walk the stage so every widget that caches a font re-fetches it from
			// the (now regenerated) provider, then release the orphaned old-scale fonts. Order matters: dispose only
			// after the walk, when nothing on stage references them anymore. Rare event - allocation is acceptable.
			stage.root.refreshFontsRecursively()
			fontProvider.disposeOrphanedFonts()
		}
		uiScale.value = suggestedUiScale()
		applyViewport(Gdx.graphics.width, Gdx.graphics.height)
		val g = Gdx.graphics
		log.debug {
			"UI scale = ${uiScale.value} | logical ${g.width}x${g.height} | backbuffer ${g.backBufferWidth}x" +
				"${g.backBufferHeight} | contentScale ${g.backBufferWidth.toFloat() / g.width} | density ${g.density}"
		}
	}

	private fun loadVisUI() {
		if (visuiSkin != "") {
			VisUI.load(assetProvider.getResource(visuiSkin, FileHandle::class.java))
		} else {
			VisUI.load()
		}
		MetaSkin.install(VisUI.getSkin())
		MetaFileChooser.setDefaultPrefsName("de.fatox.meta")
		VisUI.setDefaultTitleAlign(Align.center)
		log.debug { "Loaded VisUi." }
	}

	override fun addActor(actor: Actor) {
		try {
			stage.addActor(actor)
			// Newly added windows/dialogs land on top of the toast layer; lift toasts back above them.
			toastManager.toFront()
			MetaTooltip.bringVisibleToFront()
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
		MetaTooltip.bringVisibleToFront()
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

	override fun getToastManager(): MetaToastManager {
		return toastManager
	}

	override fun setFocusedActor(actor: Actor?) {
		focusedActor = MetaFocus.assign(focusedActor, actor)
	}

	override fun dispose() {
		setFocusedActor(null)
		metaInput.removeGlobalInputProcessor(stage)
		focusRenderer.dispose()
		stage.dispose()
		fontProvider.dispose()
	}
}
