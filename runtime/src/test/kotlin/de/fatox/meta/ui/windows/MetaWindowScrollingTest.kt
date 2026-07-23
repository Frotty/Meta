package de.fatox.meta.ui.windows

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.test.GdxTestEnvironment
import de.fatox.meta.ui.MetaSkin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Geometry contracts for Meta's dialog/window overflow policy. No GL rendering is involved. */
internal class MetaWindowScrollingTest {
	private lateinit var font: BitmapFont

	@BeforeTest
	fun setUp() {
		GdxTestEnvironment.ensure()
		MetaSkin.dispose()
		font = testFont()
		MetaSkin.initialize(Skin().apply {
			val windowStyle = Window.WindowStyle(font, Color.WHITE, BaseDrawable())
			add(MetaSkin.WINDOW, windowStyle, Window.WindowStyle::class.java)
			add(MetaSkin.WINDOW_RESIZABLE, Window.WindowStyle(windowStyle), Window.WindowStyle::class.java)
			add(MetaSkin.SCROLL_PANE, ScrollPane.ScrollPaneStyle(), ScrollPane.ScrollPaneStyle::class.java)
			add(MetaSkin.SCROLL_PANE_FLAT, ScrollPane.ScrollPaneStyle(), ScrollPane.ScrollPaneStyle::class.java)
			add(MetaSkin.SEPARATOR, BaseDrawable(), Drawable::class.java)
			add(MetaSkin.COLOR_FILL, BaseDrawable(), Drawable::class.java)
		}, installDefaults = false)
		global(clear = true) {
			singleton<FontProvider> {
				object : FontProvider {
					override fun getFont(size: Int, type: FontType): BitmapFont = font
					override fun write(x: Float, y: Float, text: String, size: Int, type: FontType) = Unit
				}
			}
		}
	}

	@AfterTest
	fun tearDown() {
		MetaSkin.dispose()
		global(clear = true) {}
	}

	@Test
	fun `fixed horizontal overflow enables horizontal scrolling`() {
		val pane = pane(FixedSizeWidget(320f, 80f), width = 120f, height = 100f)

		assertFalse(pane.isScrollingDisabledX)
		assertTrue(pane.isScrollX)
		assertFalse(pane.isScrollY)
		assertTrue(pane.maxX > 0f)
	}

	@Test
	fun `fixed vertical overflow enables only vertical scrolling`() {
		val pane = pane(FixedSizeWidget(80f, 180f), width = 120f, height = 100f)

		assertTrue(pane.isScrollingDisabledX)
		assertFalse(pane.isScrollX)
		assertTrue(pane.isScrollY)
		assertTrue(pane.maxY > 0f)
	}

	@Test
	fun `overflow on both axes exposes both scroll directions`() {
		val pane = pane(FixedSizeWidget(320f, 180f), width = 120f, height = 100f)

		assertFalse(pane.isScrollingDisabledX)
		assertTrue(pane.isScrollX)
		assertTrue(pane.isScrollY)
	}

	@Test
	fun `content that fits shows no scrollbars`() {
		val pane = pane(FixedSizeWidget(80f, 80f), width = 120f, height = 100f)

		assertTrue(pane.isScrollingDisabledX)
		assertFalse(pane.isScrollX)
		assertFalse(pane.isScrollY)
	}

	@Test
	fun `content exactly matching viewport does not count as overflow`() {
		val pane = pane(FixedSizeWidget(120f, 100f), width = 120f, height = 100f)

		assertTrue(pane.isScrollingDisabledX)
		assertFalse(pane.isScrollX)
		assertFalse(pane.isScrollY)
	}

	@Test
	fun `responsive content uses viewport width while its minimum still fits`() {
		val content = FixedSizeWidget(preferredWidth = 320f, preferredHeight = 80f, minimumWidth = 80f)
		val pane = pane(content, width = 120f, height = 100f)

		assertTrue(pane.isScrollingDisabledX)
		assertFalse(pane.isScrollX)
		assertEquals(pane.scrollWidth, content.width, 0.01f)
	}

	@Test
	fun `responsive content scrolls horizontally below its minimum width`() {
		val pane = pane(
			FixedSizeWidget(preferredWidth = 320f, preferredHeight = 80f, minimumWidth = 140f),
			width = 120f,
			height = 100f,
		)

		assertFalse(pane.isScrollingDisabledX)
		assertTrue(pane.isScrollX)
	}

	@Test
	fun `vertical scrollbar reduction can trigger real horizontal overflow`() {
		val style = scrollStyle(horizontalThickness = 8f, verticalThickness = 12f)
		val pane = pane(
			FixedSizeWidget(preferredWidth = 180f, preferredHeight = 200f, minimumWidth = 114f),
			width = 120f,
			height = 100f,
			style = style,
		)

		assertTrue(pane.isScrollY)
		assertFalse(pane.isScrollingDisabledX)
		assertTrue(pane.isScrollX)
	}

	@Test
	fun `scrollbars disappear again when viewport grows enough`() {
		val pane = pane(FixedSizeWidget(220f, 180f), width = 100f, height = 90f)
		assertTrue(pane.isScrollX)
		assertTrue(pane.isScrollY)

		pane.setSize(300f, 240f)
		pane.invalidate()
		updateWindowContentScrolling(pane)

		assertTrue(pane.isScrollingDisabledX)
		assertFalse(pane.isScrollX)
		assertFalse(pane.isScrollY)
	}

	@Test
	fun `scrollbars react when live content grows and shrinks`() {
		val content = MutableSizeWidget(80f, 70f)
		val pane = pane(content, width = 120f, height = 100f)
		assertFalse(pane.isScrollX)
		assertFalse(pane.isScrollY)

		content.resize(240f, 180f)
		updateWindowContentScrolling(pane)
		assertTrue(pane.isScrollX)
		assertTrue(pane.isScrollY)

		content.resize(80f, 70f)
		updateWindowContentScrolling(pane)
		assertFalse(pane.isScrollX)
		assertFalse(pane.isScrollY)
	}

	@Test
	fun `surface sizing fits preferred content when below responsive maximum`() {
		val size = resolveMetaSurfaceSize(0f, 0f, 420f, 260f, 1280f, 720f)

		assertEquals(MetaSurfaceSize(420f, 260f), size)
	}

	@Test
	fun `configured size acts as minimum for dialogs and fixed windows`() {
		val size = resolveMetaSurfaceSize(500f, 300f, 240f, 120f, 1280f, 720f)

		assertEquals(MetaSurfaceSize(500f, 300f), size)
	}

	@Test
	fun `small viewport caps surfaces responsively without filling screen`() {
		val size = resolveMetaSurfaceSize(0f, 0f, 2000f, 2000f, 800f, 600f)

		assertEquals(720f, size.width)
		assertEquals(528f, size.height)
		assertTrue(size.width < 800f)
		assertTrue(size.height < 600f)
	}

	@Test
	fun `large viewport retains a sane absolute Meta maximum`() {
		val size = resolveMetaSurfaceSize(0f, 0f, 3000f, 3000f, 2560f, 1440f)

		assertEquals(960f, size.width)
		assertEquals(840f, size.height)
	}

	@Test
	fun `responsive maximum grows with ordinary viewport sizes`() {
		val small = resolveMetaSurfaceSize(0f, 0f, 2000f, 2000f, 640f, 480f)
		val large = resolveMetaSurfaceSize(0f, 0f, 2000f, 2000f, 1000f, 800f)

		assertTrue(large.width > small.width)
		assertTrue(large.height > small.height)
	}

	@Test
	fun `fixed window grows to content but remains under responsive cap`() {
		val window = TestWindow(resizable = false)
		window.contentTable.add(FixedSizeWidget(1200f, 1000f))

		window.fitStaticSurfaceToContent(800f, 600f)

		assertEquals(720f, window.width)
		assertEquals(528f, window.height)
		window.validate()
		assertTrue(window.contentViewport.isScrollX)
		assertTrue(window.contentViewport.isScrollY)
	}

	@Test
	fun `fixed window with fitting content auto-sizes without scrollbars`() {
		val window = TestWindow(resizable = false)
		window.contentTable.add(FixedSizeWidget(240f, 140f))

		window.fitStaticSurfaceToContent(1000f, 800f)
		window.validate()

		assertTrue(window.width >= 240f)
		assertTrue(window.height >= 140f)
		assertFalse(window.contentViewport.isScrollX)
		assertFalse(window.contentViewport.isScrollY)
	}

	@Test
	fun `fixed window dynamically grows with content then caps and scrolls`() {
		val content = MutableSizeWidget(120f, 80f)
		val window = TestWindow(resizable = false)
		window.contentTable.add(content)
		window.fitStaticSurfaceToContent(800f, 600f)
		val initialWidth = window.width
		val initialHeight = window.height

		content.resize(420f, 300f)
		window.fitStaticSurfaceToContent(800f, 600f)
		assertTrue(window.width > initialWidth)
		assertTrue(window.height > initialHeight)

		content.resize(1200f, 1000f)
		window.fitStaticSurfaceToContent(800f, 600f)
		window.validate()
		assertEquals(720f, window.width)
		assertEquals(528f, window.height)
		assertTrue(window.contentViewport.isScrollX)
		assertTrue(window.contentViewport.isScrollY)
	}

	@Test
	fun `resizable window retains chosen size and makes overflowing body accessible`() {
		val window = TestWindow(resizable = true)
		window.contentTable.add(FixedSizeWidget(600f, 500f))
		window.setSize(220f, 160f)

		window.fitStaticSurfaceToContent(1200f, 800f)
		window.validate()

		assertEquals(220f, window.width)
		assertEquals(160f, window.height)
		assertTrue(window.contentViewport.isScrollX)
		assertTrue(window.contentViewport.isScrollY)
	}

	@Test
	fun `sticky window controls remain outside scrolling body`() {
		val window = TestWindow(resizable = true)
		val control = FixedSizeWidget(120f, 28f)
		window.controlTable.add(control).growX()
		window.contentTable.add(FixedSizeWidget(140f, 500f, minimumWidth = 100f))
		window.setSize(220f, 160f)
		window.validate()

		assertSame(window.controlTable, control.parent)
		assertFalse(control.isDescendantOf(window.contentViewport))
		assertTrue(window.contentViewport.isScrollY)
		assertEquals(28f, window.controlTable.height, 0.01f)
	}

	@Test
	fun `dialog body scrolls while status and actions stay fixed`() {
		val dialog = TestDialog()
		val action = Button()
		dialog.footer.add(action).size(100f, 36f)
		dialog.contentTable.add(FixedSizeWidget(900f, 900f))
		dialog.setDefaultSize(260f, 180f)

		dialog.fitStaticSurfaceToContent(800f, 600f)
		dialog.validate()

		assertEquals(720f, dialog.width)
		assertEquals(528f, dialog.height)
		assertTrue(dialog.contentViewport.isScrollX)
		assertTrue(dialog.contentViewport.isScrollY)
		assertFalse(action.isDescendantOf(dialog.contentViewport))
	}

	@Test
	fun `dialog with fitting content does not show unnecessary scrollbars`() {
		val dialog = TestDialog()
		dialog.contentTable.add(FixedSizeWidget(220f, 100f))
		dialog.setDefaultSize(260f, 180f)

		dialog.fitStaticSurfaceToContent(1000f, 800f)
		dialog.validate()

		assertFalse(dialog.contentViewport.isScrollX)
		assertFalse(dialog.contentViewport.isScrollY)
	}

	@Test
	fun `empty dialog status and action rows consume no layout space`() {
		val dialog = TestDialog()
		dialog.contentTable.add(FixedSizeWidget(220f, 100f))
		dialog.invalidateHierarchy()
		val emptyHeight = dialog.prefHeight

		assertFalse(dialog.status.isVisible)
		assertFalse(dialog.footer.isVisible)

		dialog.setStatus("Ready")
		dialog.footer.add(Button()).size(100f, 36f)
		dialog.invalidateHierarchy()
		val populatedHeight = dialog.prefHeight

		assertTrue(dialog.status.isVisible)
		assertTrue(dialog.footer.isVisible)
		assertTrue(populatedHeight > emptyHeight)

		dialog.setStatus("")
		dialog.footer.clearChildren()
		dialog.invalidateHierarchy()
		assertEquals(emptyHeight, dialog.prefHeight, 0.01f)
	}

	@Test
	fun `centered surface bounds preserve a stage margin and constrain oversized content`() {
		val bounds = resolveCenteredSurfaceBounds(900f, 700f, 800f, 600f, 16f)

		assertEquals(MetaSurfaceBounds(16f, 16f, 768f, 568f), bounds)
	}

	@Test
	fun `centered surface bounds retain fitting content size`() {
		val bounds = resolveCenteredSurfaceBounds(320f, 180f, 800f, 600f, 16f)

		assertEquals(MetaSurfaceBounds(240f, 210f, 320f, 180f), bounds)
	}

	@Test
	fun `manager concealment preserves visibility and title geometry`() {
		val window = TestWindow(resizable = true)
		window.setBounds(20f, 180f, 300f, 160f)
		window.layout()
		val titleX = window.titleTable.x
		val titleY = window.titleTable.y
		val titleWidth = window.titleTable.width
		val transform = window.isTransform
		val scale = window.scaleX
		val alpha = window.color.a

		window.setManagerConcealed(true)

		assertTrue(window.isVisible)
		assertEquals(transform, window.isTransform)
		assertEquals(scale, window.scaleX)
		assertEquals(alpha, window.color.a)
		assertTrue(window.isManagerConcealed)
		assertNull(window.hit(10f, 10f, true))
		assertEquals(titleX, window.titleTable.x, 0.01f)
		assertEquals(titleY, window.titleTable.y, 0.01f)
		assertEquals(titleWidth, window.titleTable.width, 0.01f)

		window.setManagerConcealed(false)

		assertFalse(window.isManagerConcealed)
		assertNotNull(window.hit(10f, 10f, true))
		assertEquals(titleX, window.titleTable.x, 0.01f)
		assertEquals(titleY, window.titleTable.y, 0.01f)
		assertEquals(titleWidth, window.titleTable.width, 0.01f)
	}

	private fun pane(
		content: Actor,
		width: Float,
		height: Float,
		style: ScrollPane.ScrollPaneStyle = scrollStyle(),
	): ScrollPane = ScrollPane(content, style).apply {
		configureWindowContentScrolling(this)
		setSize(width, height)
		updateWindowContentScrolling(this)
	}

	private class TestWindow(resizable: Boolean) : MetaWindow("", resizable, false)

	private class TestDialog : MetaDialog("", false) {
		val footer get() = buttonTable
		val status get() = statusLabel
		fun setStatus(text: String) = statusLabel.setText(text)
	}

	private class FixedSizeWidget(
		private val preferredWidth: Float,
		private val preferredHeight: Float,
		private val minimumWidth: Float = preferredWidth,
		private val minimumHeight: Float = preferredHeight,
	) : Widget() {
		override fun getPrefWidth(): Float = preferredWidth
		override fun getPrefHeight(): Float = preferredHeight
		override fun getMinWidth(): Float = minimumWidth
		override fun getMinHeight(): Float = minimumHeight
	}

	private class MutableSizeWidget(
		private var preferredWidth: Float,
		private var preferredHeight: Float,
	) : Widget() {
		override fun getPrefWidth(): Float = preferredWidth
		override fun getPrefHeight(): Float = preferredHeight
		override fun getMinWidth(): Float = preferredWidth
		override fun getMinHeight(): Float = preferredHeight

		fun resize(width: Float, height: Float) {
			preferredWidth = width
			preferredHeight = height
			invalidateHierarchy()
		}
	}

	private fun scrollStyle(
		horizontalThickness: Float = 0f,
		verticalThickness: Float = 0f,
	): ScrollPane.ScrollPaneStyle = ScrollPane.ScrollPaneStyle().apply {
		if (horizontalThickness > 0f) {
			hScroll = BaseDrawable().apply { minHeight = horizontalThickness }
			hScrollKnob = BaseDrawable().apply {
				minWidth = 8f
				minHeight = horizontalThickness
			}
		}
		if (verticalThickness > 0f) {
			vScroll = BaseDrawable().apply { minWidth = verticalThickness }
			vScrollKnob = BaseDrawable().apply {
				minWidth = verticalThickness
				minHeight = 8f
			}
		}
	}

	private fun testFont(): BitmapFont {
		val data = BitmapFont.BitmapFontData().apply {
			lineHeight = 12f
			capHeight = 9f
			ascent = 9f
			descent = -4f
			spaceXadvance = 4f
		}
		return BitmapFont(data, TextureRegion(), false).apply {
			for (code in 32..126) {
				this.data.setGlyph(code, BitmapFont.Glyph().apply {
					id = code
					width = 1
					height = 1
					xadvance = 8
				})
			}
		}
	}
}
