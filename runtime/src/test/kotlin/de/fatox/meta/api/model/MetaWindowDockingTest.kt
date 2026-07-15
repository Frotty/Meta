package de.fatox.meta.api.ui

import com.badlogic.gdx.utils.Json
import de.fatox.meta.ui.MetaUiManager
import de.fatox.meta.ui.windows.MetaWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MetaWindowDockingTest {
	@Test
	fun `sidebar minimum respects both framework default and every docked window`() {
		assertNull(resolveDockMinimum(180f, emptyList()))
		assertEquals(180f, resolveDockMinimum(180f, listOf(120f, 160f)))
		assertEquals(312f, resolveDockMinimum(180f, listOf(220f, 312f, 280f)))
	}

	@Test
	fun `narrow resize stops at the widest docked window content minimum`() {
		val config = MetaDockConfig(
			leftWidth = 300f,
			rightWidth = 400f,
			minimumSidebarWidth = 240f,
			minimumCenterWidth = 420f,
		)
		val leftMinimum = resolveDockMinimum(config.minimumSidebarWidth, listOf(110f, 180f))
		val rightMinimum = resolveDockMinimum(config.minimumSidebarWidth, listOf(220f, 280f, 250f))
		val widths = resolveDockWidths(
			viewportWidth = 1600f,
			config = config,
			leftMinimumWidth = leftMinimum,
			rightMinimumWidth = rightMinimum,
			desiredLeftWidth = 260f,
			desiredRightWidth = 120f,
		)

		assertEquals(260f, widths.left)
		assertEquals(280f, widths.right)
		assertEquals(
			280f,
			resolveDockWidthForSide(1600f, config, MetaDockSide.RIGHT, leftMinimum, rightMinimum, 260f, 120f),
		)
	}

	@Test
	fun `new docking hot paths have no fragile enum mapping companions`() {
		assertNoEnumMappingDependency(MetaWindow::class.java)
		assertNoEnumMappingDependency(MetaUiManager::class.java)
		assertNoEnumMappingDependency(Class.forName("de.fatox.meta.api.ui.MetaWindowDockingKt"))
	}

	private fun assertNoEnumMappingDependency(type: Class<*>) {
		val resource = "/${type.name.replace('.', '/')}.class"
		val bytecode = checkNotNull(type.getResourceAsStream(resource)) { resource }.use { it.readBytes() }
		val marker = type.name.replace('.', '/') + "\$WhenMappings"
		assertFalse(
			bytecode.toString(Charsets.ISO_8859_1).contains(marker),
			"${type.simpleName} must not depend on separately generated enum mapping bytecode",
		)
	}

	@Test
	fun `fixed and fill panels form a gap-separated left sidebar`() {
		val bounds = calculateDockBounds(
			1600f, 900f,
			MetaDockConfig(leftWidth = 280f, margin = 8f, gap = 6f, topInset = 40f, bottomInset = 48f),
			MetaDockSide.LEFT,
			listOf(
				MetaDockItem("brushes", 0, 120f, 80f, 150f, fill = false),
				MetaDockItem("tiles", 100, 120f, 120f, 300f, fill = true),
			),
		)

		assertEquals(MetaDockBounds(8f, 710f, 280f, 150f), bounds.getValue("brushes"))
		assertEquals(MetaDockBounds(8f, 48f, 280f, 656f), bounds.getValue("tiles"))
	}

	@Test
	fun `right sidebar is anchored to viewport edge`() {
		val bounds = calculateDockBounds(
			1200f, 800f,
			MetaDockConfig(rightWidth = 320f, margin = 10f, topInset = 30f, bottomInset = 40f),
			MetaDockSide.RIGHT,
			listOf(MetaDockItem("overview", 0, 120f, 100f, 100f, fill = true)),
		)

		assertEquals(MetaDockBounds(870f, 40f, 320f, 730f), bounds.getValue("overview"))
	}

	@Test
	fun `edge detection ignores windows outside the snap zones`() {
		assertEquals(MetaDockSide.LEFT, detectDockSide(16f, 280f, 1200f, 24f))
		assertEquals(MetaDockSide.RIGHT, detectDockSide(896f, 280f, 1200f, 24f))
		assertEquals(MetaDockSide.LEFT, detectDockSide(-10f, 280f, 1200f, 24f))
		assertNull(detectDockSide(-100f, 280f, 1200f, 24f))
		assertNull(detectDockSide(300f, 280f, 1200f, 24f))
	}

	@Test
	fun `nearest edge wins when a wide window touches both snap zones`() {
		assertEquals(MetaDockSide.LEFT, detectDockSide(0f, 1200f, 1200f, 24f))
		assertEquals(MetaDockSide.RIGHT, detectDockSide(-20f, 1220f, 1200f, 24f))
	}

	@Test
	fun `configured width shrinks to viewport while respecting achievable minimum`() {
		val bounds = calculateDockBounds(
			260f, 180f,
			MetaDockConfig(rightWidth = 340f, margin = 8f, gap = 6f, topInset = 40f, bottomInset = 48f),
			MetaDockSide.RIGHT,
			listOf(MetaDockItem("one", 0, 100f, 100f, 100f, fill = false)),
		).getValue("one")

		assertEquals(8f, bounds.x)
		assertEquals(244f, bounds.width)
		assertEquals(40f, bounds.y)
		assertEquals(100f, bounds.height)
	}

	@Test
	fun `minimum heights never collapse or overlap in an impossibly short viewport`() {
		val bounds = calculateDockBounds(
			500f, 180f,
			MetaDockConfig(leftWidth = 200f, margin = 8f, gap = 6f, topInset = 40f, bottomInset = 48f),
			MetaDockSide.LEFT,
			listOf(
				MetaDockItem("top", 0, 100f, 80f, 120f, fill = false),
				MetaDockItem("bottom", 100, 100f, 80f, 120f, fill = false),
			),
		)

		val top = bounds.getValue("top")
		val bottom = bounds.getValue("bottom")
		assertEquals(80f, top.height)
		assertEquals(80f, bottom.height)
		assertEquals(6f, top.y - (bottom.y + bottom.height))
		assertTrue(bottom.y < 48f, "An impossible stack should overflow predictably instead of overlap")
	}

	@Test
	fun `window minimum width can enlarge a configured sidebar`() {
		val bounds = calculateDockBounds(
			1200f, 800f,
			MetaDockConfig(leftWidth = 200f),
			MetaDockSide.LEFT,
			listOf(MetaDockItem("wide", 0, 280f, 100f, 100f, fill = true)),
		).getValue("wide")

		assertEquals(280f, bounds.width)
	}

	@Test
	fun `dock preview uses the same width and insets as final layout`() {
		val config = MetaDockConfig(rightWidth = 320f, margin = 10f, topInset = 30f, bottomInset = 40f)
		assertEquals(
			MetaDockBounds(870f, 40f, 320f, 730f),
			dockZoneBounds(1200f, 800f, config, MetaDockSide.RIGHT, minimumWidth = 200f),
		)
	}

	@Test
	fun `two sidebars shrink proportionally to preserve the center workspace`() {
		val widths = resolveDockWidths(
			viewportWidth = 800f,
			config = MetaDockConfig(leftWidth = 300f, rightWidth = 340f, margin = 8f, minimumCenterWidth = 320f),
			leftMinimumWidth = 120f,
			rightMinimumWidth = 120f,
		)
		val left = assertNotNull(widths.left)
		val right = assertNotNull(widths.right)

		assertEquals(464f, left + right)
		assertEquals(320f, 800f - 16f - left - right)
		assertTrue(left >= 120f)
		assertTrue(right >= 120f)
		assertEquals(
			left,
			resolveDockWidthForSide(800f, MetaDockConfig(), MetaDockSide.LEFT, 120f, 120f),
		)
		assertEquals(
			right,
			resolveDockWidthForSide(800f, MetaDockConfig(), MetaDockSide.RIGHT, 120f, 120f),
		)
	}

	@Test
	fun `single sidebar also preserves the configured center workspace`() {
		assertEquals(
			MetaDockWidths(left = 264f, right = null),
			resolveDockWidths(
				viewportWidth = 600f,
				config = MetaDockConfig(leftWidth = 300f, margin = 8f, minimumCenterWidth = 320f),
				leftMinimumWidth = 120f,
				rightMinimumWidth = null,
			),
		)
	}

	@Test
	fun `user selected sidebar width overrides its configured default`() {
		assertEquals(
			MetaDockWidths(left = 224f, right = null),
			resolveDockWidths(
				viewportWidth = 1200f,
				config = MetaDockConfig(leftWidth = 300f, minimumSidebarWidth = 180f),
				leftMinimumWidth = 180f,
				rightMinimumWidth = null,
				desiredLeftWidth = 224f,
			),
		)
	}

	@Test
	fun `user selected width is clamped without sacrificing center workspace`() {
		assertEquals(
			MetaDockWidths(left = 264f, right = null),
			resolveDockWidths(
				viewportWidth = 600f,
				config = MetaDockConfig(leftWidth = 300f, margin = 8f, minimumCenterWidth = 320f),
				leftMinimumWidth = 180f,
				rightMinimumWidth = null,
				desiredLeftWidth = 500f,
			),
		)
	}

	@Test
	fun `dock widths survive persistence round trip`() {
		val json = Json()
		val restored = json.fromJson(
			MetaDockLayoutData::class.java,
			json.toJson(MetaDockLayoutData(leftWidth = 218f, rightWidth = 356f)),
		)

		assertEquals(218f, restored.leftWidth)
		assertEquals(356f, restored.rightWidth)
	}

	@Test
	fun `closing a fixed panel lets the fill panel reclaim its space`() {
		val config = MetaDockConfig(topInset = 40f, bottomInset = 40f, gap = 8f)
		val withFixed = calculateDockBounds(
			1200f, 800f, config, MetaDockSide.LEFT,
			listOf(
				MetaDockItem("fixed", 0, 100f, 100f, 160f, fill = false),
				MetaDockItem("fill", 100, 100f, 100f, 100f, fill = true),
			),
		).getValue("fill")
		val afterClose = calculateDockBounds(
			1200f, 800f, config, MetaDockSide.LEFT,
			listOf(MetaDockItem("fill", 100, 100f, 100f, 100f, fill = true)),
		).getValue("fill")

		assertEquals(552f, withFixed.height)
		assertEquals(720f, afterClose.height)
	}

	@Test
	fun `invalid docking geometry is rejected at the API boundary`() {
		assertFailsWith<IllegalArgumentException> { MetaDockConfig(leftWidth = 0f) }
		assertFailsWith<IllegalArgumentException> { MetaDockConfig(gap = -1f) }
		assertFailsWith<IllegalArgumentException> { MetaDockConfig(snapDistance = Float.NaN) }
		assertFailsWith<IllegalArgumentException> { MetaDockConfig(rightWidth = Float.POSITIVE_INFINITY) }
		assertFailsWith<IllegalArgumentException> { MetaDockConfig(minimumCenterWidth = -1f) }
		assertFailsWith<IllegalArgumentException> { MetaDockConfig(minimumSidebarWidth = 0f) }
	}

	@Test
	fun `programmatic updates preserve dock state and floating bounds`() {
		assertEquals(
			MetaDockUpdate(MetaDockSide.LEFT, updateFloatingBounds = false, updateDockHeight = false),
			resolveDockUpdate(MetaDockSide.LEFT, MetaWindowInteraction.PROGRAMMATIC, detectedSide = null),
		)
		assertEquals(
			MetaDockUpdate(null, updateFloatingBounds = true, updateDockHeight = false),
			resolveDockUpdate(null, MetaWindowInteraction.PROGRAMMATIC, detectedSide = null),
		)
	}

	@Test
	fun `move and resize updates select the correct persisted bounds`() {
		assertEquals(
			MetaDockUpdate(MetaDockSide.RIGHT, updateFloatingBounds = false, updateDockHeight = true),
			resolveDockUpdate(null, MetaWindowInteraction.MOVE, detectedSide = MetaDockSide.RIGHT),
		)
		assertEquals(
			MetaDockUpdate(null, updateFloatingBounds = true, updateDockHeight = false),
			resolveDockUpdate(MetaDockSide.RIGHT, MetaWindowInteraction.MOVE, detectedSide = null),
		)
		assertEquals(
			MetaDockUpdate(MetaDockSide.RIGHT, updateFloatingBounds = false, updateDockHeight = true),
			resolveDockUpdate(MetaDockSide.RIGHT, MetaWindowInteraction.RESIZE, detectedSide = null),
		)
		assertEquals(
			MetaDockUpdate(MetaDockSide.RIGHT, updateFloatingBounds = false, updateDockHeight = false),
			resolveDockUpdate(MetaDockSide.RIGHT, MetaWindowInteraction.DOCK_WIDTH_RESIZE, detectedSide = null),
		)
	}

	@Test
	fun `clicking a title without movement cannot dock or reorder a window`() {
		assertEquals(
			false,
			windowGestureChanged(MetaWindowInteraction.MOVE, 10f, 20f, 300f, 200f, 10f, 20f, 300f, 200f),
		)
		assertEquals(
			false,
			windowGestureChanged(MetaWindowInteraction.RESIZE, 10f, 20f, 300f, 200f, 10f, 20f, 300f, 200f),
		)
		assertTrue(windowGestureChanged(MetaWindowInteraction.MOVE, 10f, 20f, 300f, 200f, 11f, 20f, 300f, 200f))
		assertTrue(windowGestureChanged(MetaWindowInteraction.RESIZE, 10f, 20f, 300f, 200f, 10f, 20f, 301f, 200f))
		assertTrue(
			windowGestureChanged(
				MetaWindowInteraction.DOCK_WIDTH_RESIZE,
				10f, 20f, 300f, 200f,
				10f, 20f, 301f, 200f,
			),
		)
	}

	@Test
	fun `docked resize affordance is only available on fixed panel divider`() {
		assertTrue(dockedPanelCanResizeHeight(fill = false, localY = 4f, edgeSize = 8f))
		assertEquals(false, dockedPanelCanResizeHeight(fill = false, localY = 12f, edgeSize = 8f))
		assertEquals(false, dockedPanelCanResizeHeight(fill = true, localY = 4f, edgeSize = 8f))
	}

	@Test
	fun `dock ordering supports top middle and bottom insertion`() {
		val existing = listOf(
			MetaDockOrderItem("a", 0, 700f),
			MetaDockOrderItem("b", 100, 500f),
		)

		assertEquals(-100, calculateDockOrder(800f, existing).order)
		assertEquals(50, calculateDockOrder(600f, existing).order)
		assertEquals(200, calculateDockOrder(400f, existing).order)
	}

	@Test
	fun `exhausted order gaps are normalized without duplicate order values`() {
		val update = calculateDockOrder(
			movingCenterY = 600f,
			existing = listOf(
				MetaDockOrderItem("a", 10, 700f),
				MetaDockOrderItem("b", 11, 500f),
			),
		)

		assertEquals(100, update.order)
		assertEquals(mapOf("a" to 0, "b" to 200), update.normalizedOrders)
		assertEquals(3, (update.normalizedOrders.values + update.order).distinct().size)
	}
}
