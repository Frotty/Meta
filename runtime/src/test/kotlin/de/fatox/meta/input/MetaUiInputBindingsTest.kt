package de.fatox.meta.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.ControllerMapping
import com.badlogic.gdx.controllers.ControllerPowerLevel
import com.badlogic.gdx.utils.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

internal class MetaUiInputBindingsTest {
	@Test
	fun `keyboard UI bindings can be customized`() {
		val bindings = MetaUiInputBindings()

		bindings.setKeyboardKeys(MetaUiAction.CONFIRM, Input.Keys.SPACE)

		assertEquals(MetaUiAction.CONFIRM, bindings.actionForKey(Input.Keys.SPACE))
		assertNull(bindings.actionForKey(Input.Keys.ENTER))
		assertEquals(Input.Keys.ENTER, bindings.canonicalKeyFor(MetaUiAction.CONFIRM))
	}

	@Test
	fun `raw controller button bindings can be customized`() {
		val bindings = MetaUiInputBindings()
		val controller = FakeController()

		bindings.setControllerButtonCodes(MetaUiAction.BACK, 42)

		assertEquals(MetaUiAction.BACK, bindings.actionForButton(controller, 42))
		assertNull(bindings.actionForButton(controller, controller.mapping.buttonB))
		assertEquals(Input.Keys.ESCAPE, bindings.canonicalKeyFor(MetaUiAction.BACK))
	}

	@Test
	fun `semantic controller button bindings follow controller mapping`() {
		val bindings = MetaUiInputBindings()
		val controller = FakeController()

		assertEquals(MetaUiAction.CONFIRM, bindings.actionForButton(controller, controller.mapping.buttonA))
		assertEquals(MetaUiAction.BACK, bindings.actionForButton(controller, controller.mapping.buttonBack))
	}

	@Test
	fun `profiles round trip custom keyboard controller and axis settings`() {
		val bindings = MetaUiInputBindings()
		val controller = FakeController()
		bindings.horizontalAxis = 2
		bindings.verticalAxis = 3
		bindings.axisNavigationEnabled = false
		bindings.setKeyboardKeys(MetaUiAction.BACK, Input.Keys.BACKSPACE)
		bindings.setControllerBindings(
			MetaUiAction.CONFIRM,
			MetaControllerButtonBinding.semantic(MetaControllerButton.Y),
			MetaControllerButtonBinding.raw(42),
		)

		val json = Json()
		val profile = json.fromJson(MetaUiInputProfile::class.java, json.toJson(bindings.toProfile()))
		val restored = MetaUiInputBindings().applyProfile(profile)

		assertEquals(2, restored.horizontalAxis)
		assertEquals(3, restored.verticalAxis)
		assertFalse(restored.axisNavigationEnabled)
		assertEquals(MetaUiAction.BACK, restored.actionForKey(Input.Keys.BACKSPACE))
		assertNull(restored.actionForKey(Input.Keys.ESCAPE))
		assertEquals(MetaUiAction.CONFIRM, restored.actionForButton(controller, controller.mapping.buttonY))
		assertEquals(MetaUiAction.CONFIRM, restored.actionForButton(controller, 42))
		assertNull(restored.actionForButton(controller, controller.mapping.buttonA))
	}

	@Test
	fun `partial profiles keep defaults for missing actions`() {
		val profile = MetaUiInputProfile().apply {
			keyboardBindings = arrayOf(MetaUiKeyboardBindingProfile(MetaUiAction.BACK, intArrayOf(Input.Keys.BACKSPACE)))
		}

		val restored = MetaUiInputBindings().applyProfile(profile)

		assertEquals(MetaUiAction.BACK, restored.actionForKey(Input.Keys.BACKSPACE))
		assertEquals(MetaUiAction.CONFIRM, restored.actionForKey(Input.Keys.ENTER))
		assertEquals(MetaUiAction.NAVIGATE_UP, restored.actionForKey(Input.Keys.UP))
	}

	private class FakeMapping : ControllerMapping(
		0, 1, 2, 3,
		10, 11, 12, 13,
		14, 15, 16, 17,
		18, 19, 20, 21,
		22, 23, 24, 25,
	)

	private class FakeController : Controller {
		private val mapping = FakeMapping()

		override fun getButton(buttonCode: Int): Boolean = false
		override fun getAxis(axisCode: Int): Float = 0f
		override fun getName(): String = "fake"
		override fun getUniqueId(): String = "fake"
		override fun getMinButtonIndex(): Int = 0
		override fun getMaxButtonIndex(): Int = 25
		override fun getAxisCount(): Int = 4
		override fun isConnected(): Boolean = true
		override fun canVibrate(): Boolean = false
		override fun isVibrating(): Boolean = false
		override fun startVibration(duration: Int, strength: Float) = Unit
		override fun cancelVibration() = Unit
		override fun supportsPlayerIndex(): Boolean = false
		override fun getPlayerIndex(): Int = Controller.PLAYER_IDX_UNSET
		override fun setPlayerIndex(index: Int) = Unit
		override fun getMapping(): ControllerMapping = mapping
		override fun getPowerLevel(): ControllerPowerLevel = ControllerPowerLevel.POWER_UNKNOWN
		override fun addListener(listener: ControllerListener) = Unit
		override fun removeListener(listener: ControllerListener) = Unit
	}
}
