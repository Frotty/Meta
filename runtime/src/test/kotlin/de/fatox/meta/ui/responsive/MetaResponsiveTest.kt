package de.fatox.meta.ui.responsive

import de.fatox.meta.reactive.subscribe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MetaResponsiveTest {
	@Test
	fun `desktop defaults change at exact resolution boundaries`() {
		val state = MetaResponsiveState()
		val transitions = ArrayList<MetaBreakpoint>()
		val subscription = state.breakpoint.subscribe { transitions.add(state.breakpoint.value) }

		state.resize(1279f, 720f)
		assertEquals(MetaBreakpoints.NARROW, state.breakpoint.value)
		assertTrue(transitions.isEmpty(), "Changing size inside a breakpoint must not emit a breakpoint change")

		state.resize(1280f, 720f)
		assertEquals(MetaBreakpoints.HD, state.breakpoint.value)
		state.resize(1920f, 1080f)
		assertEquals(MetaBreakpoints.FULL_HD, state.breakpoint.value)
		state.resize(2560f, 1440f)
		assertEquals(MetaBreakpoints.QHD, state.breakpoint.value)
		state.resize(3840f, 2160f)
		assertEquals(MetaBreakpoints.UHD, state.breakpoint.value)
		assertEquals(listOf(MetaBreakpoints.HD, MetaBreakpoints.FULL_HD, MetaBreakpoints.QHD, MetaBreakpoints.UHD), transitions)

		subscription.dispose()
	}

	@Test
	fun `responsive values cascade and remain reactive when extended`() {
		val state = MetaResponsiveState()
		val density = responsive("compact")
			.from(MetaBreakpoints.HD, "standard")
			.from(MetaBreakpoints.QHD, "spacious")
		val resolved = state.resolve(density)

		state.resize(1920f, 1080f)
		assertEquals("standard", resolved.value)
		state.resize(2560f, 1440f)
		assertEquals("spacious", resolved.value)

		state.resize(3840f, 2160f)
		assertEquals("spacious", resolved.value)
		density.from(MetaBreakpoints.UHD, "cinema")
		assertEquals("cinema", resolved.value)
	}

	@Test
	fun `width ranges and height constraints have non-overlapping boundaries`() {
		val hdOnly = MetaResponsiveQuery.between(MetaBreakpoints.HD, MetaBreakpoints.FULL_HD)
		assertFalse(hdOnly.matches(MetaResponsiveSize(1279f, 720f)))
		assertTrue(hdOnly.matches(MetaResponsiveSize(1280f, 720f)))
		assertTrue(hdOnly.matches(MetaResponsiveSize(1919f, 1080f)))
		assertFalse(hdOnly.matches(MetaResponsiveSize(1920f, 1080f)))

		val shortWindow = MetaResponsiveQuery.heightBelow(800f)
		assertTrue(shortWindow.matches(MetaResponsiveSize(1920f, 799f)))
		assertFalse(shortWindow.matches(MetaResponsiveSize(1920f, 800f)))
	}

	@Test
	fun `custom breakpoint sets sort once and reject ambiguous scales`() {
		val desktop = MetaBreakpoint("desktop", 1400f)
		val narrow = MetaBreakpoint("narrow", 0f)
		val set = MetaBreakpointSet(desktop, narrow)
		assertEquals(narrow, set.active(1399f))
		assertEquals(desktop, set.active(1400f))

		assertFailsWith<IllegalArgumentException> { MetaBreakpointSet(desktop) }
		assertFailsWith<IllegalArgumentException> {
			MetaBreakpointSet(narrow, MetaBreakpoint("duplicate", 0f))
		}
		assertFailsWith<IllegalArgumentException> { MetaResponsiveState().resize(Float.NaN, 10f) }
	}
}
