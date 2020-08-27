package de.fatox.meta

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.UnsupportedLookAndFeelException
import kotlin.reflect.KClass
import javax.swing.UIManager as JavaUIManager

class ScreenConfig {
	internal val nameToClass: MutableMap<String, KClass<out Screen>> = mutableMapOf()
	internal val classToName: MutableMap<KClass<out Screen>, String> = mutableMapOf()
	internal val screenCreators: MutableMap<String, () -> Screen> = mutableMapOf()

	@PublishedApi
	internal fun <T : Screen> register(screenClass: KClass<T>, name: String, creator: () -> T) {
		require(nameToClass[name] == null) { "Name already registered: $name" }

		nameToClass[name] = screenClass
		classToName[screenClass] = name
		screenCreators[name] = creator
	}
}

inline fun <reified T : Screen> ScreenConfig.register(
	name: String = T::class.qualifiedName
		?: "",
	noinline creator: () -> T,
) {
	register(T::class, name, creator)
}

abstract class Meta(protected var modifier: PosModifier) : Game() {
	protected val firstScreen: Screen by lazyInject()
	protected val uiManager: UIManager by lazyInject()

	private var lastChange: Long = 0
	private lateinit var lastScreen: Screen

	private val screenConfig: ScreenConfig by lazyInject()

	init {
		setUncaughtHandler()
		metaInstance = this
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
	abstract fun MetaInject.injection()
	abstract fun ScreenConfig.screens()
	abstract fun WindowConfig.windows()

	override fun create() {
		MetaInject.injection()
		MetaModule
		uiManager.posModifier = modifier
		MetaInject.global { singleton("default") { ScreenConfig().apply { screens() } } }
		MetaInject.global { singleton("default") { WindowConfig().apply { windows() } } }
		config()
		changeScreen(firstScreen)
	}

	val lastScreenType: KClass<*> get() = lastScreen::class
	val lastScreenName: String get() = screenConfig.classToName[lastScreenType]!!

	companion object {
		private val log: Logger = LoggerFactory.getLogger(Meta::class.java)
		private lateinit var metaInstance: Meta
		val instance: Meta by lazy { metaInstance }

		fun addModule(module: Any) {
			module
		}

		fun registerMetaAnnotation(annotationClass: Class<*>?) {}
		fun canChangeScreen(): Boolean {
			return TimeUtils.millis() > instance.lastChange + 150
		}

		fun newLastScreen() {
			changeScreen(
				instance.screenConfig.screenCreators[instance.lastScreenName]!!()
			)
		}

		fun changeScreen(newScreen: Screen) {
			if (canChangeScreen()) {
				instance.lastChange = TimeUtils.millis()
				val oldScreen = instance.getScreen()
				if (oldScreen != null && !oldScreen.javaClass.isInstance(newScreen)) {
					instance.lastScreen = oldScreen
				}
				Gdx.app.postRunnable { instance.setScreen(newScreen) }
			}
		}
	}
}