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
import de.fatox.meta.input.MetaPlayer
import de.fatox.meta.input.MetaUiAction
import de.fatox.meta.input.MetaUiInputBindings
import de.fatox.meta.input.MetaUiInputProfiles
import de.fatox.meta.input.ScrollListener
import de.fatox.meta.ui.components.MetaScrollPane
import de.fatox.meta.ui.components.shiftedHorizontalScrollPosition
import de.fatox.meta.ui.components.updateMetaScrollFocus
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.test.GdxTestEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

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

	// ── Per-player cursors ────────────────────────────────────────────────────

	@Test
	fun `player one's profile is the registered singleton, not a copy`() {
		// The whole backwards-compatibility story: a consumer that injects MetaUiInputBindings and mutates it is
		// configuring player one, exactly as before this existed. A copy would leave every such consumer configuring
		// something nothing reads.
		val profiles = MetaUiInputProfiles()

		assertSame(bindings, profiles[MetaPlayer.ONE], "player one got a copy of the bindings, not the singleton")
	}

	@Test
	fun `a later player's profile starts empty`() {
		// Copying the defaults would give two people one cursor either could move, which is the outcome per-player
		// input exists to prevent. An unconfigured second player answers nothing instead.
		val profiles = MetaUiInputProfiles()

		val second = profiles[MetaPlayer(1)]

		assertNull(second.actionForKey(Input.Keys.UP), "player two inherited the arrows")
		assertNull(second.actionForKey(Input.Keys.ENTER), "player two inherited confirm")
		assertEquals(2, profiles.playerCount)
	}

	@Test
	fun `two cursors on one keyboard move independently`() {
		// The point of the whole thing: WASD and the arrows, one device, two cursors.
		val profiles = MetaUiInputProfiles()
		val two = MetaPlayer(1)
		profiles[two].setKeyboardKeys(MetaUiAction.NAVIGATE_DOWN, Input.Keys.S)
		val second = UiControlHelper(two, profiles[two])

		val left = testRoot()
		val leftTop = button(0f, 100f)
		val leftBottom = button(0f, 0f)
		left.addActor(leftTop)
		left.addActor(leftBottom)
		val right = testRoot()
		val rightTop = button(300f, 100f)
		val rightBottom = button(300f, 0f)
		right.addActor(rightTop)
		right.addActor(rightBottom)
		stage.addActor(left)
		stage.addActor(right)

		helper.focusFirstIn(left, leftTop)
		second.focusFirstIn(right, rightTop)

		// Player two's key must move player two only.
		input.keyDown(Input.Keys.S)
		input.keyUp(Input.Keys.S)
		assertSame(rightBottom, second.focusedActor.value, "player two's key did not move player two")
		assertSame(leftTop, helper.focusedActor.value, "player two's key moved player one as well")

		// And player one's key must move player one only.
		input.keyDown(Input.Keys.DOWN)
		input.keyUp(Input.Keys.DOWN)
		assertSame(leftBottom, helper.focusedActor.value, "player one's key did not move player one")
		assertSame(rightBottom, second.focusedActor.value, "player one's key moved player two as well")
	}

	@Test
	fun `a second player's confirm does not reach player one`() {
		// The constraint that would have derailed this: synthesis re-emits the canonical key for any bound alias, so
		// without scoping it to player one, player two's confirm arrives at player one's cursor as ENTER and both act.
		val profiles = MetaUiInputProfiles()
		val two = MetaPlayer(1)
		profiles[two].setKeyboardKeys(MetaUiAction.CONFIRM, Input.Keys.SHIFT_RIGHT)
		val second = UiControlHelper(two, profiles[two])

		val root = testRoot()
		val playerOnesButton = button(0f, 0f)
		val playerTwosButton = button(300f, 0f)
		var playerOneClicks = 0
		var playerTwoClicks = 0
		playerOnesButton.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				playerOneClicks++
			}
		})
		playerTwosButton.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				playerTwoClicks++
			}
		})
		root.addActor(playerOnesButton)
		root.addActor(playerTwosButton)
		stage.addActor(root)
		helper.focusFirstIn(root, playerOnesButton)
		second.focusFirstIn(root, playerTwosButton)

		input.keyDown(Input.Keys.SHIFT_RIGHT)
		input.keyUp(Input.Keys.SHIFT_RIGHT)

		assertEquals(1, playerTwoClicks, "player two's confirm did not activate player two's control")
		assertEquals(0, playerOneClicks, "player two's confirm also activated player one's control")
	}

	@Test
	fun `only player one drives the renderer's focused actor`() {
		// One slot, so two writers would trample. A second cursor is shown by cells reading its own focusedActor.
		val profiles = MetaUiInputProfiles()
		val two = MetaPlayer(1)
		val second = UiControlHelper(two, profiles[two])
		val mine = button(0f, 0f)
		val theirs = button(300f, 0f)
		stage.addActor(mine)
		stage.addActor(theirs)

		helper.focusFromPointer(mine)
		second.focusFromPointer(theirs)

		assertSame(theirs, second.focusedActor.value, "player two's own cursor did not move")
		assertSame(mine, renderer.currentFocusedActor, "player two overwrote the renderer's focused actor")
	}

	@Test
	fun `a helper given no bindings still uses the injected singleton`() {
		// The no-argument path every existing consumer takes, asserted rather than assumed.
		val fresh = UiControlHelper()
		val root = testRoot()
		val top = button(0f, 100f)
		val bottom = button(0f, 0f)
		root.addActor(top)
		root.addActor(bottom)
		stage.addActor(root)
		fresh.focusFirstIn(root, top)

		bindings.setKeyboardKeys(MetaUiAction.NAVIGATE_DOWN, Input.Keys.J)
		input.keyDown(Input.Keys.J)
		input.keyUp(Input.Keys.J)

		assertSame(bottom, fresh.focusedActor.value, "a default helper did not follow the injected bindings")
	}

	@Test
	fun `a disposed helper stops reacting and lets go of what it held`() {
		// Only mattered once a second cursor became a second instance: a recreated screen would leave its old helper
		// registered with MetaInput, still holding its focusedRoot, and that player's keys would drive both.
		val profiles = MetaUiInputProfiles()
		val two = MetaPlayer(1)
		profiles[two].setKeyboardKeys(MetaUiAction.NAVIGATE_DOWN, Input.Keys.S)
		val second = UiControlHelper(two, profiles[two])
		val root = testRoot()
		val top = button(0f, 100f)
		val bottom = button(0f, 0f)
		root.addActor(top)
		root.addActor(bottom)
		stage.addActor(root)
		second.focusFirstIn(root, top)

		second.dispose()
		input.keyDown(Input.Keys.S)
		input.keyUp(Input.Keys.S)

		assertNull(second.focusedActor.value, "a disposed helper kept hold of its focused actor")
		assertNotSame(bottom, second.selectedActor, "a disposed helper still navigated on its player's key")
	}

	@Test
	fun `disposing twice is harmless`() {
		val second = UiControlHelper(MetaPlayer(1), MetaUiInputProfiles()[MetaPlayer(1)])

		second.dispose()
		second.dispose()
	}

	@Test
	fun `conflicting keys between two profiles are reported`() {
		// The registry hands out live mutable bindings, so it cannot prevent a collision -- but a game committing a
		// rebind can ask, and both cursors moving on one press is otherwise silent.
		val profiles = MetaUiInputProfiles()
		val two = MetaPlayer(1)
		profiles[two].setKeyboardKeys(MetaUiAction.NAVIGATE_DOWN, Input.Keys.DOWN, Input.Keys.S)

		val shared = profiles.conflictingKeys(two, MetaPlayer.ONE)

		assertEquals(1, shared.size, "expected exactly the shared key, got ${shared.toList()}")
		assertEquals(Input.Keys.DOWN, shared[0], "DOWN is bound for both players and was not reported")
	}

	@Test
	fun `disjoint profiles report no conflicts`() {
		val profiles = MetaUiInputProfiles()
		val two = MetaPlayer(1)
		profiles[two].setKeyboardKeys(MetaUiAction.NAVIGATE_DOWN, Input.Keys.S)

		assertEquals(0, profiles.conflictingKeys(two, MetaPlayer.ONE).size, "disjoint key sets reported a conflict")
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
	fun `focused semantic control consumes direction before spatial navigation`() {
		val root = testRoot()
		val semantic = SemanticControl().apply { setBounds(0f, 0f, 80f, 32f) }
		val neighbour = button(100f, 0f)
		root.addActor(semantic)
		root.addActor(neighbour)
		stage.addActor(root)

		helper.focusFirstIn(root, semantic)
		input.keyDown(Input.Keys.RIGHT)
		input.keyUp(Input.Keys.RIGHT)

		assertEquals(1, semantic.rightActions)
		assertSame(semantic, helper.focusedActor.value)
	}

	@Test
	fun `pointer focus resolves nested content to its navigable parent`() {
		val button = button(0f, 0f)
		val child = Actor().apply { setBounds(0f, 0f, 16f, 16f) }
		button.addActor(child)
		stage.addActor(button)

		helper.focusFromPointer(child)

		assertSame(button, helper.focusedActor.value)
	}

	@Test
	fun `stage scroll focus chooses deepest nested MetaScrollPane and restores its parent`() {
		val outerContent = Group().apply { setBounds(0f, 0f, 200f, 400f) }
		val innerContent = Actor().apply { setBounds(0f, 0f, 100f, 300f) }
		val outer = MetaScrollPane(null, ScrollPane.ScrollPaneStyle())
		val inner = MetaScrollPane(null, ScrollPane.ScrollPaneStyle()).apply { setBounds(50f, 50f, 100f, 100f) }
		outer.setActor(outerContent)
		inner.setActor(innerContent)
		outerContent.addActor(inner)
		outer.setBounds(0f, 0f, 200f, 200f)
		stage.addActor(outer)
		outer.validate()
		inner.setBounds(50f, 50f, 100f, 100f)
		inner.validate()

		updateMetaScrollFocus(stage, outerContent)
		assertSame(outer, stage.scrollFocus)

		updateMetaScrollFocus(stage, innerContent)
		assertSame(inner, stage.scrollFocus)

		updateMetaScrollFocus(stage, outerContent)
		assertSame(outer, stage.scrollFocus)

		updateMetaScrollFocus(stage, null)
		assertNull(stage.scrollFocus)
	}

	@Test
	fun `stage scroll focus treats a child button as part of its MetaScrollPane`() {
		val content = Group().apply { setBounds(0f, 0f, 200f, 300f) }
		val childButton = Button().apply { setBounds(20f, 120f, 120f, 32f) }
		val pane = MetaScrollPane(null, ScrollPane.ScrollPaneStyle()).apply { setBounds(0f, 0f, 200f, 180f) }
		content.addActor(childButton)
		pane.setActor(content)
		stage.addActor(pane)
		pane.validate()

		updateMetaScrollFocus(stage, content)
		assertSame(pane, stage.scrollFocus)

		updateMetaScrollFocus(stage, childButton)
		assertSame(pane, stage.scrollFocus)
	}

	@Test
	fun `stage scroll focus skips a nested pane with no range and chooses scrollable outer pane`() {
		val outerContent = Group().apply { setBounds(0f, 0f, 200f, 300f) }
		val innerContent = Group().apply { setBounds(0f, 0f, 160f, 120f) }
		val childButton = Button().apply { setBounds(20f, 40f, 120f, 32f) }
		val outer = MetaScrollPane(null, ScrollPane.ScrollPaneStyle()).apply { setBounds(0f, 0f, 200f, 180f) }
		val inner = MetaScrollPane(null, ScrollPane.ScrollPaneStyle()).apply { setBounds(10f, 100f, 160f, 120f) }
		innerContent.addActor(childButton)
		inner.setActor(innerContent)
		outerContent.addActor(inner)
		outer.setActor(outerContent)
		stage.addActor(outer)
		outer.validate()
		inner.validate()

		assertEquals(0f, inner.maxY)
		assertTrue(outer.maxY > 0f)
		updateMetaScrollFocus(stage, childButton)
		assertSame(outer, stage.scrollFocus)
	}

	@Test
	fun `shift wheel horizontal scrolling follows browser direction and clamps`() {
		assertEquals(100f, shiftedHorizontalScrollPosition(0f, 300f, amountY = 1f))
		assertEquals(50f, shiftedHorizontalScrollPosition(150f, 300f, amountY = -1f))
		assertEquals(300f, shiftedHorizontalScrollPosition(250f, 300f, amountY = 1f))
		assertEquals(0f, shiftedHorizontalScrollPosition(50f, 300f, amountY = -1f))
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
	fun `arrow keys stay with text editing focus instead of moving UI selection`() {
		val root = testRoot()
		val textField = TextField("", TextField.TextFieldStyle(headlessFont(), Color.WHITE, null, null, null))
			.apply { setBounds(0f, 60f, 120f, 24f) }
		val nextButton = button(0f, 0f)
		root.addActor(textField)
		root.addActor(nextButton)
		stage.addActor(root)
		helper.focusFirstIn(root, textField)
		stage.keyboardFocus = textField

		input.keyDown(Input.Keys.DOWN)
		input.keyUp(Input.Keys.DOWN)

		assertSame(textField, stage.keyboardFocus)
		assertSame(textField, helper.focusedActor.value)
	}

	@Test
	fun `confirm does not activate another UI control while a text field owns keyboard focus`() {
		val root = testRoot()
		val textField = TextField("", TextField.TextFieldStyle(headlessFont(), Color.WHITE, null, null, null))
			.apply { setBounds(0f, 60f, 120f, 24f) }
		val button = button(0f, 0f)
		var clicks = 0
		button.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				clicks++
			}
		})
		root.addActor(textField)
		root.addActor(button)
		stage.addActor(root)
		helper.selectedActor = button
		stage.keyboardFocus = textField

		input.keyDown(Input.Keys.ENTER)
		input.keyUp(Input.Keys.ENTER)

		assertEquals(0, clicks)
		assertSame(textField, stage.keyboardFocus)
		assertSame(button, helper.focusedActor.value)
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

	private class SemanticControl : Actor(), MetaFocusable, MetaUiActionHandler {
		var rightActions = 0

		override fun setMetaFocused(focused: Boolean) = Unit

		override fun handleMetaUiAction(action: MetaUiAction): Boolean {
			if (action != MetaUiAction.NAVIGATE_RIGHT) return false
			rightActions++
			return true
		}
	}

	private fun testRoot(): Group =
		Group().apply { setBounds(0f, 0f, 800f, 600f) }

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
