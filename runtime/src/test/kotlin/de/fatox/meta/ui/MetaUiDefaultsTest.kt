package de.fatox.meta.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.kotcrab.vis.ui.VisUI
import de.fatox.meta.test.GdxTestEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetaUiDefaultsTest {
	@BeforeEach
	fun setUp() {
		GdxTestEnvironment.ensure()
		if (VisUI.isLoaded()) VisUI.dispose()
		VisUI.load(Skin())
	}

	@AfterEach
	fun tearDown() {
		if (VisUI.isLoaded()) VisUI.dispose()
	}

	@Test
	fun `content layout helpers apply the standard spacing rhythm`() {
		val row = metaRow { add(Actor()) }
		val column = metaColumn { add(Actor()) }

		assertEquals(MetaSpacing.SM, row.cells.first().spaceRight)
		assertEquals(MetaSpacing.SM, column.cells.first().spaceBottom)
	}
}
