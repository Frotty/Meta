package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import de.fatox.meta.ui.layout.MetaLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class MetaGridTest {
	@Test
	fun `auto-fit tracks collapse as assigned width narrows`() {
		val items = List(5) { Actor() }
		val grid = MetaGrid(minColumnWidth = 100f, maxColumns = 3, columnGap = 8f, rowGap = 8f)
		for (item in items) grid.addItem(item, basisHeight = 20f)

		grid.setSize(328f, 48f)
		grid.layout()
		assertEquals(3, grid.resolvedColumns())
		assertEquals(104f, items[0].width)
		assertEquals(112f, items[1].x)
		assertEquals(48f, grid.prefHeight)

		grid.setWidth(208f)
		assertEquals(2, grid.resolvedColumns())
		assertEquals(76f, grid.prefHeight)
	}

	@Test
	fun `column spans participate in automatic row placement`() {
		val wide = Actor()
		val next = Actor()
		val grid = MetaGrid(minColumnWidth = 100f, maxColumns = 3, columnGap = 8f, rowGap = 4f)
		grid.addItem(wide, columnSpan = 2, basisHeight = 20f)
		grid.addItem(next, columnSpan = 2, basisHeight = 20f)
		grid.setSize(328f, 44f)
		grid.layout()

		assertEquals(216f, wide.width)
		assertEquals(0f, next.x)
		assertEquals(0f, next.y)
	}

	@Test
	fun `child mutation updates responsive preferred height`() {
		val first = Actor()
		val second = Actor()
		val grid = MetaGrid(minColumnWidth = 100f, maxColumns = 1, rowGap = 5f)
		grid.setWidth(100f)
		grid.addItem(first, basisHeight = 20f).addItem(second, basisHeight = 30f)
		assertEquals(55f, grid.prefHeight)

		grid.removeActor(first)
		assertEquals(30f, grid.prefHeight)
		grid.clearChildren()
		assertEquals(0f, grid.prefHeight)
	}

	@Test
	fun `layout children are remeasured when track width changes`() {
		val first = WidthResponsiveWidget()
		val second = WidthResponsiveWidget()
		val grid = MetaGrid(minColumnWidth = 100f, maxColumns = 2, columnGap = 8f)
		grid.addItem(first).addItem(second)

		grid.setSize(300f, 20f)
		assertEquals(20f, grid.prefHeight)
		grid.layout()
		MetaLayout.assertValid(grid)

		grid.setSize(240f, 40f)
		assertEquals(40f, grid.prefHeight)
		grid.layout()
		MetaLayout.assertValid(grid)
	}

	@Test
	fun `plain actor natural width survives stretched layouts`() {
		val actor = Actor().apply { setSize(30f, 20f) }
		val grid = MetaGrid(minColumnWidth = 100f, horizontalAlign = MetaFlexAlign.STRETCH)
		grid.addItem(actor)
		grid.setSize(100f, 20f)
		grid.layout()
		assertEquals(100f, actor.width)

		grid.horizontalAlign = MetaFlexAlign.END
		grid.layout()
		assertEquals(30f, actor.width)
		assertEquals(70f, actor.x)
	}

	@Test
	fun `non-stretched layout children measure height at their compact width`() {
		val actor = WidthResponsiveWidget()
		val grid = MetaGrid(minColumnWidth = 200f, horizontalAlign = MetaFlexAlign.END)
		grid.addItem(actor)
		grid.setSize(200f, 40f)

		assertEquals(40f, grid.prefHeight)
		grid.layout()
		assertEquals(100f, actor.width)
		assertEquals(100f, actor.x)
		MetaLayout.assertValid(grid)
	}

	@Test
	fun `invalid grid geometry is rejected`() {
		assertFailsWith<IllegalArgumentException> { MetaGrid(minColumnWidth = 0f) }
		assertFailsWith<IllegalArgumentException> { MetaGrid(minColumnWidth = 100f, maxColumns = 0) }
		assertFailsWith<IllegalArgumentException> { MetaGrid(minColumnWidth = 100f, columnGap = -1f) }
		assertFailsWith<IllegalArgumentException> {
			MetaGrid(minColumnWidth = 100f).addItem(Actor(), columnSpan = 0)
		}
	}

	private class WidthResponsiveWidget : Widget() {
		override fun getPrefWidth(): Float = 100f
		override fun getPrefHeight(): Float = if (width >= 130f) 20f else 40f
		override fun getMinWidth(): Float = 10f
		override fun getMinHeight(): Float = 10f
	}
}
