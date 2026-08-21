package de.fatox.meta

import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertTrue

internal class Java25RuntimeTest {
	@Test
	fun `runtime tests exercise the required Java 25 profile`() {
		assertTrue(Runtime.version().feature() >= 25, "Meta tests must execute on Java 25 or newer")
		val arguments = ManagementFactory.getRuntimeMXBean().inputArguments
		assertTrue("-XX:+UseCompactObjectHeaders" in arguments, "Compact object headers must remain enabled")
		assertTrue("--enable-native-access=ALL-UNNAMED" in arguments, "libGDX native access must remain enabled")
	}
}
