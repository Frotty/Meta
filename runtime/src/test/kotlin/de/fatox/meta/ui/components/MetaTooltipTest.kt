package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.viewport.Viewport
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.global
import de.fatox.meta.test.GdxTestEnvironment
import de.fatox.meta.ui.MetaSkin
import java.lang.ref.WeakReference
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MetaTooltipTest {
	private lateinit var font: BitmapFont

	@BeforeTest
	fun setUp() {
		GdxTestEnvironment.ensure()
		MetaSkin.dispose()
		font = testFont()
		MetaSkin.initialize(Skin().apply {
			add("meta.tooltip", BaseDrawable(), Drawable::class.java)
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
	fun `tooltip text width uses content width until max is reached`() {
		val short = MetaTooltip.resolveTextWidth(contentWidth = 72f, maxWidth = 280f)
		assertEquals(72f, short.width)
		assertFalse(short.wrap)

		val long = MetaTooltip.resolveTextWidth(contentWidth = 520f, maxWidth = 280f)
		assertEquals(280f, long.width)
		assertTrue(long.wrap)

		val uncapped = MetaTooltip.resolveTextWidth(contentWidth = 520f, maxWidth = 0f)
		assertEquals(520f, uncapped.width)
		assertFalse(uncapped.wrap)
	}

	@Test
	fun `tooltip attached off-stage works after actor is first shown`() {
		val actor = Actor().apply { setBounds(20f, 20f, 40f, 20f) }
		MetaTooltip.attach(actor, "Rock", hideDelaySeconds = 0f)
		val stage = testStage()

		stage.addActor(actor)
		actor.fire(hover(InputEvent.Type.enter))

		assertTrue(MetaTooltip.isVisible(actor))
		MetaTooltip.remove(actor)
		stage.dispose()
	}

	@Test
	fun `tooltip survives stage detach and re-attach`() {
		val actor = Actor().apply { setBounds(20f, 20f, 40f, 20f) }
		val stage = testStage()
		MetaTooltip.attach(actor, "Reusable", hideDelaySeconds = 0f)
		stage.addActor(actor)
		actor.remove()

		assertTrue(MetaTooltip.isAttached(actor))
		stage.addActor(actor)
		actor.fire(hover(InputEvent.Type.enter))

		assertTrue(MetaTooltip.isVisible(actor))
		MetaTooltip.remove(actor)
		stage.dispose()
	}

	@Test
	fun `visible tooltip is removed when a reflow detaches its target`() {
		val actor = Actor().apply { setBounds(0f, 0f, 40f, 20f) }
		val stage = testStage()
		MetaTooltip.attach(actor, "Moving tile", hideDelaySeconds = 0f)
		stage.addActor(actor)
		actor.fire(hover(InputEvent.Type.enter))
		assertTrue(MetaTooltip.isVisible(actor))

		actor.remove()
		MetaTooltip.bringVisibleToFront()

		assertFalse(MetaTooltip.isVisible(actor))
		assertTrue(MetaTooltip.isAttached(actor))
		MetaTooltip.remove(actor)
		stage.dispose()
	}

	@Test
	fun `only the latest hovered target may own a visible tooltip`() {
		val first = Actor().apply { setBounds(0f, 0f, 40f, 20f) }
		val second = Actor().apply { setBounds(50f, 0f, 40f, 20f) }
		val stage = testStage()
		MetaTooltip.attach(first, "First", hideDelaySeconds = 0f)
		MetaTooltip.attach(second, "Second", hideDelaySeconds = 0f)
		stage.addActor(first)
		stage.addActor(second)

		first.fire(hover(InputEvent.Type.enter))
		second.fire(hover(InputEvent.Type.enter))

		assertFalse(MetaTooltip.isVisible(first))
		assertTrue(MetaTooltip.isVisible(second))
		MetaTooltip.remove(first)
		MetaTooltip.remove(second)
		stage.dispose()
	}

	@Test
	fun `repeated attach updates registration without adding another listener`() {
		val actor = Actor()
		val listenersBefore = actor.listeners.size

		MetaTooltip.attach(actor, "First")
		MetaTooltip.attach(actor, "Updated")

		assertEquals(listenersBefore + 1, actor.listeners.size)
		assertEquals("Updated", MetaTooltip.configuredText(actor))
		MetaTooltip.remove(actor)
	}

	@Test
	fun `explicit removal removes tooltip listener`() {
		val actor = Actor()
		val listenersBefore = actor.listeners.size
		MetaTooltip.attach(actor, "Temporary")

		MetaTooltip.remove(actor)

		assertFalse(MetaTooltip.isAttached(actor))
		assertEquals(listenersBefore, actor.listeners.size)
	}

	@Test
	fun `abandoned actor is not strongly retained by tooltip registry`() {
		val actorReference = abandonedTooltipActor()

		repeat(40) {
			if (actorReference.get() == null) return
			System.gc()
			Thread.sleep(10)
		}

		assertNull(actorReference.get())
	}

	private fun abandonedTooltipActor(): WeakReference<Actor> {
		val actor = Actor()
		MetaTooltip.attach(actor, "Collectible")
		return WeakReference(actor)
	}

	private fun hover(type: InputEvent.Type): InputEvent = InputEvent().apply {
		this.type = type
		pointer = -1
	}

	private fun testStage(): Stage = Stage(TestViewport(), noopBatch()).apply {
		viewport.update(320, 240, true)
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
			for (code in 32..126) data.setGlyph(code, BitmapFont.Glyph().apply {
				id = code
				width = 1
				height = 1
				xadvance = 8
			})
		}
	}

	private fun noopBatch(): Batch {
		val projection = Matrix4()
		val transform = Matrix4()
		return Proxy.newProxyInstance(Batch::class.java.classLoader, arrayOf(Batch::class.java)) { _, method, _ ->
			when (method.name) {
				"getProjectionMatrix" -> projection
				"getTransformMatrix" -> transform
				"isBlendingEnabled" -> true
				"isDrawing" -> false
				"getBlendSrcFunc", "getBlendDstFunc", "getBlendSrcFuncAlpha", "getBlendDstFuncAlpha" -> 0
				else -> null
			}
		} as Batch
	}

	private class TestViewport : Viewport() {
		init {
			camera = OrthographicCamera()
			setWorldSize(320f, 240f)
		}

		override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
			setScreenBounds(0, 0, screenWidth, screenHeight)
			if (centerCamera) camera.position.set(worldWidth * 0.5f, worldHeight * 0.5f, 0f)
			camera.update()
		}
	}
}
