package de.fatox.meta.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.debug
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.extensions.trace
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.ui.FocusRenderer
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.components.MetaFileChooser
import de.fatox.meta.ui.components.MetaTooltip
import de.fatox.meta.ui.components.nearestScrollableMetaScrollPane
import de.fatox.meta.ui.components.scrollDebugPath
import de.fatox.meta.ui.components.scrollDebugState
import de.fatox.meta.ui.components.updateMetaScrollFocus
import kotlin.math.abs

private val log = MetaLoggerFactory.logger {}

/**
 * Suggested default UI scale. OS scaling is already represented by the back-buffer/logical-size ratio, so Meta
 * never applies a second scale in that case. For an unscaled 4K/5K desktop it accepts a larger default only when
 * resolution and a sane EDID density agree. Ambiguous displays remain at 100%; users can always override this.
 */
fun suggestedUiScale(): Float = suggestedUiScale(
	logicalWidth = Gdx.graphics.width,
	logicalHeight = Gdx.graphics.height,
	backBufferWidth = Gdx.graphics.backBufferWidth,
	backBufferHeight = Gdx.graphics.backBufferHeight,
	density = Gdx.graphics.density,
)

internal fun suggestedUiScale(
	logicalWidth: Int,
	logicalHeight: Int,
	backBufferWidth: Int,
	backBufferHeight: Int,
	density: Float,
): Float {
	if (logicalWidth <= 0 || logicalHeight <= 0 || backBufferWidth <= 0 || backBufferHeight <= 0) return 1f
	val contentScaleX = backBufferWidth.toFloat() / logicalWidth
	val contentScaleY = backBufferHeight.toFloat() / logicalHeight
	// OS scaling already makes logical pixels larger. Applying Meta scaling too would double-scale the UI.
	if (contentScaleX > 1.1f || contentScaleY > 1.1f) return 1f
	// Resolution or EDID density alone is ambiguous; require both, and reject implausible EDID values.
	if (density !in 1.4f..4f) return 1f
	return when {
		backBufferWidth >= 5120 && backBufferHeight >= 2880 && density >= 2f -> 1.5f
		backBufferWidth >= 3840 && backBufferHeight >= 2160 -> 1.25f
		else -> 1f
	}
}

class MetaUIRenderer : UIRenderer {
	private var focusedActor: Actor? = null
	private val metaInput: MetaInputProcessor by lazyInject()
	private val spriteBatch: SpriteBatch by lazyInject()
	private val focusRenderer: FocusRenderer by lazyInject()
	private val fontProvider: FontProvider by lazyInject()

	private val stage: Stage = Stage(ScreenViewport(), spriteBatch)
	private val toastManager = MetaToastManager(stage)
	private val reactiveScope = ReactiveScope()
	private var loaded = false
	private var disposed = false
	private var lastScrollHoverTarget: Actor? = null
	private var lastScrollHoverPane: Actor? = null
	private var lastReportedScrollFocus: Actor? = null

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
		check(!disposed) { "A disposed UI renderer cannot be loaded again" }
		if (loaded) return
		loaded = true
		val runWithUI = MetaAudioVideoState.state.value.runWithUI
		log.trace { "load with UI enabled = $runWithUI" }
		if (runWithUI) {
			loadMetaUI()
		}

		stage.root.addCaptureListener(object : InputListener() {
			override fun mouseMoved(event: InputEvent, x: Float, y: Float): Boolean {
				val target = event.target
				val pane = target.nearestScrollableMetaScrollPane()
				val focusBefore = stage.scrollFocus
				updateMetaScrollFocus(stage, event.target)
				val focusAfter = stage.scrollFocus
				if (target !== lastScrollHoverTarget || pane !== lastScrollHoverPane ||
					focusAfter !== lastReportedScrollFocus
				) {
					log.debug {
						"scroll-hover target=${target.scrollDebugPath()} pane=${pane?.scrollDebugState() ?: "-"} " +
							"focus=${focusBefore.scrollDebugPath()}->${focusAfter.scrollDebugPath()}"
					}
					lastScrollHoverTarget = target
					lastScrollHoverPane = pane
					lastReportedScrollFocus = focusAfter
				}
				return false
			}

			override fun scrolled(
				event: InputEvent,
				x: Float,
				y: Float,
				amountX: Float,
				amountY: Float,
			): Boolean {
				val hit = stage.hit(event.stageX, event.stageY, true)
				val pane = hit.nearestScrollableMetaScrollPane()
				log.debug {
					"scroll-wheel hit=${hit.scrollDebugPath()} routed=${event.target.scrollDebugPath()} " +
						"pane=${pane?.scrollDebugState() ?: "-"} focus=${stage.scrollFocus.scrollDebugPath()} " +
						"dx=$amountX dy=$amountY"
				}
				return false
			}

			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
				if (!event.target.isInside<TextField>()) stage.keyboardFocus = null
				if (!event.target.isInside<ScrollPane>()) stage.scrollFocus = null
				return false
			}
		})
		metaInput.addGlobalInputProcessor(stage)

		// HiDPI: every consumer gets DPI-correct UI by default (no per-game wiring). Re-apply live on any uiScale
		// change (e.g. a settings slider), and seed the default from the display. Games may override uiScale.value
		// afterwards with a user-chosen / persisted value.
		reactiveScope.subscribe(uiScale) {
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

	private inline fun <reified T : Actor> Actor?.isInside(): Boolean {
		var current = this
		while (current != null) {
			if (current is T) return true
			current = current.parent
		}
		return false
	}

	override fun cancelTouchFocus() = stage.cancelTouchFocus()

	private fun loadMetaUI() {
		MetaSkin.initialize()
		MetaFileChooser.setDefaultPrefsName("de.fatox.meta")
		log.debug { "Loaded Meta UI." }
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
		if (!MetaAudioVideoState.state.value.runWithUI) return

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
		if (disposed) return
		disposed = true
		setFocusedActor(null)
		reactiveScope.dispose()
		if (loaded) metaInput.removeGlobalInputProcessor(stage)
		focusRenderer.dispose()
		stage.dispose()
		fontProvider.dispose()
		MetaSkin.dispose()
	}
}
