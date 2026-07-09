package de.fatox.meta.ui.components

import de.fatox.meta.test.GdxTestEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class MetaIconsTest {
	@BeforeTest
	fun setUp() {
		GdxTestEnvironment.ensure()
	}

	@Test
	fun `remix icon index exposes names and glyphs`() {
		val info = assertNotNull(MetaIcons.info("ri-information-line"))

		assertEquals("ri-information-line", info.name)
		assertEquals("System", info.category)
		assertEquals(MetaIcons.glyph("information-line"), MetaIcons.glyph("ri-information-line"))
		assertTrue(MetaIcons.exists("ri-add-line"))
		assertTrue(MetaIcons.names().contains("ri-folder-open-line", false))
		assertTrue(MetaIcons.search("folder-open").contains(MetaIcons.info("ri-folder-open-line")!!, false))
	}
}
