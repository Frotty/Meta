package de.fatox.meta.test

import com.badlogic.gdx.Input
import de.fatox.meta.api.MetaInputProcessor
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.input.Hotkey
import org.junit.jupiter.api.Test
import java.awt.Robot
import java.awt.event.KeyEvent

internal class ShortcutTest : MetaTest() {
	private val metaInput: MetaInputProcessor by lazyInject()

	class TestShortcutClass {
		@Hotkey(keycodes = [Input.Keys.CONTROL_LEFT, Input.Keys.D])
		fun doSomeShit() {
			println("okies")
		}

		init {

		}
	}

	@Test
	fun testSimple() {
		TestShortcutClass()
		val robot = Robot()
		robot.keyPress(KeyEvent.VK_CONTROL)
		robot.keyPress(KeyEvent.VK_D)
	}
}