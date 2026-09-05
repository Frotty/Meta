package de.fatox.meta.test

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.GraphicsHandler
import de.fatox.meta.api.MonitorHandler
import de.fatox.meta.api.NoGraphicsHandler
import de.fatox.meta.api.NoMonitorHandler
import de.fatox.meta.api.NoSoundHandler
import de.fatox.meta.api.NoWindowHandler
import de.fatox.meta.api.SoundHandler
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.graphics.font.FontInfo
import de.fatox.meta.graphics.font.MetaFontProvider
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.input.MetaUiInputBindings
import de.fatox.meta.input.MetaUiInputProfiles
import de.fatox.meta.ui.UiControlHelper
import de.fatox.meta.ui.refreshFontsRecursively
import de.fatox.meta.ui.MetaToastManager

/**
 * Builds and measures **real** Meta widget trees in a plain unit test.
 *
 * [GdxTestEnvironment] gets scene2d far enough to lay out plain actors, which is enough to test the layout
 * containers themselves and not enough to test a screen: every widget that carries text needs a font, a font needs a
 * texture, and a texture needs a graphics device. So the only geometry a consumer could check was geometry with the
 * text taken out of it — stand-in actors standing where the labels go, which is exactly the part most likely to be
 * the wrong size.
 *
 * This closes that. [HeadlessGL20] supplies a graphics device that discards every call, and this supplies the object
 * graph Meta's widgets resolve against, so a test can construct a `MetaLabel`, a `MetaFlexBox` full of them, or a
 * whole screen's worth of rows, and assert what it measures:
 *
 * ```kotlin
 * @BeforeEach fun setUp() = MetaHeadlessUi.install()
 * @AfterEach  fun tearDown() = MetaHeadlessUi.dispose()
 *
 * @Test fun `the options rows line up`() {
 *     val row = metaFlexRow { addItem(MetaLabel("Volume", 18)); addItem(MetaLabel("100%", 18)) }
 *     val root = MetaTable().apply { setSize(1920f, 1080f); add(row) }
 *     root.validate()
 *     MetaLayout.assertValid(root)
 * }
 * ```
 *
 * ### Scope
 *
 * Everything up to and including layout is real: real TTF faces through [MetaFontProvider], real generated skin
 * chrome, real text measurement, real flex and table arithmetic, a real `SpriteBatch` and `Stage`, and
 * [de.fatox.meta.ui.layout.MetaLayout] over the result. A screen that owns its stage can therefore be tested as
 * itself instead of rebuilt inside the test.
 *
 * **Measurements are real, pixels are not.** Every draw call is discarded, so a test may lay out, measure and
 * validate a tree, and must never assert on what was rendered. [HeadlessGL20] says which calls it reports as
 * succeeding and why.
 *
 * [uiScale] is writable so a test can check that a scale change re-measures, which is the one piece of responsive
 * behaviour that otherwise only shows up on someone's monitor. Applying it to a tree that already exists needs
 * [refreshFonts].
 *
 * ### It owns the injection graph
 *
 * [install] clears the global graph and [dispose] leaves it empty. That is deliberate — a font provider surviving
 * from a previous test brings that test's cached faces at that test's scale with it — but it means a suite cannot
 * interleave this with a bootstrap of its own that registers singletons **once**. A game whose setup is guarded
 * (`if (installed) return`) will not re-register after a teardown has emptied the graph, and the next test fails with
 * `Unknown class`. The same applies to `MetaModule`, whose registrations run from an object initialiser and therefore
 * exactly once per classloader.
 *
 * So: let the harness own the graph for the tests that use it, and register anything extra inside the same test after
 * [install]. Restoring a caller's previous graph would be better, and needs a snapshot API that `MetaInject` does not
 * expose today.
 */
object MetaHeadlessUi {
	private var previousGdxInput: Input? = null

	/** Disposables a fixture handed over, released in [dispose] in reverse order of acquisition. */
	private val owned = ArrayList<Disposable>()

	/**
	 * Takes ownership of something a fixture created, so a test does not have to keep a reference it was never given.
	 * Used by [toastStage]; safe to call before [install] and idempotent per object.
	 */
	fun own(disposable: Disposable) {
		for (index in owned.indices) if (owned[index] === disposable) return
		owned.add(disposable)
	}


	/**
	 * The UI scale the font provider rasterizes against. Write it to exercise a scale change; widgets re-fetch their
	 * face through [FontProvider.fontGeneration] the same way they do at runtime.
	 */
	val uiScale: Signal<Float> = signal(1f)

	private var installed = false

	/**
	 * Boots the headless application and the GL stub once per JVM, then registers the object graph and initialises
	 * the skin. Call from `@BeforeEach`; pair with [dispose].
	 *
	 * @param installSkinDefaults generate the real skin chrome. On by default because a widget measured against a
	 *   defaults-free skin is measured without its own padding and borders, which is not the size it will have.
	 *   Turn it off for a test that only cares about layout containers.
	 * @param fontProvider the face source, for a test that wants to observe or stand in for it. Cannot be replaced
	 *   after the fact: initializing the skin resolves the provider, and `MetaInject` refuses to re-register a
	 *   singleton it has already built. A decorator over [MetaFontProvider] is the way to record which faces a
	 *   widget tree asks for — which is what a game needs to know if it pre-rasterizes them at startup.
	 */
	@JvmOverloads
	fun install(
		installSkinDefaults: Boolean = true,
		fontProvider: () -> FontProvider = { MetaFontProvider() },
		/**
		 * The input processor to register. Defaults to [LayoutOnlyInput], which dispatches nothing — pass
		 * `{ DispatchingInput() }` to cover behaviour that only happens when input actually arrives.
		 */
		input: () -> MetaInputProcessor = { LayoutOnlyInput() },
		/**
		 * A [UIManager], or `null` to register none as before.
		 *
		 * Null is still the default because most tests do not need one and registering a stub they never call would
		 * only widen what a failure could mean. Pass `{ RecordingUiManager() }` to test a `MetaDialog` subclass, which
		 * cannot be closed or detached without one.
		 */
		uiManager: (() -> UIManager)? = null,
		/**
		 * A toast manager for [LayoutOnlyRenderer], or `null` to keep its documented throw.
		 *
		 * Null by default deliberately: the throw is what stops a test asserting a toast that was never rendered, and
		 * a silent no-op would be worse than no seam at all. A test that *means* to exercise a notification passes one
		 * — `{ MetaToastManager(toastStage()) }` is enough.
		 */
		toastManager: (() -> MetaToastManager)? = null,
	) {
		GdxTestEnvironment.ensure()
		HeadlessGL20.install()
		// UiControlHelper.activateSelectedActor asks `Gdx.input.isKeyPressed`, not MetaInputProcessor, so a fixture
		// tracking modifiers privately would leave ctrl+confirm activating a control production leaves alone. Wire the
		// two together for the lifetime of the install, and restore whatever was there on dispose.
		previousGdxInput = Gdx.input
		// Set before anything that can throw, not after everything succeeded. The
		// first global is already acquired, so from here on teardown has work to do —
		// and if `dispose()` bailed out because a later step failed, the stub and the
		// graph would outlive the test that installed them and change every test
		// after it in the JVM. A failed setup must poison one test, not the run.
		installed = true

		try {
			// Cleared first: a leftover graph from a previous test would keep that
			// test's font provider, and therefore its cached faces at its scale.
			//
			// What follows is everything a Meta widget resolves *at construction time*.
			// Most reach for their font lazily, but a handful resolve eagerly —
			// `MetaSelectBox` takes a UiControlHelper that way — and a missing eager
			// dependency is not a degraded widget, it is a GdxRuntimeException before
			// the tree can be measured at all.
			//
			// The input processor is a stub, not the real MetaInput: that constructor
			// claims `Gdx.input.inputProcessor` and adds a listener to the static
			// Controllers registry, which is process-wide state a layout harness has no
			// business taking. It cannot simply be left out either — UiControlHelper's
			// `init` registers a global processor, so a select box cannot be built
			// without something answering for the interface. See LayoutOnlyInput.
			MetaInject.global(clear = true) {
				// Platform handlers. Engine code resolves these from the graph rather than from `Meta.instance`,
				// so a harness must supply them - `MetaModule`'s own defaults run from an object initialiser and
				// therefore exactly once per classloader, which a cleared graph does not bring back. Registering
				// the No* implementations here is what lets a consuming game test a screen that plays a sound or
				// reads display settings without standing up a whole Meta application.
				singleton<WindowHandler> { NoWindowHandler }
				singleton<MonitorHandler> { NoMonitorHandler }
				singleton<SoundHandler> { NoSoundHandler }
				singleton<GraphicsHandler> { NoGraphicsHandler }
				singleton<AssetProvider> { MetaAssetProvider() }
				singleton("default") { FontInfo() }
				singleton<FontProvider>("default", fontProvider)
				singleton<UIRenderer> { LayoutOnlyRenderer(uiScale, toastManager) }
				singleton<MetaInputProcessor> {
					input().also { processor ->
						if (processor is DispatchingInput) Gdx.input = HeldKeyInput(processor, previousGdxInput)
					}
				}
				// Owned, not merely registered: UIManager is Disposable, and clearing the graph would otherwise drop a
				// manager holding stages or reactive scopes without ever calling dispose on it.
				uiManager?.let { factory -> singleton<UIManager> { factory().also { own(it) } } }
				singleton { MetaUiInputBindings() }
				singleton { MetaUiInputProfiles() }
				singleton("default") { UiControlHelper() }
			}

			MetaSkin.dispose()
			if (installSkinDefaults) {
				MetaSkin.initialize()
			} else {
				MetaSkin.initialize(Skin(), installDefaults = false)
			}
		} catch (failure: Throwable) {
			// Roll back rather than leave half a harness standing, and still fail the
			// test that asked for it.
			runCatching { dispose() }
			throw failure
		}
	}

	/**
	 * Applies a [uiScale] change to a tree that already exists, the way the real renderer does.
	 *
	 * Writing [uiScale] alone regenerates nothing: [MetaFontProvider] re-rasterizes lazily, so widgets built before
	 * the write keep the faces they already hold while widgets built after it get new ones — a split that would make a
	 * scale test quietly meaningless. At runtime `MetaUIRenderer` subscribes to the scale and walks its stage; there
	 * is no renderer here to do that, so a test names the root itself.
	 *
	 * The order is the renderer's and matters for the same reason: refresh the tree first so every widget re-fetches,
	 * *then* release the old faces, which are still referenced until it has.
	 */
	fun refreshFonts(root: Actor) {
		root.refreshFontsRecursively()
		runCatching { MetaInject.inject<FontProvider>("default").disposeOrphanedFonts() }
	}

	/**
	 * Releases everything [install] created, in the order it has to happen. Call from `@AfterEach`.
	 *
	 * The providers are disposed *before* the graph is cleared, because clearing only drops references: a
	 * [MetaFontProvider] owns FreeType generators and cached faces and a [MetaAssetProvider] owns an `AssetManager`,
	 * and none of that is garbage — it is native memory that an install/dispose cycle per test would otherwise leak
	 * once per test. The GL stub goes last, since disposing fonts still calls through it.
	 */
	fun dispose() {
		if (!installed) return
		previousGdxInput?.let { Gdx.input = it }
		previousGdxInput = null
		// Reverse order: a manager built on a stage goes before the stage it draws through.
		for (index in owned.indices.reversed()) runCatching { owned[index].dispose() }
		owned.clear()
		MetaSkin.dispose()
		// Resolved rather than remembered: the graph owns them, and a `runCatching` keeps a teardown from failing
		// over a provider a test never caused to be created.
		runCatching { MetaInject.inject<FontProvider>("default").dispose() }
		runCatching { MetaInject.inject<AssetProvider>().dispose() }
		MetaInject.global(clear = true) {}
		HeadlessGL20.uninstall()
		uiScale.value = 1f
		installed = false
	}
}

/**
 * A [UIRenderer] that owns a UI scale and refuses everything else.
 *
 * Meta's widgets reach the renderer for exactly one thing during layout — the scale their font is rasterized at — so
 * that is all this provides. A test that wants a stage can build one; what it does not get is Meta's own UI layer,
 * whose toast manager and focus renderer exist to draw. Refusing loudly is deliberate: a test that has wandered into
 * drawing should say so rather than quietly get a no-op and then assert against nothing.
 */
private class LayoutOnlyRenderer(
	override val uiScale: Signal<Float>,
	private val toastManager: (() -> MetaToastManager)?,
) : UIRenderer {
	override val uiWidth: Float get() = 1920f
	override val uiHeight: Float get() = 1080f

	override fun load() = Unit
	override fun addActor(actor: Actor) = Unit
	override fun update() = Unit
	override fun draw() = Unit
	override fun resize(width: Int, height: Int) = Unit
	override fun getCamera(): Camera = OrthographicCamera()
	override fun setFocusedActor(actor: Actor?) = Unit

	/**
	 * Throws unless a test supplied one, which is the point rather than an omission: a silently absent toast layer
	 * would let a test assert a notification that was never rendered and still pass.
	 */
	/** Memoized: production hands back one manager, and a fresh one per call would give every toast its own list. */
	private var resolvedToastManager: MetaToastManager? = null

	override fun getToastManager(): MetaToastManager =
		resolvedToastManager
			?: toastManager?.invoke()?.also { resolvedToastManager = it }
			?: throw UnsupportedOperationException(
			"MetaHeadlessUi provides no UI layer of its own, so there is no toast manager. Pass " +
				"`toastManager = { MetaToastManager(toastStage()) }` to install(), or build a Stage yourself; see " +
				"HeadlessGL20 for what drawing does and does not do here.",
		)
}

/**
 * Answers `isKeyPressed` from a [DispatchingInput]'s held keys and delegates everything else.
 *
 * Needed because two different questions are asked about the keyboard: `MetaInputProcessor` is *told* about presses,
 * while `UiControlHelper.activateSelectedActor` *asks* `Gdx.input` whether a modifier is down. A fixture that answers
 * only the first lets a test activate a control while ctrl is held, which production refuses.
 */
private class HeldKeyInput(
	private val source: DispatchingInput,
	private val delegate: Input?,
) : Input by (delegate ?: MockInput()) {
	override fun isKeyPressed(key: Int): Boolean =
		if (key == Input.Keys.ANY_KEY) source.isAnyKeyHeld() else source.isKeyHeld(key)
}
