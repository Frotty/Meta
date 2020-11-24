package de.fatox.meta

import com.badlogic.gdx.Gdx
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.error
import javax.swing.*

private val log = MetaLoggerFactory.logger {}

private const val ERROR_HEADER = "Please report this crash with the following info:\n"

object ExceptionHandler : Thread.UncaughtExceptionHandler {
	private val icon: ImageIcon? by lazy {
		try {
			ImageIcon(Gdx.files.internal("meta-icon-error.png").readBytes(), "Meta Error")
		} catch (t: Throwable) {
			null
		}
	}
	private val textArea: JTextArea by lazy {
		JTextArea(30, ERROR_HEADER.length + 50)
			.apply { isEditable = false }
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

		textArea.text = "Please report this crash with the following info:\n${e.stackTraceToString()}"

		JOptionPane.showMessageDialog(
			null,
			scrollPane,
			"Uncaught Exception",
			JOptionPane.ERROR_MESSAGE,
			icon
		)
	}
}