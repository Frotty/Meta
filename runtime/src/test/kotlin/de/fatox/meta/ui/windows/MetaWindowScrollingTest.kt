package de.fatox.meta.ui.windows

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import de.fatox.meta.test.GdxTestEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MetaWindowScrollingTest {
	@BeforeTest
	fun setUp() = GdxTestEnvironment.ensure()

	@Test
	fun `window content enables horizontal scrolling when content is wider than viewport`() {
		val track = BaseDrawable().apply { minHeight = 6f }
		val knob = BaseDrawable().apply {
			minWidth = 12f
			minHeight = 6f
		}
		val pane = ScrollPane(
			FixedSizeWidget(320f, 80f),
			ScrollPane.ScrollPaneStyle().apply {
				hScroll = track
				hScrollKnob = knob
			},
		)

		configureWindowContentScrolling(pane)
		pane.setSize(120f, 100f)
		pane.validate()

		assertFalse(pane.isScrollingDisabledX)
		assertTrue(pane.isScrollX)
	}

	private class FixedSizeWidget(private val preferredWidth: Float, private val preferredHeight: Float) : Widget() {
		override fun getPrefWidth(): Float = preferredWidth
		override fun getPrefHeight(): Float = preferredHeight
	}
}
