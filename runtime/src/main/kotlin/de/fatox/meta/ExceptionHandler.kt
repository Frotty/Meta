package de.fatox.meta

import com.badlogic.gdx.Gdx
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.error
import de.fatox.meta.injection.MetaInject
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.prefs.Preferences
import javax.swing.*
import kotlin.system.exitProcess

private val log = MetaLoggerFactory.logger {}

private const val ERROR_HEADER = "Please report this crash with the following info:\n"
private const val AUTO_SEND_PREF = "autoSendCrashReports"

object ExceptionHandler : Thread.UncaughtExceptionHandler {
	private val icon: ImageIcon? by lazy {
		try {
			ImageIcon(Gdx.files.internal("meta-icon-error.png").readBytes(), "Meta Error")
		} catch (t: Throwable) {
			null
		}
	}
	private val textArea: JTextArea by lazy {
		JTextArea(30, ERROR_HEADER.length + 50).apply { isEditable = false }
	}
	private val scrollPane: JScrollPane by lazy { JScrollPane(textArea) }

	init {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
		} catch (e: ClassNotFoundException) {
			e.printStackTrace()
		} catch (e: InstantiationException) {
			e.printStackTrace()
		} catch (e: IllegalAccessException) {
			e.printStackTrace()
		} catch (e: UnsupportedLookAndFeelException) {
			e.printStackTrace()
		}
	}

	override fun uncaughtException(t: Thread, e: Throwable) {
		log.error(e) { e.localizedMessage }

		val stackTrace = e.stackTraceToString()
		val crashFile = crashLogFile()
		val report = crashReport(t, e, stackTrace)
		val crashText = buildString {
			append(ERROR_HEADER)
			if (crashFile != null) {
				append("Crash log: ")
				append(crashFile.absolutePath)
				append("\n\n")
			}
			append(stackTrace)
		}
		writeCrashLog(crashFile, crashText)
		textArea.text = crashText
		textArea.caretPosition = 0
		if (autoSendEnabled() && crashReportUrl().isNotBlank()) {
			sendCrashReport(report, null)
		}

		if (GraphicsEnvironment.isHeadless()) return

		SwingUtilities.invokeLater {
			lateinit var frame: JFrame
			val statusLabel = JLabel(if (crashReportUrl().isBlank()) "Crash upload is not configured." else "Diagnostic report not sent.")
			val sendButton = JButton("Send Report").apply {
				isEnabled = crashReportUrl().isNotBlank()
				addActionListener {
					isEnabled = false
					statusLabel.text = "Sending diagnostic report..."
					sendCrashReport(report) { ok, message ->
						SwingUtilities.invokeLater {
							statusLabel.text = message
							isEnabled = !ok
							if (ok) Timer(500) { frame.dispose() }.apply {
								isRepeats = false
								start()
							}
						}
					}
				}
			}
			val copyButton = JButton("Copy Report").apply {
				addActionListener {
					Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(crashText), null)
					statusLabel.text = "Crash report copied to clipboard."
				}
			}
			val openFolderButton = JButton("Open Log Folder").apply {
				isEnabled = crashFile?.parentFile?.exists() == true && Desktop.isDesktopSupported()
				addActionListener {
					runCatching { Desktop.getDesktop().open(crashFile?.parentFile) }
						.onFailure { statusLabel.text = "Could not open log folder: ${it.message}" }
				}
			}
			val autoSendCheckbox = JCheckBox("Always send crash diagnostics automatically").apply {
				isSelected = autoSendEnabled()
				isEnabled = crashReportUrl().isNotBlank()
				addActionListener {
					setAutoSendEnabled(isSelected)
				}
			}
			val okButton = JButton("OK").apply {
				addActionListener { frame.dispose() }
			}
			val actions = JPanel().apply {
				add(sendButton)
				add(copyButton)
				add(openFolderButton)
				add(autoSendCheckbox)
				add(okButton)
			}
			val content = JPanel(BorderLayout(8, 8)).apply {
				border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
				icon?.let { add(JLabel(it), BorderLayout.WEST) }
				scrollPane.preferredSize = Dimension(820, 560)
				add(scrollPane, BorderLayout.CENTER)
				add(JPanel(BorderLayout()).apply {
					add(statusLabel, BorderLayout.NORTH)
					add(actions, BorderLayout.SOUTH)
				}, BorderLayout.SOUTH)
			}
			frame = JFrame("Uncaught Exception").apply {
				defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
				addWindowListener(object : WindowAdapter() {
					override fun windowClosed(event: WindowEvent) {
						// The application has already terminated with an uncaught exception. Returning through the
						// jpackage launcher makes it display a second, misleading "Failed to launch JVM" dialog.
						// Exit the crashed process directly after the report window is dismissed instead.
						exitProcess(1)
					}
				})
				icon?.image?.let { iconImage = it }
				contentPane = content
				pack()
				setLocationRelativeTo(null)
				extendedState = extendedState and Frame.ICONIFIED.inv()
				state = Frame.NORMAL
				isAlwaysOnTop = true
				isVisible = true
				toFront()
				requestFocus()
				okButton.requestFocusInWindow()
				SwingUtilities.invokeLater {
					toFront()
					requestFocus()
				}
			}
		}
	}

	private fun crashReport(thread: Thread, throwable: Throwable, stackTrace: String): Map<String, String> =
		mapOf(
			"appName" to appName(),
			"version" to System.getProperty("meta.crashReportVersion", ""),
			"revision" to System.getProperty("meta.crashReportRevision", ""),
			"timestamp" to Instant.now().toString(),
			"threadName" to thread.name,
			"throwableClass" to throwable.javaClass.name,
			"message" to (throwable.message ?: ""),
			"stackTrace" to stackTrace,
			"osName" to System.getProperty("os.name", ""),
			"osVersion" to System.getProperty("os.version", ""),
			"osArch" to System.getProperty("os.arch", ""),
			"javaVersion" to System.getProperty("java.version", ""),
			"javaVendor" to System.getProperty("java.vendor", ""),
			"serviceHost" to System.getProperty("meta.crashReportServiceHost", ""),
		)

	private fun sendCrashReport(report: Map<String, String>, onDone: ((Boolean, String) -> Unit)?) {
		val url = crashReportUrl()
		if (url.isBlank()) {
			onDone?.invoke(false, "Crash upload is not configured.")
			return
		}
		Thread({
			try {
				val connection = URL(url).openConnection() as HttpURLConnection
				connection.requestMethod = "POST"
				connection.connectTimeout = 8_000
				connection.readTimeout = 8_000
				connection.doOutput = true
				connection.setRequestProperty("Content-Type", "application/json")
				connection.outputStream.use { it.write(toJson(report).toByteArray(StandardCharsets.UTF_8)) }
				val code = connection.responseCode
				if (code in 200..299) {
					onDone?.invoke(true, "Diagnostic report sent. Thank you.")
				} else {
					onDone?.invoke(false, "Diagnostic upload failed: HTTP $code")
				}
				connection.disconnect()
			} catch (t: Throwable) {
				onDone?.invoke(false, "Diagnostic upload failed: ${t.message}")
			}
		}, "crash-report-uploader").apply {
			start()
		}
	}

	private fun toJson(values: Map<String, String>): String =
		values.entries.joinToString(prefix = "{", postfix = "}") {
			"\"${jsonEscape(it.key)}\":\"${jsonEscape(it.value)}\""
		}

	private fun jsonEscape(value: String): String =
		buildString(value.length + 16) {
			for (char in value) {
				when (char) {
					'\\' -> append("\\\\")
					'"' -> append("\\\"")
					'\n' -> append("\\n")
					'\r' -> append("\\r")
					'\t' -> append("\\t")
					else -> if (char.code < 32) append("\\u%04x".format(char.code)) else append(char)
				}
			}
		}

	private fun writeCrashLog(crashFile: File?, crashText: String) {
		runCatching {
			if (crashFile == null) return@runCatching
			crashFile.parentFile?.mkdirs()
			crashFile.writeText(crashText, StandardCharsets.UTF_8)
		}
	}

	private fun crashLogFile(): File? {
		val appName = appName()
		val safeAppName = appName.replace(Regex("[^A-Za-z0-9._-]"), "_")
		val localAppData = System.getenv("LOCALAPPDATA")
		if (!localAppData.isNullOrBlank()) return File(localAppData, "$safeAppName/logs/crash.log")
		val userHome = System.getProperty("user.home")
		if (!userHome.isNullOrBlank()) return File(userHome, ".$safeAppName/logs/crash.log")
		val tempDir = System.getProperty("java.io.tmpdir") ?: return null
		return File(tempDir, "$safeAppName/logs/crash.log")
	}

	private fun appName(): String =
		System.getProperty("meta.crashReportAppName")
			?.takeIf { it.isNotBlank() }
			?: runCatching { MetaInject.inject<String>("gameName") }
			.getOrNull()
			?.takeIf { it.isNotBlank() }
			?: "Meta"

	private fun crashReportUrl(): String = System.getProperty("meta.crashReportUrl", "")

	private fun preferences(): Preferences =
		Preferences.userRoot().node("de/fatox/meta/ExceptionHandler/${appName()}")

	private fun autoSendEnabled(): Boolean =
		preferences().getBoolean(AUTO_SEND_PREF, false)

	private fun setAutoSendEnabled(enabled: Boolean) {
		preferences().putBoolean(AUTO_SEND_PREF, enabled)
	}
}
