package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import de.fatox.meta.test.GdxTestEnvironment
import de.fatox.meta.ui.components.MetaFlexDirection
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MetaUiDefaultsTest {
	@BeforeEach
	fun setUp() {
		GdxTestEnvironment.ensure()
		MetaSkin.dispose()
		MetaSkin.initialize(Skin(), installDefaults = false)
	}

	@AfterEach
	fun tearDown() {
		MetaSkin.dispose()
	}

	@Test
	fun `content layout helpers apply the standard spacing rhythm`() {
		val row = metaRow { add(Actor()) }
		val column = metaColumn { add(Actor()) }

		assertEquals(MetaSpacing.SM, row.cells.first().spaceRight)
		assertEquals(MetaSpacing.SM, column.cells.first().spaceBottom)
	}

	@Test
	fun `responsive layout builders expose ergonomic framework defaults`() {
		val row = metaFlexRow(wrap = true) { addItem(Actor(), basisWidth = 20f, basisHeight = 10f) }
		val column = metaFlexColumn { addItem(Actor(), basisWidth = 20f, basisHeight = 10f) }
		val grid = metaGrid(minColumnWidth = 80f, maxColumns = 3) { addItem(Actor(), basisHeight = 10f) }
		val stack = metaStack { addItem(Actor(), basisWidth = 20f, basisHeight = 10f) }

		assertEquals(MetaFlexDirection.ROW, row.direction)
		assertTrue(row.wrap)
		assertEquals(MetaSpacing.SM, row.mainGap)
		assertEquals(MetaFlexDirection.COLUMN, column.direction)
		assertEquals(3, grid.maxColumns)
		assertEquals(20f, stack.prefWidth)
	}

	@Test
	fun `theme text colors meet WCAG AA contrast`() {
		assertContrastAtLeast(MetaColor.TEXT, MetaColor.PRIMARY, 4.5f)
		assertContrastAtLeast(MetaColor.TEXT, MetaColor.PRIMARY_HOVER, 4.5f)
		assertContrastAtLeast(MetaColor.TEXT, MetaColor.SECONDARY, 4.5f)
		assertContrastAtLeast(MetaColor.TEXT, MetaColor.TERTIARY, 4.5f)
		assertContrastAtLeast(MetaColor.TEXT_MUTED, MetaColor.SURFACE_RAISED, 4.5f)
		assertContrastAtLeast(MetaColor.TEXT_MUTED, MetaColor.BACKGROUND, 4.5f)
	}

	private fun assertContrastAtLeast(foreground: com.badlogic.gdx.graphics.Color, background: com.badlogic.gdx.graphics.Color, minimum: Float) {
		val lighter = maxOf(luminance(foreground), luminance(background))
		val darker = minOf(luminance(foreground), luminance(background))
		val ratio = (lighter + 0.05f) / (darker + 0.05f)
		assertTrue(ratio >= minimum, "Expected contrast >= $minimum, got $ratio")
	}

	private fun luminance(color: com.badlogic.gdx.graphics.Color): Float =
		0.2126f * linear(color.r) + 0.7152f * linear(color.g) + 0.0722f * linear(color.b)

	private fun linear(channel: Float): Float = if (channel <= 0.04045f) {
		channel / 12.92f
	} else {
		Math.pow(((channel + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
	}
}
