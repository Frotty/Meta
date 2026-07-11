@file:Suppress("GDXKotlinUnsafeIterator")

package de.fatox.meta.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.Viewport
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.input.KeyListener
import de.fatox.meta.input.MetaUiAction
import de.fatox.meta.input.MetaUiInputBindings
import de.fatox.meta.input.ScrollListener
import de.fatox.meta.ui.components.MetaScrollPane
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.test.GdxTestEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class UiControlHelperTest {
	private lateinit var input: TestInput
	private lateinit var renderer: TestRenderer
	private lateinit var bindings: MetaUiInputBindings
	private lateinit var helper: UiControlHelper
	private lateinit var stage: Stage

	@BeforeTest
	fun setUp() {
		GdxTestEnvironment.ensure()
		input = TestInput()
		renderer = TestRenderer()
		bindings = MetaUiInputBindings()
		MetaInject.global(clear = true) {
			singleton<MetaInputProcessor>(input)
			singleton<UIRenderer>(renderer)
			singleton(bindings)
		}
		helper = UiControlHelper()
		stage = Stage(TestViewport(), NullBatch())
		stage.viewport.update(800, 600, true)
	}

	@Test
	fun `navigation skips disabled controls`() {
		val root = testRoot()
		val top = button(0f, 100f).apply { name = "top" }
		val disabledMiddle = button(0f, 50f).apply {
			name = "disabled"
			isDisabled = true
		}
		val bottom = button(0f, 0f).apply { name = "bottom" }
		root.addActor(top)
		root.addActor(disabledMiddle)
		root.addActor(bottom)
		stage.addActor(root)

		helper.focusFirstIn(root, top)
		input.keyDown(Input.Keys.DOWN)
		input.keyUp(Input.Keys.DOWN)

		assertSame(
			bottom,
			renderer.currentFocusedActor,
			"focused ${renderer.currentFocusedActor?.name}, top=${edges(top)}, bottom=${edges(bottom)}"
		)
		assertSame(bottom, helper.focusedActor.value)
	}

	@Test
	fun `nested MetaScrollPanes claim and restore mouse wheel focus on hover`() {
		val outerContent = Group()
		val innerContent = Actor()
		val outer = MetaScrollPane(null, ScrollPane.ScrollPaneStyle())
		val inner = MetaScrollPane(null, ScrollPane.ScrollPaneStyle())
		outer.setActor(outerContent)
		inner.setActor(innerContent)
		outerContent.addActor(inner)
		stage.addActor(outer)

		outerContent.fire(mouseHoverEvent(InputEvent.Type.enter))
		assertSame(outer, stage.scrollFocus)

		innerContent.fire(mouseHoverEvent(InputEvent.Type.enter))
		assertSame(inner, stage.scrollFocus)

		innerContent.fire(mouseHoverEvent(InputEvent.Type.exit))
		assertSame(outer, stage.scrollFocus)

		outerContent.fire(mouseHoverEvent(InputEvent.Type.exit))
		assertNull(stage.scrollFocus)
	}

	@Test
	fun `navigation stays inside the focused modal root`() {
		val background = button(0f, 120f)
		val modalRoot = testRoot()
		val modalTop = button(0f, 60f)
		val modalBottom = button(0f, 0f)
		modalRoot.addActor(modalTop)
		modalRoot.addActor(modalBottom)
		stage.addActor(background)
		stage.addActor(modalRoot)

		helper.focusFirstIn(modalRoot, modalTop)
		input.keyDown(Input.Keys.UP)
		input.keyUp(Input.Keys.UP)

		assertSame(modalTop, renderer.currentFocusedActor)
		assertSame(modalTop, helper.focusedActor.value)
	}

	@Test
	fun `manual navigation keeps legacy parent lineage search outside scoped roots`() {
		val root = testRoot()
		val nestedGroup = testRoot()
		val siblingGroup = testRoot()
		val nestedButton = button(0f, 0f)
		val siblingButton = button(0f, 120f)
		nestedGroup.addActor(nestedButton)
		siblingGroup.addActor(siblingButton)
		root.addActor(nestedGroup)
		root.addActor(siblingGroup)
		stage.addActor(root)

		helper.selectedActor = nestedButton
		input.keyDown(Input.Keys.UP)
		input.keyUp(Input.Keys.UP)

		assertSame(siblingButton, renderer.currentFocusedActor)
		assertSame(siblingButton, helper.focusedActor.value)
	}

	@Test
	fun `focusFirstIn scopes navigation to the requested root`() {
		val background = button(0f, 120f)
		val scopedRoot = testRoot()
		val scopedTop = button(0f, 60f)
		val scopedBottom = button(0f, 0f)
		scopedRoot.addActor(scopedTop)
		scopedRoot.addActor(scopedBottom)
		stage.addActor(background)
		stage.addActor(scopedRoot)

		helper.focusFirstIn(scopedRoot, scopedTop)
		input.keyDown(Input.Keys.UP)
		input.keyUp(Input.Keys.UP)

		assertSame(scopedTop, renderer.currentFocusedActor)
		assertSame(scopedTop, helper.focusedActor.value)
	}

	@Test
	fun `text fields can receive initial UI focus`() {
		val root = testRoot()
		val textField = TextField("", TextField.TextFieldStyle(headlessFont(), Color.WHITE, null, null, null))
			.apply { setBounds(0f, 0f, 120f, 24f) }
		root.addActor(textField)
		stage.addActor(root)

		helper.focusFirstIn(root)

		assertSame(textField, renderer.currentFocusedActor)
		assertSame(textField, helper.focusedActor.value)
	}

	@Test
	fun `custom keyboard confirm activates focused button and emits canonical enter`() {
		val root = testRoot()
		val button = button(0f, 0f)
		var clicks = 0
		var enterUps = 0
		button.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				clicks++
			}
		})
		input.addGlobalKeyListener(Input.Keys.ENTER, 0, object : KeyListener() {
			override fun onEvent() = Unit

			override fun onUp() {
				enterUps++
			}
		})
		root.addActor(button)
		stage.addActor(root)
		bindings.setKeyboardKeys(MetaUiAction.CONFIRM, Input.Keys.SPACE)

		helper.focusFirstIn(root, button)
		input.keyDown(Input.Keys.SPACE)
		input.keyUp(Input.Keys.SPACE)

		assertEquals(1, clicks)
		assertEquals(1, enterUps)
	}

	@Test
	fun `clearing focus inside a removed root leaves outside focus alone`() {
		val root = testRoot()
		val inside = button(0f, 0f)
		val outside = button(80f, 0f)
		root.addActor(inside)
		stage.addActor(root)
		stage.addActor(outside)

		helper.focusFirstIn(root, inside)
		helper.clearFocusIfInside(root)

		assertNull(renderer.currentFocusedActor)
		helper.selectedActor = outside

		helper.clearFocusIfInside(root)

		assertSame(outside, renderer.currentFocusedActor)
	}

	@Test
	fun `manual selection can still focus custom actors for fallback rendering`() {
		val customActor = Actor().apply { setBounds(0f, 0f, 64f, 32f) }
		stage.addActor(customActor)

		helper.selectedActor = customActor

		assertSame(customActor, renderer.currentFocusedActor)
		assertSame(customActor, helper.focusedActor.value)
	}

	private fun button(x: Float, y: Float): Button =
		Button().apply { setBounds(x, y, 48f, 24f) }

	private fun testRoot(): Group =
		Group().apply { setBounds(0f, 0f, 800f, 600f) }

	private fun mouseHoverEvent(type: InputEvent.Type): InputEvent = InputEvent().apply {
		this.type = type
		pointer = -1
	}

	private fun edges(actor: Actor): String {
		val tmp = Vector2()
		actor.localToStageCoordinates(tmp)
		return "${tmp.x},${tmp.y},${tmp.x + actor.width},${tmp.y + actor.height}"
	}

	private fun headlessFont(): BitmapFont {
		val data = BitmapFont.BitmapFontData().apply {
			lineHeight = 12f
			capHeight = 9f
			ascent = 9f
			descent = -3f
		}
		return BitmapFont(data, TextureRegion(), false)
	}

	private class TestInput : InputAdapter(), MetaInputProcessor {
		private val globalProcessors = ArrayList<InputProcessor>()
		private val globalKeys = HashMap<Int, ArrayList<KeyListener>>()
		override var exclusiveProcessor: InputProcessor? = null
		override val isLeftCtrlDown = false
		override val isRightCtrlDown = false
		override val isLeftShiftDown = false
		override val isRightShiftDown = false

		override fun pushExclusiveProcessor(processor: InputProcessor) {
			exclusiveProcessor = processor
		}

		override fun popExclusiveProcessor(processor: InputProcessor): Boolean {
			if (exclusiveProcessor !== processor) return false
			exclusiveProcessor = null
			return true
		}

		override fun clearExclusiveProcessors() {
			exclusiveProcessor = null
		}

		override fun changeScreen() = Unit
		override fun addGlobalInputProcessor(inputProcessor: InputProcessor): InputProcessor =
			inputProcessor.also { globalProcessors.add(it) }

		override fun removeGlobalInputProcessor(inputProcessor: InputProcessor): Boolean =
			globalProcessors.remove(inputProcessor)

		override fun addScreenInputProcessor(inputProcessor: InputProcessor): InputProcessor = inputProcessor
		override fun removeScreenInputProcessor(inputProcessor: InputProcessor): Boolean = false

		override fun addGlobalKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener {
			globalKeys.getOrPut(keycode) { ArrayList() }.add(keyListener)
			return keyListener
		}

		override fun removeGlobalKeyListener(keycode: Int, keyListener: KeyListener): Boolean =
			globalKeys[keycode]?.remove(keyListener) ?: false

		override fun addScreenKeyListener(keycode: Int, millisRequired: Long, keyListener: KeyListener): KeyListener =
			keyListener

		override fun removeScreenKeyListener(keycode: Int, keyListener: KeyListener): Boolean = false
		override fun addGlobalScrollListener(scrollListener: ScrollListener): ScrollListener = scrollListener
		override fun removeGlobalScrollListener(scrollListener: ScrollListener): Boolean = false
		override fun addScreenScrollListener(scrollListener: ScrollListener): ScrollListener = scrollListener
		override fun removeScreenScrollListener(scrollListener: ScrollListener): Boolean = false

		override fun keyDown(keycode: Int): Boolean {
			exclusiveProcessor?.keyDown(keycode)?.let { return false }
			globalKeys[keycode]?.forEach { it.onDown() }
			for (i in globalProcessors.indices) globalProcessors[i].keyDown(keycode)
			return false
		}

		override fun keyUp(keycode: Int): Boolean {
			exclusiveProcessor?.keyUp(keycode)?.let { return false }
			globalKeys[keycode]?.forEach { it.onUp() }
			for (i in globalProcessors.indices) globalProcessors[i].keyUp(keycode)
			return false
		}
	}

	private class TestRenderer : UIRenderer {
		override val uiScale: Signal<Float> = signal(1f)
		override val uiWidth = 800f
		override val uiHeight = 600f
		var currentFocusedActor: Actor? = null

		override fun load() = Unit
		override fun addActor(actor: Actor) = Unit
		override fun update() = Unit
		override fun draw() = Unit
		override fun resize(width: Int, height: Int) = Unit
		override fun getCamera(): Camera = OrthographicCamera()
		override fun getToastManager(): MetaToastManager = throw UnsupportedOperationException()
		override fun setFocusedActor(actor: Actor?) {
			currentFocusedActor = actor
		}
	}

	private class NullBatch : Batch {
		private val color = Color.WHITE.cpy()
		private val projection = Matrix4()
		private val transform = Matrix4()
		private var drawing = false
		override fun begin() {
			drawing = true
		}

		override fun end() {
			drawing = false
		}

		override fun setColor(tint: Color) {
			color.set(tint)
		}

		override fun setColor(r: Float, g: Float, b: Float, a: Float) {
			color.set(r, g, b, a)
		}

		override fun getColor(): Color = color
		override fun setPackedColor(color: Float) = Unit
		override fun getPackedColor(): Float = Color.toFloatBits(color.r, color.g, color.b, color.a)
		override fun draw(
			texture: Texture,
			x: Float,
			y: Float,
			originX: Float,
			originY: Float,
			width: Float,
			height: Float,
			scaleX: Float,
			scaleY: Float,
			rotation: Float,
			srcX: Int,
			srcY: Int,
			srcWidth: Int,
			srcHeight: Int,
			flipX: Boolean,
			flipY: Boolean,
		) = Unit

		override fun draw(
			texture: Texture,
			x: Float,
			y: Float,
			width: Float,
			height: Float,
			srcX: Int,
			srcY: Int,
			srcWidth: Int,
			srcHeight: Int,
			flipX: Boolean,
			flipY: Boolean,
		) = Unit

		override fun draw(texture: Texture, x: Float, y: Float, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int) =
			Unit

		override fun draw(
			texture: Texture,
			x: Float,
			y: Float,
			width: Float,
			height: Float,
			u: Float,
			v: Float,
			u2: Float,
			v2: Float,
		) = Unit

		override fun draw(texture: Texture, x: Float, y: Float) = Unit
		override fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float) = Unit
		override fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) = Unit
		override fun draw(region: TextureRegion, x: Float, y: Float) = Unit
		override fun draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) = Unit
		override fun draw(
			region: TextureRegion,
			x: Float,
			y: Float,
			originX: Float,
			originY: Float,
			width: Float,
			height: Float,
			scaleX: Float,
			scaleY: Float,
			rotation: Float,
		) = Unit

		override fun draw(
			region: TextureRegion,
			x: Float,
			y: Float,
			originX: Float,
			originY: Float,
			width: Float,
			height: Float,
			scaleX: Float,
			scaleY: Float,
			rotation: Float,
			clockwise: Boolean,
		) = Unit

		override fun draw(region: TextureRegion, width: Float, height: Float, transform: Affine2) = Unit
		override fun flush() = Unit
		override fun disableBlending() = Unit
		override fun enableBlending() = Unit
		override fun setBlendFunction(srcFunc: Int, dstFunc: Int) = Unit
		override fun setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int) =
			Unit

		override fun getBlendSrcFunc(): Int = 0
		override fun getBlendDstFunc(): Int = 0
		override fun getBlendSrcFuncAlpha(): Int = 0
		override fun getBlendDstFuncAlpha(): Int = 0
		override fun getProjectionMatrix(): Matrix4 = projection
		override fun getTransformMatrix(): Matrix4 = transform
		override fun setProjectionMatrix(projection: Matrix4) {
			this.projection.set(projection)
		}

		override fun setTransformMatrix(transform: Matrix4) {
			this.transform.set(transform)
		}

		override fun setShader(shader: ShaderProgram?) = Unit
		override fun getShader(): ShaderProgram? = null
		override fun isBlendingEnabled(): Boolean = true
		override fun isDrawing(): Boolean = drawing
		override fun dispose() = Unit
	}

	private class TestViewport : Viewport() {
		init {
			camera = OrthographicCamera()
			setWorldSize(800f, 600f)
		}

		override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
			setScreenBounds(0, 0, screenWidth, screenHeight)
			if (centerCamera) camera.position.set(worldWidth * 0.5f, worldHeight * 0.5f, 0f)
			camera.update()
		}
	}
}
