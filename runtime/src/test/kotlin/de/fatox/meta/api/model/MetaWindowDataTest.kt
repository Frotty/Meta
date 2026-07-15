package de.fatox.meta.api.model

import com.badlogic.gdx.utils.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetaWindowDataTest {
	@Test
	fun `anchored window position follows resized viewport edges`() {
		val data = MetaWindowData(
			width = 160f,
			height = 120f,
			horizontalAnchor = "right",
			verticalAnchor = "top",
			horizontalDistance = 24f,
			verticalDistance = 32f,
		)

		assertEquals(1016f, data.resolveHorizontal(viewportWidth = 1200f, windowWidth = 160f))
		assertEquals(648f, data.resolveVertical(viewportHeight = 800f, windowHeight = 120f))
	}

	@Test
	fun `legacy absolute window position is still used when no anchors are saved`() {
		val data = MetaWindowData(x = 420f, y = 180f, width = 160f, height = 120f)

		assertEquals(420f, data.resolveHorizontal(viewportWidth = 1200f, windowWidth = 160f))
		assertEquals(180f, data.resolveVertical(viewportHeight = 800f, windowHeight = 120f))
	}

	@Test
	fun `window position is clamped into smaller viewport`() {
		val data = MetaWindowData(
			width = 160f,
			height = 120f,
			horizontalAnchor = "right",
			verticalAnchor = "top",
			horizontalDistance = 24f,
			verticalDistance = 32f,
		)

		assertEquals(16f, data.resolveHorizontal(viewportWidth = 200f, windowWidth = 160f))
		assertEquals(0f, data.resolveVertical(viewportHeight = 150f, windowHeight = 120f))
	}

	@Test
	fun `restored resizable window dimensions respect min and max size`() {
		assertEquals(180f, clampWindowDimension(value = 80f, min = 180f, max = 360f))
		assertEquals(280f, clampWindowDimension(value = 500f, min = 120f, max = 280f))
		assertEquals(500f, clampWindowDimension(value = 500f, min = 120f, max = 0f))
	}

	@Test
	fun `legacy persisted window data receives safe undocked defaults`() {
		val data = Json().fromJson(
			MetaWindowData::class.java,
			"""{"x":42,"y":24,"width":300,"height":200,"displayed":true}""",
		)

		assertEquals("", data.dockSide)
		assertEquals(0, data.dockOrder)
		assertEquals(0f, data.dockHeight)
		assertEquals(false, data.dockFill)
	}

	@Test
	fun `docking fields survive persistence round trip`() {
		val original = MetaWindowData(
			x = 42f,
			y = 24f,
			width = 300f,
			height = 200f,
			dockSide = "right",
			dockOrder = 200,
			dockHeight = 180f,
			dockFill = true,
		)
		val json = Json()
		val restored = json.fromJson(MetaWindowData::class.java, json.toJson(original))

		assertEquals("right", restored.dockSide)
		assertEquals(200, restored.dockOrder)
		assertEquals(180f, restored.dockHeight)
		assertEquals(true, restored.dockFill)
	}
}
