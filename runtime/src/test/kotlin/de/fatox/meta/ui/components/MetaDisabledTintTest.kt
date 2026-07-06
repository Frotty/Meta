package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import de.fatox.meta.ui.MetaColor
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetaDisabledTintTest {
	@Test
	fun `disabled tint greys out nested button content and restores original colors`() {
		val root = Group()
		val child = Actor().apply { color.set(Color.WHITE) }
		val nestedChild = Actor().apply { color.set(Color.SKY) }
		val nestedGroup = Group().apply { addActor(nestedChild) }
		root.addActor(child)
		root.addActor(nestedGroup)

		val tint = MetaDisabledTint(root)

		tint.apply(true)

		assertColor(MetaColor.TEXT_DISABLED, child.color)
		assertColor(MetaColor.TEXT_DISABLED, nestedGroup.color)
		assertColor(MetaColor.TEXT_DISABLED, nestedChild.color)

		tint.apply(false)

		assertColor(Color.WHITE, child.color)
		assertColor(Color.WHITE, nestedGroup.color)
		assertColor(Color.SKY, nestedChild.color)
	}

	private fun assertColor(expected: Color, actual: Color) {
		assertEquals(expected.r, actual.r, 0.001f)
		assertEquals(expected.g, actual.g, 0.001f)
		assertEquals(expected.b, actual.b, 0.001f)
		assertEquals(expected.a, actual.a, 0.001f)
	}
}
