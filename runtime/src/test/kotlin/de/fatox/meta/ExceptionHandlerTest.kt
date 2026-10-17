package de.fatox.meta

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.prefs.AbstractPreferences
import java.util.prefs.Preferences
import java.util.prefs.PreferencesFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

private const val INSTALLED_MARKER = "meta-exception-handler-installed"
private const val HANDLED_MARKER = "meta-exception-handler-returned"
private val CLASS_LOAD = Regex("""\[class,load] (\S+)""")
private val DESKTOP_PACKAGES = listOf("java.awt.", "javax.swing.", "sun.awt.", "sun.java2d.")

internal class ExceptionHandlerTest {
	@TempDir
	lateinit var temporaryDirectory: Path

	@Test
	fun `application storage names use one case independent directory name`() {
		assertEquals("oxrox", canonicalAppStorageName("OxRox"))
		assertEquals("oxrox", canonicalAppStorageName("oxrox"))
		assertEquals("my_game-1.2", canonicalAppStorageName("My Game-1.2"))
	}

	/**
	 * Meta's constructor installs the handler, so whatever initialising it loads, every game pays for at launch. Swing
	 * is only needed once a crash dialog is shown. This runs in a fresh JVM because this one is no clean slate: other
	 * tests may already have initialised the handler or loaded AWT.
	 */
	@Test
	fun `installing the handler loads no AWT or Swing class`() {
		val output = runFreshJvm(
			InstallExceptionHandler::class.java,
			"-Djava.awt.headless=true",
			"-Xlog:class+load=info:stdout",
		)
		val installedAt = output.indexOf(INSTALLED_MARKER)
		assertTrue(installedAt >= 0, "The handler was not installed:\n" + output.takeLast(40).joinToString("\n"))
		val loaded = output.subList(0, installedAt).mapNotNull { CLASS_LOAD.find(it)?.groupValues?.get(1) }
		// Proves the class-load log was read at all; without it an unparsed log would pass.
		assertTrue(ExceptionHandler::class.java.name in loaded, "The handler's own class load was not logged")
		val desktopClasses = loaded.filter { name -> DESKTOP_PACKAGES.any { name.startsWith(it) } }
		assertEquals(emptyList(), desktopClasses, "Installing the handler loaded AWT/Swing")
	}

	/**
	 * A handler that throws loses the crash it was handed. Without java.desktop the first AWT call fails, standing in
	 * for a missing native library or a native image built without AWT. Preferences and the crash log are redirected
	 * so nothing real is touched.
	 */
	@Test
	fun `a crash the dialog cannot show is still logged and does not escape the handler`() {
		val appData = temporaryDirectory.toFile()
		val output = runFreshJvm(
			CrashWithoutAwt::class.java,
			"--limit-modules", "java.base,java.prefs",
			"-Djava.util.prefs.PreferencesFactory=${InMemoryPreferencesFactory::class.java.name}",
			"-Dmeta.crashReportAppName=crash-test",
			environment = mapOf("LOCALAPPDATA" to appData.path),
		)
		assertTrue(HANDLED_MARKER in output, "The handler threw:\n" + output.takeLast(40).joinToString("\n"))
		val crashLog = File(appData, "crash-test/logs/crash.log")
		assertTrue("IllegalStateException: boom" in crashLog.readText(), "The crash log lost the original exception")
	}

	private fun runFreshJvm(
		main: Class<*>,
		vararg jvmArgs: String,
		environment: Map<String, String> = emptyMap(),
	): List<String> {
		val log = Files.createTempFile(temporaryDirectory, "fresh-jvm", ".log").toFile()
		val command = listOf(File(System.getProperty("java.home"), "bin/java").path) + jvmArgs +
			listOf("-cp", System.getProperty("java.class.path"), main.name)
		val builder = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log)
		builder.environment().putAll(environment)
		val process = builder.start()
		if (!process.waitFor(60, TimeUnit.SECONDS)) {
			process.destroyForcibly()
			fail("The fresh JVM did not exit within 60 seconds")
		}
		val output = log.readLines()
		assertEquals(0, process.exitValue(), "The fresh JVM failed:\n" + output.takeLast(40).joinToString("\n"))
		return output
	}
}

/** Entry point for the fresh JVM: installs the handler the way Meta's constructor does, then marks the point. */
internal object InstallExceptionHandler {
	@JvmStatic
	fun main(args: Array<String>) {
		Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler)
		println(INSTALLED_MARKER)
	}
}

/** Entry point for the fresh JVM: hands the handler a crash and marks that it returned. */
internal object CrashWithoutAwt {
	@JvmStatic
	fun main(args: Array<String>) {
		ExceptionHandler.uncaughtException(Thread.currentThread(), IllegalStateException("boom"))
		println(HANDLED_MARKER)
	}
}

/** Keeps the fresh JVM's preference reads out of the registry and the home directory. */
internal class InMemoryPreferencesFactory : PreferencesFactory {
	private val root = InMemoryPreferences(null, "")

	override fun userRoot(): Preferences = root
	override fun systemRoot(): Preferences = root
}

private class InMemoryPreferences(parent: InMemoryPreferences?, name: String) : AbstractPreferences(parent, name) {
	private val values = HashMap<String, String>()

	override fun putSpi(key: String, value: String) {
		values[key] = value
	}

	override fun getSpi(key: String): String? = values[key]
	override fun removeSpi(key: String) {
		values.remove(key)
	}

	override fun removeNodeSpi() = Unit
	override fun keysSpi(): Array<String> = values.keys.toTypedArray()
	override fun childrenNamesSpi(): Array<String> = emptyArray()
	override fun childSpi(name: String): AbstractPreferences = InMemoryPreferences(this, name)
	override fun syncSpi() = Unit
	override fun flushSpi() = Unit
}
