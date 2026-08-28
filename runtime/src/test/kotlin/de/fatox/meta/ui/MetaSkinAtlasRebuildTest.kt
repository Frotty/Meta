package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.test.MetaHeadlessUi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Reclaiming glyph space without disturbing anything drawing from the atlas.
 *
 * Glyphs share the chrome's page and `PixmapPacker` cannot free a region, so every UI-scale change would otherwise
 * leave the previous scale's glyphs resident forever - eventually spilling onto a second page, which reintroduces
 * the flushing the shared atlas exists to remove.
 *
 * The rebuild has to be invisible to everything holding a drawable. Widget styles keep direct references, so the
 * drawable *objects* must survive with their configuration intact and only change what they draw from. A rebuild
 * that replaced them would need a restyle pass across the whole tree; one that dropped their padding would resize
 * every widget silently.
 */
class MetaSkinAtlasRebuildTest {
	@BeforeEach fun setUp() = MetaHeadlessUi.install()
	@AfterEach fun tearDown() = MetaHeadlessUi.dispose()

	@Test
	fun `rebuilding keeps one page and the same drawable objects`() {
		val skin = MetaSkin.skin()
		val button = skin.getDrawable("meta.button.up") as NinePatchDrawable
		val checkbox = skin.getDrawable("meta.checkbox.off")
		val paddingBefore = floatArrayOf(button.leftWidth, button.rightWidth, button.topHeight, button.bottomHeight)
		val minBefore = floatArrayOf(button.minWidth, button.minHeight)
		val patchBefore = button.patch

		MetaSkin.rebuildAtlas()

		assertEquals(1, MetaSkin.atlasPageCount, "The rebuild must land on a single page")
		// Same objects: a widget style holding these must keep working without being restyled.
		assertSame(button, skin.getDrawable("meta.button.up"), "The drawable object was replaced")
		assertSame(checkbox, skin.getDrawable("meta.checkbox.off"), "The drawable object was replaced")
		// Different pixels source: setPatch is what makes the old page droppable.
		assertTrue(patchBefore !== button.patch) {
			"The drawable still points at the old page, so nothing was reclaimed"
		}
		// setPatch copies padding out of the patch, so an unrestored value silently resizes every widget using it.
		assertEquals(paddingBefore[0], button.leftWidth, "leftWidth lost")
		assertEquals(paddingBefore[1], button.rightWidth, "rightWidth lost")
		assertEquals(paddingBefore[2], button.topHeight, "topHeight lost")
		assertEquals(paddingBefore[3], button.bottomHeight, "bottomHeight lost")
		assertEquals(minBefore[0], button.minWidth, "minWidth lost")
		assertEquals(minBefore[1], button.minHeight, "minHeight lost")
	}

	@Test
	fun `drawables reconfigured after installation keep their metrics`() {
		// The menu bar and menu items are packed with no padding and then given their real metrics afterwards by
		// configureToolbarDrawables/configureMenuItemDrawables. Restoring what was recorded at pack time would
		// quietly revert that on the first scale change, so menu geometry would differ before and after - and
		// nothing about the menus would look obviously broken, just wrong.
		val skin = MetaSkin.skin()
		val names = arrayOf("meta.menu.item", "meta.menu.item.over", "meta.menu.bar.over", "meta.menu.bar.selected")
		val before = names.map { name ->
			val d = skin.getDrawable(name)
			floatArrayOf(d.leftWidth, d.rightWidth, d.topHeight, d.bottomHeight, d.minWidth, d.minHeight)
		}
		// Sanity: these must actually differ from the packed defaults, or this test proves nothing.
		assertTrue(before[0][0] > 0f) { "meta.menu.item has no configured padding; this test would be vacuous" }

		MetaSkin.rebuildAtlas()

		names.forEachIndexed { index, name ->
			val d = skin.getDrawable(name)
			val was = before[index]
			assertEquals(was[0], d.leftWidth, "$name leftWidth changed across a rebuild")
			assertEquals(was[1], d.rightWidth, "$name rightWidth changed across a rebuild")
			assertEquals(was[2], d.topHeight, "$name topHeight changed across a rebuild")
			assertEquals(was[3], d.bottomHeight, "$name bottomHeight changed across a rebuild")
			assertEquals(was[4], d.minWidth, "$name minWidth changed across a rebuild")
			assertEquals(was[5], d.minHeight, "$name minHeight changed across a rebuild")
		}
	}

	@Test
	fun `repeated rebuilds with glyphs in between stay on one page`() {
		val fonts = MetaInject.inject<FontProvider>("default")
		// Each pass rasterizes a fresh set of glyph sizes into the shared atlas, as a scale change does. Without a
		// rebuild these accumulate until the page overflows and the batch starts flushing again.
		repeat(12) { pass ->
			for (size in intArrayOf(12, 16, 18, 24, 32)) fonts.getFont(size + pass, FontType.REGULAR)
			MetaSkin.rebuildAtlas()
			assertEquals(1, MetaSkin.atlasPageCount) {
				"Atlas spilled to ${MetaSkin.atlasPageCount} pages after $pass rebuilds; glyphs are not being reclaimed"
			}
		}
		// The chrome must still be intact and usable after all that copying.
		val drawable: Drawable? = MetaSkin.skin().optional("meta.button.up", Drawable::class.java)
		assertNotNull(drawable, "The chrome did not survive repeated rebuilds")
	}
}
