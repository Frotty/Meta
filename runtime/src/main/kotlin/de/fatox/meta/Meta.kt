package de.fatox.meta

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.DummyPosModifier
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.injection.Metastasis
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.*
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
import javax.swing.UIManager as JavaUIManager


open class Meta : Game {
	private var metastasis: Metastasis? = null
	private var lastChange: Long = 0
	private var lastScreen: Screen? = null

	protected val firstScreen: Screen by lazyInject()
	protected val uiManager: UIManager by lazyInject()

	protected var modifier: PosModifier

	@JvmOverloads
	constructor(modifier: PosModifier = DummyPosModifier()) {
		this.modifier = modifier
		setUncaughtHandler()
		metaInstance = this
		setupMetastasis()
		addModule(MetaModule())
	}

	constructor(modifier: PosModifier, vararg modules: Any?) {
		this.modifier = modifier
		setUncaughtHandler()
		metaInstance = this
		setupMetastasis()
		for (module in modules) {
			addModule(module)
		}
		addModule(MetaModule())
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
			jTextField.text = "Please report this crash with the following info:\n$sw"
			jTextField.isEditable = false
			val scroll = JScrollPane(jTextField, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)
			JOptionPane.showMessageDialog(JFrame(), scroll, "Uncaught Exception", JOptionPane.ERROR_MESSAGE)
		}
	}

	private fun setupMetastasis() {
		metastasis = Metastasis()
	}

	override fun create() {
		//inject(this)
		uiManager.posModifier = modifier
		changeScreen(firstScreen)
	}

	val lastScreenType: Class<*>
		get() = lastScreen!!.javaClass

	companion object {
		private val log = LoggerFactory.getLogger(Meta::class.java)
		private var metaInstance: Meta? = null
		val instance: Meta?
			get() = if (metaInstance != null) metaInstance else Meta()

		fun addModule(module: Any?) {
			instance!!.metastasis!!.loadModule(module)
		}

		fun registerMetaAnnotation(annotationClass: Class<*>?) {}
		fun canChangeScreen(): Boolean {
			return TimeUtils.millis() > metaInstance!!.lastChange + 150
		}

		fun newLastScreen() {
			if (instance!!.lastScreen != null) {
				try {
					changeScreen(instance!!.lastScreen!!.javaClass.newInstance())
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}

		fun changeScreen(newScreen: Screen?) {
			if (canChangeScreen()) {
				metaInstance!!.lastChange = TimeUtils.millis()
				val oldScreen = instance!!.getScreen()
				if (oldScreen != null && !oldScreen.javaClass.isInstance(newScreen)) {
					instance!!.lastScreen = oldScreen
				}
				Gdx.app.postRunnable { instance!!.setScreen(newScreen) }
			}
		}

		@JvmStatic
		fun inject(`object`: Any?) {
			instance!!.metastasis!!.injectFields(`object`)
		}
	}
}