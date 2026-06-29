package de.fatox.meta.ui.layout

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import de.fatox.meta.test.GdxTestEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Headless tests for [MetaLayout] - pure scene2d geometry, no GL/Gdx. Uses fixed-size stand-in widgets in place of
 * real (font-measured) content so the checks are deterministic without a rasterized font.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class MetaLayoutTest {

	@BeforeEach
	fun setUp() = GdxTestEnvironment.ensure() // Table/Cell need Gdx.files to be present

	/** A widget with an explicit preferred size; [shrinkable] lets a table squeeze it below pref (to test clipping). */
	private class FixedWidget(
		private val w: Float,
		private val h: Float,
		private val shrinkable: Boolean = false,
	) : Widget() {
		override fun getPrefWidth() = w
		override fun getPrefHeight() = h
		override fun getMinWidth() = if (shrinkable) 0f else w
		override fun getMinHeight() = if (shrinkable) 0f else h
	}

	@Test
	fun `a table sized to its content has no problems`() {
		val table = Table().apply {
			add(FixedWidget(100f, 20f))
			add(FixedWidget(60f, 20f))
			pack() // size to preferred
		}
		assertEquals(emptyList(), MetaLayout.problems(table))
	}

	@Test
	fun `a child positioned outside its parent is reported as overflow`() {
		val group = Group().apply { setSize(50f, 50f) }
		group.addActor(Actor().apply { setBounds(40f, 40f, 30f, 30f) }) // extends to 70x70

		val problems = MetaLayout.problems(group)
		assertTrue(problems.any { it.kind == MetaLayout.Kind.OVERFLOW }, "expected overflow, got $problems")
		assertFailsWith<AssertionError> { MetaLayout.assertValid(group) }
	}

	@Test
	fun `content squeezed below its preferred size is reported as clipped`() {
		val table = Table().apply {
			add(FixedWidget(100f, 20f, shrinkable = true))
			setSize(40f, 20f) // narrower than the child's preferred width
			validate()
		}
		val problems = MetaLayout.problems(table)
		assertTrue(problems.any { it.kind == MetaLayout.Kind.CLIPPED }, "expected clipped, got $problems")
	}

	@Test
	fun `clipping check can be disabled`() {
		val table = Table().apply {
			add(FixedWidget(100f, 20f, shrinkable = true))
			setSize(40f, 20f)
			validate()
		}
		// With clipping off and the child fitting horizontally within the table box, there is nothing to report.
		assertEquals(emptyList(), MetaLayout.problems(table, checkClipping = false))
	}

	@Test
	fun `assertValid passes for a well-formed nested layout`() {
		val root = Table().apply {
			add(FixedWidget(80f, 20f)).row()
			add(Table().apply {
				add(FixedWidget(30f, 10f))
				add(FixedWidget(30f, 10f))
			})
			pack()
		}
		MetaLayout.assertValid(root) // should not throw
	}

	@Test
	fun `the problem report names the offending actor path`() {
		val group = Group().apply { setSize(10f, 10f) }
		group.addActor(Actor().apply { setBounds(0f, 0f, 50f, 50f) })

		val error = assertFailsWith<AssertionError> { MetaLayout.assertValid(group) }
		assertTrue(error.message!!.contains("Group>Actor"), "message should include the actor path: ${error.message}")
	}
}
