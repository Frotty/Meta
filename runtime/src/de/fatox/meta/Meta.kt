package de.fatox.meta

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.DummyPosModifier
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.UnsupportedLookAndFeelException
import javax.swing.UIManager as JavaUIManager


abstract class Meta(protected var modifier: PosModifier, vararg modules: Any) : Game() {
	protected val firstScreen: Screen by lazyInject()
	protected val uiManager: UIManager by lazyInject()

	private var lastChange: Long = 0
	private lateinit var lastScreen: Screen

	init {
		setUncaughtHandler()
		metaInstance = this
		for (module in modules) {
			addModule(module)
		}
		addModule(MetaModule)
	}

	private fun setUncaughtHandler() {
		Thread.setDefaultUncaughtExceptionHandler { _, exception ->
			log.error(exception.message, exception)
			try {
				JavaUIManager.setLookAndFeel(JavaUIManager.getSystemLookAndFeelClassName())
			} catch (e: ClassNotFoundException) {
				e.printStackTrace()
			} catch (e: InstantiationException) {
				e.printStackTrace()
			} catch (e: IllegalAccessException) {
				e.printStackTrace()
			} catch (e: UnsupportedLookAndFeelException) {
				e.printStackTrace()
			}
			val sw = StringWriter()
			exception.printStackTrace(PrintWriter(sw))
			val jTextField = JTextArea()
			jTextField.lineWrap = true
			jTextField.columns = "Please report this crash with the following info:\n".length + 50
			jTextField.rows = 30
			jTextField.text = "Please report this crash with the following info:\n$sw"
			jTextField.isEditable = false
			val scroll = JScrollPane(jTextField)
			JOptionPane.showMessageDialog(null, scroll, "Uncaught Exception", JOptionPane.ERROR_MESSAGE)
		}
	}

	abstract fun config()

	override fun create() {
		uiManager.posModifier = modifier
		config()
		changeScreen(firstScreen)
	}

	val lastScreenType: Class<*>
		get() = lastScreen.javaClass

	companion object {
		private val log: Logger = LoggerFactory.getLogger(Meta::class.java)
		private lateinit var metaInstance: Meta
		val instance: Meta by lazy { metaInstance }

		fun addModule(module: Any) {
			module
		}

		fun registerMetaAnnotation(annotationClass: Class<*>?) {}
		fun canChangeScreen(): Boolean {
			return TimeUtils.millis() > metaInstance.lastChange + 150
		}

		fun newLastScreen() {
			try {
				changeScreen(instance.lastScreen.javaClass.newInstance())
			} catch (e: Exception) {
				log.error("Failed to create last screen!", e)
			}
		}

		fun changeScreen(newScreen: Screen) {
			if (canChangeScreen()) {
				metaInstance.lastChange = TimeUtils.millis()
				val oldScreen = instance.getScreen()
				if (oldScreen != null && !oldScreen.javaClass.isInstance(newScreen)) {
					instance.lastScreen = oldScreen
				}
				Gdx.app.postRunnable { instance.setScreen(newScreen) }
			}
		}
	}
}