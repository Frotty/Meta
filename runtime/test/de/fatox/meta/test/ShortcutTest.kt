package de.fatox.meta.test

import com.badlogic.gdx.Input

import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.input.Hotkey
import de.fatox.meta.input.MetaInput
import org.junit.jupiter.api.Test
import java.awt.Robot
import java.awt.event.KeyEvent

internal class ShortcutTest : MetaTest() {
	private val metaInput: MetaInput by lazyInject()

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