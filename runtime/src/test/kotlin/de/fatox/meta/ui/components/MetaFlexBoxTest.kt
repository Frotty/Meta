package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import de.fatox.meta.ui.layout.MetaLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class MetaFlexBoxTest {
	@Test
	fun `wrapped rows preserve order and derive height from assigned width`() {
		val items = List(5) { Actor() }
		val flex = MetaFlexBox(wrap = true, mainGap = 4f, crossGap = 4f)
		for (item in items) flex.addItem(item, basisWidth = 20f, basisHeight = 20f)
		flex.setSize(68f, 44f)
		flex.layout()

		assertEquals(0f, items[0].x)
		assertEquals(24f, items[1].x)
		assertEquals(48f, items[2].x)
		assertEquals(24f, items[0].y)
		assertEquals(0f, items[3].y)
		assertEquals(20f, flex.prefWidth)
		assertEquals(44f, flex.prefHeight)
	}

	@Test
	fun `grow distributes remaining main-axis space`() {
		val first = Actor()
		val second = Actor()
		val flex = MetaFlexBox(mainGap = 4f)
		flex.addItem(first, basisWidth = 20f, basisHeight = 10f, grow = 1f)
		flex.addItem(second, basisWidth = 20f, basisHeight = 10f, grow = 1f)
		flex.setSize(100f, 10f)
		flex.layout()

		assertEquals(48f, first.width)
		assertEquals(52f, second.x)
		assertEquals(48f, second.width)
	}

	@Test
	fun `shrink distributes overflow without crossing item minimums`() {
		val protected = Actor()
		val flexible = Actor()
		val flex = MetaFlexBox(mainGap = 4f)
		flex.addItem(protected, basisWidth = 100f, basisHeight = 10f, minWidth = 80f)
		flex.addItem(flexible, basisWidth = 100f, basisHeight = 10f, minWidth = 0f)
		flex.setSize(100f, 10f)

		flex.layout()

		assertEquals(80f, protected.width)
		assertEquals(84f, flexible.x)
		assertEquals(16f, flexible.width)
		assertEquals(84f, flex.minWidth)
	}

	@Test
	fun `column direction wraps into additional columns`() {
		val items = List(3) { Actor() }
		val flex = MetaFlexBox(direction = MetaFlexDirection.COLUMN, wrap = true, mainGap = 4f, crossGap = 4f)
		for (item in items) flex.addItem(item, basisWidth = 20f, basisHeight = 20f)
		flex.setSize(44f, 44f)
		flex.layout()

		assertEquals(24f, items[0].y)
		assertEquals(0f, items[1].y)
		assertEquals(24f, items[2].x)
	}

	@Test
	fun `items use their natural sizes when no explicit basis is supplied`() {
		val first = Actor().apply { setSize(30f, 10f) }
		val second = Actor().apply { setSize(20f, 15f) }
		val flex = MetaFlexBox(mainGap = 4f, align = MetaFlexAlign.END)
		flex.addItem(first).addItem(second)
		flex.setSize(60f, 20f)
		flex.layout()

		assertEquals(54f, flex.prefWidth)
		assertEquals(15f, flex.prefHeight)
		assertEquals(0f, first.y)
		assertEquals(0f, second.y)
	}

	@Test
	fun `space between and stretch apply within each line`() {
		val first = Actor()
		val second = Actor()
		val flex = MetaFlexBox(justify = MetaFlexJustify.SPACE_BETWEEN, align = MetaFlexAlign.STRETCH)
		flex.addItem(first, basisWidth = 20f, basisHeight = 8f)
		flex.addItem(second, basisWidth = 20f, basisHeight = 12f)
		flex.setSize(100f, 30f)
		flex.layout()

		assertEquals(80f, second.x)
		assertEquals(30f, first.height)
		assertEquals(30f, second.height)
	}

	@Test
	fun `space around and evenly distribute outer and inner free space`() {
		val aroundFirst = Actor()
		val aroundSecond = Actor()
		val around = MetaFlexBox(mainGap = 0f, justify = MetaFlexJustify.SPACE_AROUND)
		around.addItem(aroundFirst, basisWidth = 20f, basisHeight = 10f)
		around.addItem(aroundSecond, basisWidth = 20f, basisHeight = 10f)
		around.setSize(100f, 10f)
		around.layout()
		assertEquals(15f, aroundFirst.x)
		assertEquals(65f, aroundSecond.x)

		val evenFirst = Actor()
		val evenSecond = Actor()
		val evenly = MetaFlexBox(mainGap = 0f, justify = MetaFlexJustify.SPACE_EVENLY)
		evenly.addItem(evenFirst, basisWidth = 20f, basisHeight = 10f)
		evenly.addItem(evenSecond, basisWidth = 20f, basisHeight = 10f)
		evenly.setSize(100f, 10f)
		evenly.layout()
		assertEquals(20f, evenFirst.x)
		assertEquals(60f, evenSecond.x)
	}

	@Test
	fun `growing layout children remeasure their height at the assigned width`() {
		val responsive = WidthResponsiveWidget()
		val flex = MetaFlexBox(align = MetaFlexAlign.START)
		flex.addItem(responsive, basisWidth = 50f, grow = 1f)
		flex.setSize(150f, 20f)
		flex.layout()

		assertEquals(150f, responsive.width)
		assertEquals(20f, flex.prefHeight)
		MetaLayout.assertValid(flex)
	}

	@Test
	fun `nested flex boxes wrap responsively without manual rebuilding`() {
		val inner = MetaFlexBox(wrap = true, mainGap = 4f, crossGap = 4f)
		inner.addItem(Actor(), basisWidth = 50f, basisHeight = 20f)
		inner.addItem(Actor(), basisWidth = 50f, basisHeight = 20f)
		val outer = MetaFlexBox().addItem(inner, basisWidth = 50f, grow = 1f)

		outer.setSize(150f, 20f)
		assertEquals(20f, outer.prefHeight)
		outer.layout()
		MetaLayout.assertValid(outer)

		outer.setSize(50f, 44f)
		assertEquals(44f, outer.prefHeight)
		outer.layout()
		MetaLayout.assertValid(outer)
	}

	@Test
	fun `runtime direction wrapping and child mutations reflow`() {
		val first = Actor()
		val second = Actor()
		val flex = MetaFlexBox(wrap = false, mainGap = 4f, crossGap = 4f)
		flex.addItem(first, basisWidth = 20f, basisHeight = 10f)
		flex.addItem(second, basisWidth = 20f, basisHeight = 10f)
		assertEquals(44f, flex.prefWidth)

		flex.wrap = true
		flex.setWidth(20f)
		assertEquals(24f, flex.prefHeight)
		flex.direction = MetaFlexDirection.COLUMN
		flex.setSize(44f, 10f)
		flex.layout()
		assertEquals(24f, second.x)

		flex.removeActor(first)
		assertEquals(20f, flex.prefWidth)
		flex.clearChildren()
		assertEquals(0f, flex.prefHeight)
	}

	@Test
	fun `invalid flex geometry is rejected`() {
		assertFailsWith<IllegalArgumentException> { MetaFlexBox(mainGap = -1f) }
		assertFailsWith<IllegalArgumentException> { MetaFlexBox(crossGap = Float.NaN) }
		assertFailsWith<IllegalArgumentException> {
			MetaFlexBox().addItem(Actor(), basisWidth = -1f)
		}
		assertFailsWith<IllegalArgumentException> { MetaFlexBox().addItem(Actor(), grow = -1f) }
		assertFailsWith<IllegalArgumentException> { MetaFlexBox().addItem(Actor(), shrink = -1f) }
		assertFailsWith<IllegalArgumentException> { MetaFlexBox().addItem(Actor(), minWidth = -1f) }
	}

	private class WidthResponsiveWidget : Widget() {
		override fun getPrefWidth(): Float = 50f
		override fun getPrefHeight(): Float = if (width >= 100f) 20f else 40f
		override fun getMinWidth(): Float = 10f
		override fun getMinHeight(): Float = 10f
	}
}
