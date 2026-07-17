package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import de.fatox.meta.ui.layout.MetaLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class MetaStackTest {
	@Test
	fun `default stack children fill the complete shared bounds`() {
		val background = Actor().apply { setSize(20f, 10f) }
		val overlay = Actor().apply { setSize(30f, 15f) }
		val stack = MetaStack().addItem(background).addItem(overlay)
		stack.setSize(100f, 60f)
		stack.layout()

		assertEquals(100f, background.width)
		assertEquals(60f, background.height)
		assertEquals(100f, overlay.width)
		assertEquals(60f, overlay.height)
		assertEquals(30f, stack.prefWidth)
		assertEquals(15f, stack.prefHeight)
	}

	@Test
	fun `per-item alignment positions compact overlays`() {
		val badge = Actor()
		val stack = MetaStack().addItem(
			badge,
			basisWidth = 20f,
			basisHeight = 10f,
			horizontalAlign = MetaFlexAlign.END,
			verticalAlign = MetaFlexAlign.START,
		)
		stack.setSize(100f, 60f)
		stack.layout()

		assertEquals(80f, badge.x)
		assertEquals(50f, badge.y)
	}

	@Test
	fun `stretched layout children derive height from stack width`() {
		val responsive = WidthResponsiveWidget()
		val stack = MetaStack(verticalAlign = MetaFlexAlign.START).addItem(responsive)
		stack.setSize(150f, 20f)

		assertEquals(20f, stack.prefHeight)
		stack.layout()
		MetaLayout.assertValid(stack)
	}

	@Test
	fun `stack child mutation and invalid bases are handled`() {
		val actor = Actor()
		val stack = MetaStack().addItem(actor, basisWidth = 20f, basisHeight = 10f)
		assertEquals(20f, stack.prefWidth)
		stack.removeActor(actor)
		assertEquals(0f, stack.prefWidth)
		assertFailsWith<IllegalArgumentException> { stack.addItem(Actor(), basisWidth = -1f) }
	}

	private class WidthResponsiveWidget : Widget() {
		override fun getPrefWidth(): Float = 50f
		override fun getPrefHeight(): Float = if (width >= 100f) 20f else 40f
		override fun getMinWidth(): Float = 10f
		override fun getMinHeight(): Float = 10f
	}
}
