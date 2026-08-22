package de.fatox.meta.test

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.Actor
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.graphics.font.FontInfo
import de.fatox.meta.graphics.font.MetaFontProvider
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaSkin
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
 * chrome, real text measurement, real flex and table arithmetic, and [de.fatox.meta.ui.layout.MetaLayout] over the
 * result. Drawing is not, and neither is anything needing a `Stage` — see [HeadlessGL20] for why that is a refusal
 * rather than an omission.
 *
 * [uiScale] is writable so a test can check that a scale change re-measures, which is the one piece of responsive
 * behaviour that otherwise only shows up on someone's monitor.
 */
object MetaHeadlessUi {

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
	 */
	@JvmOverloads
	fun install(installSkinDefaults: Boolean = true) {
		GdxTestEnvironment.ensure()
		HeadlessGL20.install()

		// Cleared first: a leftover graph from a previous test would keep that test's
		// font provider, and therefore its cached faces at its scale.
		MetaInject.global(clear = true) {
			singleton<AssetProvider> { MetaAssetProvider() }
			singleton("default") { FontInfo() }
			singleton<FontProvider>("default") { MetaFontProvider() }
			singleton<UIRenderer> { LayoutOnlyRenderer(uiScale) }
		}

		MetaSkin.dispose()
		if (installSkinDefaults) MetaSkin.initialize() else MetaSkin.initialize(com.badlogic.gdx.scenes.scene2d.ui.Skin(), installDefaults = false)
		installed = true
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
 * Meta's widgets reach the renderer for exactly one thing during layout — the scale their font is rasterized at — and
 * the real one owns a stage, a toast manager and a focus renderer that all need a graphics device. Refusing the rest
 * loudly is deliberate: a test that has wandered into drawing should say so, not quietly get a no-op.
 */
private class LayoutOnlyRenderer(override val uiScale: Signal<Float>) : UIRenderer {
	override val uiWidth: Float get() = 1920f
	override val uiHeight: Float get() = 1080f

	override fun load() = Unit
	override fun addActor(actor: Actor) = Unit
	override fun update() = Unit
	override fun draw() = Unit
	override fun resize(width: Int, height: Int) = Unit
	override fun getCamera(): Camera = OrthographicCamera()
	override fun setFocusedActor(actor: Actor?) = Unit

	override fun getToastManager(): MetaToastManager =
		throw UnsupportedOperationException("MetaHeadlessUi has no stage, so no toast manager; see HeadlessGL20")
}
