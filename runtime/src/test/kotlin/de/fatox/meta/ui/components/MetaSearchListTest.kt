package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Container
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.signal
import de.fatox.meta.test.MetaHeadlessUi
import de.fatox.meta.ui.tabs.MetaTab
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MetaSearchListTest {
	@BeforeEach fun setUp() = MetaHeadlessUi.install()
	@AfterEach fun tearDown() = MetaHeadlessUi.dispose()

	@Test
	fun `label highlights preserve glyph geometry and literal brackets and follow text changes`() {
		val label = MetaLabel("[AV] AV av", color = Color.GRAY).apply {
			setWrap(true)
			setSize(55f, 90f)
			validate()
		}
		val normal = label.geometry()
		label.setHighlight("a", Color.YELLOW)
		label.validate()

		assertEquals(normal, label.geometry())
		assertEquals("[AV] AV av", label.text.toString())
		assertEquals(3, label.highlightCount)
		assertEquals(Color.WHITE.toIntBits(), label.glyphLayout.colors[1])

		label.setText("Visual")
		label.validate()
		assertEquals(1, label.highlightCount)
		label.setHighlight("")
		label.validate()
		assertEquals(0, label.highlightCount)
	}

	@Test
	fun `reactive search collapses nonmatches and preserves mounted controls`() {
		val stage = Stage()
		val scope = ReactiveScope()
		try {
			val first = MetaLabel("Display mode")
			val second = MetaLabel("Audio volume")
			val list = MetaSearchList(gap = 10f).apply {
				addEntry(first, first, keywords = listOf("fullscreen"))
				addEntry(second, second)
				setSize(200f, 200f)
			}
			stage.addActor(list)
			val firstRow = first.parent as MetaHighlight
			val secondRow = second.parent as MetaHighlight
			val totalHeight = list.prefHeight
			val query = signal("")
			list.bindQuery(scope) { query() }
			stage.keyboardFocus = second
			var touchCancelled = false
			stage.addTouchFocus({ event ->
				touchCancelled = (event as InputEvent).isTouchFocusCancel
				true
			}, second, second, 0, 0)

			query.value = "  FULLSCREEN "
			assertTrue(firstRow.highlighted)
			assertFalse(secondRow.isVisible)
			assertNull(stage.keyboardFocus)
			assertTrue(touchCancelled)
			assertSame(stage, second.stage)
			assertEquals(firstRow.prefHeight, list.prefHeight)
			firstRow.setSize(200f, firstRow.prefHeight)
			firstRow.validate()
			assertSame(first, firstRow.hit(10f, 5f, true))

			query.value = "mode"
			assertFalse(firstRow.highlighted)
			first.validate()
			assertEquals(1, first.highlightCount)

			query.value = "missing"
			assertEquals(0f, list.prefHeight)
			query.value = " "
			assertTrue(secondRow.isVisible)
			assertEquals(totalHeight, list.prefHeight)

			scope.dispose()
			query.value = "missing"
			assertTrue(secondRow.isVisible)
		} finally {
			scope.dispose()
			stage.dispose()
		}
	}

	@Test
	fun `clicking a search result opens its tab scrolls to the setting and briefly outlines it`() {
		val stage = Stage()
		val scope = ReactiveScope()
		try {
			val start = object : MetaTab(isCloseableByUser = false) {
				override val tabTitle = "Display"
				override val contentTable = MetaTable()
			}
			val destination = object : MetaTab(isCloseableByUser = false) {
				override val tabTitle = "Controls"
				override val contentTable = MetaTable().apply { top() }
			}
			val tabs = MetaTabbedPane().apply { add(start); add(destination) }
			val target = MetaHighlight(MetaLabel("Confirm changes"))
			destination.contentTable.add(MetaLabel("First setting")).height(600f).row()
			destination.contentTable.add(target).growX().height(32f).row()
			val pane = MetaScrollPane(destination.contentTable).apply { setScrollingDisabled(true, false) }
			val outerContent = MetaTable().apply {
				add(MetaLabel("Tab heading")).height(180f).row()
				add(pane).growX().height(110f)
			}
			val outerPane = MetaScrollPane(outerContent).apply { setScrollingDisabled(true, false) }
			val body = Container<Actor>().fill().apply { setSize(300f, 150f) }
			stage.addActor(body)
			val search = MetaSettingsSearch()
			val query = signal("  TOGGLE ")
			search.submit(listOf(
				MetaSettingsSearchEntry("Brightness", start.tabTitle) { error("Unmatched result activated") },
				MetaSettingsSearchEntry("Confirm changes", destination.tabTitle, listOf("toggle")) {
					tabs.switchTab(destination)
					query.value = ""
					target
				},
			))
			search.bindQuery(scope) { query() }
			scope.effect {
				body.actor = when {
					query().isNotBlank() -> search
					tabs.activeMetaTabValue() === destination -> outerPane
					else -> start.contentTable
				}
			}
			body.validate()
			val results = search.children.first() as MetaActionList<*>
			assertEquals(1, results.rowsTable.children.size)
			val row = results.rowsTable.children.first()
			val x = 24f
			val y = row.height / 2f
			val position = row.localToStageCoordinates(Vector2(x, y))
			val hit = row.hit(x, y, true)
			for (type in listOf(InputEvent.Type.touchDown, InputEvent.Type.touchUp)) {
				hit.fire(InputEvent().apply {
					this.type = type
					this.stage = stage
					stageX = position.x
					stageY = position.y
					pointer = 0
					button = 0
				})
			}

			assertSame(destination, tabs.activeMetaTab)
			assertSame(outerPane, body.actor)
			assertEquals(pane.maxY, pane.scrollY, 1f)
			assertTrue(pane.scrollY > 0f)
			assertTrue(outerPane.scrollY > 0f)
			assertEquals(pane.scrollY, pane.visualScrollY)
			val targetPosition = target.localToStageCoordinates(Vector2(target.width / 2f, target.height / 2f))
			assertTrue(stage.hit(targetPosition.x, targetPosition.y, true).isDescendantOf(target))
			assertTrue(target.highlighted)
			stage.act(3f)
			assertFalse(target.highlighted)
		} finally {
			scope.dispose()
			stage.dispose()
		}
	}

	private fun MetaLabel.geometry(): List<List<Float>> = (0 until glyphLayout.runs.size).map { index ->
		val run = glyphLayout.runs[index]
		listOf(run.x, run.y, run.width) + run.xAdvances.toArray().toList()
	}
}
