package de.fatox.meta.graphics.font

import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.test.MetaHeadlessUi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

/**
 * When face files are read, and how many.
 *
 * Meta ships four faces and an application typically draws two of them. All four used to be opened in the
 * constructor, so a game supplying its own regular and bold still had `RobotoMono.ttf` and `remixicon.ttf` — 684 KB
 * of TrueType — read and parsed during its startup for faces it never draws.
 *
 * Both properties here are invisible from outside: nothing observably changes when a face is opened, and the
 * regression is silent. Hence [MetaFontProvider.openFaceCount].
 */
internal class MetaFontProviderFaceLoadingTest {

	private lateinit var provider: MetaFontProvider

	@BeforeEach
	fun setUp() {
		MetaHeadlessUi.install()
		// A provider of this test's own, so the count starts where the constructor left it rather than wherever the
		// harness's skin generation left the shared one.
		provider = MetaFontProvider()
	}

	@AfterEach
	fun tearDown() {
		// Disposing twice is deliberate in two of these tests; the second call must be a no-op, not a crash.
		provider.dispose()
		MetaHeadlessUi.dispose()
	}

	@Test
	fun `constructing the provider opens no faces`() {
		assertEquals(0, provider.openFaceCount, "the constructor read font files before anything asked for one")
	}

	@Test
	fun `asking for one face opens one face`() {
		provider.getFont(18, FontType.REGULAR)
		assertEquals(1, provider.openFaceCount, "asking for regular text opened more than the regular face")

		// And again: the second request is served from the open face, not by reopening it.
		provider.getFont(24, FontType.REGULAR)
		assertEquals(1, provider.openFaceCount)
	}

	@Test
	fun `each type opens independently`() {
		provider.getFont(18, FontType.BOLD)
		assertEquals(1, provider.openFaceCount)
		provider.getFont(18, FontType.ICON)
		assertEquals(2, provider.openFaceCount, "the icon face did not open on demand")
	}

	@Test
	fun `preparing faces opens all of them and is repeatable`() {
		provider.prepareFaces()
		assertEquals(FontType.entries.size, provider.openFaceCount, "prepareFaces left a face unopened")

		provider.prepareFaces()
		assertEquals(FontType.entries.size, provider.openFaceCount, "prepareFaces reopened faces it had already read")
	}

	@Test
	fun `a prepared face still measures text`() {
		// The point of preparing off the GL thread is that the face is usable afterwards. A prepare that produced an
		// unusable generator would show up here and nowhere else.
		provider.prepareFaces()
		val font = provider.getFont(24, FontType.REGULAR)
		assertTrue(font.data.getGlyph('M') != null, "a prepared face produced no glyphs")
	}

	@Test
	fun `preparing faces off the main thread produces usable faces`() {
		// The contract SplashScreen relies on: this runs on its preparation worker.
		val worker = Thread { provider.prepareFaces() }
		worker.start()
		worker.join(10_000)
		assertTrue(!worker.isAlive, "prepareFaces did not finish on a worker thread")

		assertEquals(FontType.entries.size, provider.openFaceCount)
		// Rasterizing is still the GL thread's job, and this is it.
		assertTrue(provider.getFont(20, FontType.BOLD).data.getGlyph('W') != null)
	}

	@Test
	fun `a disposed provider opens no more faces`() {
		// The preparation worker outlives the phase that started it, so an application closed during startup can
		// reach prepareFaces after teardown. Storing a face then leaves a FreeType handle in a map nothing will
		// clear again.
		provider.getFont(18, FontType.REGULAR)
		provider.dispose()

		provider.prepareFaces()
		assertEquals(0, provider.openFaceCount, "a disposed provider reopened faces")
	}

	@Test
	fun `disposal releases the faces it opened`() {
		provider.prepareFaces()
		assertEquals(FontType.entries.size, provider.openFaceCount)

		provider.dispose()
		assertEquals(0, provider.openFaceCount, "disposal left faces open")
	}

	@Test
	fun `the dependencies prepareFaces needs are resolved at construction`() {
		// Not lazily. prepareFaces runs on a worker and reads the asset provider and the font info; MetaInject keeps
		// its singletons in unsynchronized maps and hands out NONE-mode lazy delegates, so resolving either of those
		// off-thread can build two of something or publish one half-built. Resolving at construction puts it on
		// whichever thread builds the provider, and SplashScreen makes sure that is the GL thread.
		//
		// Observable only as this: with nothing registered, building one fails here rather than succeeding and
		// failing later on another thread.
		MetaHeadlessUi.dispose()
		MetaInject.global(clear = true) {}
		assertFails { MetaFontProvider() }

		// Restored so the shared teardown has a graph to work with.
		MetaHeadlessUi.install()
	}

	@Test
	fun `the injected provider is the one Meta hands out`() {
		// Guards against this test passing against a provider nothing else uses.
		val injected: FontProvider = MetaInject.inject("default")
		assertTrue(injected is MetaFontProvider, "the graph no longer supplies a MetaFontProvider: $injected")
	}
}
