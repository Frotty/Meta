package de.fatox.meta.api.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import de.fatox.meta.ScreenConfig
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.ui.bindDisabled
import de.fatox.meta.ui.components.MetaMenuBar
import de.fatox.meta.ui.tabs.MetaTab
import de.fatox.meta.ui.windows.MetaDialog
import de.fatox.meta.ui.windows.MetaWindow
import kotlin.reflect.KClass

class WindowConfig {
	internal val nameToClass: MutableMap<String, KClass<out Window>> = mutableMapOf()
	internal val classToName: MutableMap<KClass<out Window>, String> = mutableMapOf()
	internal val creators: MutableMap<String, () -> Window> = mutableMapOf()
	internal val singletons: MutableMap<String, () -> Window> = mutableMapOf()
	internal val singletonCache: MutableMap<String, Window> = mutableMapOf()

	internal inline fun nameOf(windowClass: KClass<out Window>): String = classToName.getValue(windowClass)

	internal fun <T : Window> create(name: String): T =
		singletonCache[name] as T?
			?: singletons[name]?.invoke()?.also { singletonCache[name] = it } as T?
			?: creators.getValue(name)() as T

	internal fun <T : Window> create(windowClass: KClass<out T>): T = create(nameOf(windowClass))

	@PublishedApi
	internal fun <T : MetaWindow> register(windowClass: KClass<T>, name: String, creator: () -> T) =
		registerInternal(windowClass, name, creators, creator)

	@PublishedApi
	internal fun <T : MetaWindow> registerSingleton(windowClass: KClass<T>, name: String, creator: () -> T) =
		registerInternal(windowClass, name, singletons, creator)

	private inline fun <T : MetaWindow> registerInternal(
		windowClass: KClass<T>,
		name: String,
		map: MutableMap<String, () -> Window>,
		noinline creator: () -> T
	) {
		require(nameToClass[name] == null) { "Name already registered: $name" }

		nameToClass[name] = windowClass
		classToName[windowClass] = name
		map[name] = creator
	}
}

inline fun <reified T : MetaWindow> WindowConfig.register(
	name: String = T::class.qualifiedName ?: "",
	noinline creator: () -> T,
) {
	handleLegacyName<T>(name)
	register(T::class, name, creator)
}

inline fun <reified T : MetaWindow> WindowConfig.registerSingleton(
	name: String = T::class.qualifiedName ?: "",
	noinline creator: () -> T,
) {
	handleLegacyName<T>(name)
	registerSingleton(T::class, name, creator)
}

val logger = MetaLoggerFactory.logger {}

inline fun <reified T : MetaWindow> handleLegacyName(name: String) {
	val gameName: String = MetaInject.inject("gameName")
	Gdx.files.external(".$gameName").child(MetaData.GLOBAL_DATA_FOLDER_NAME).list().forEach { screenId ->
		if (screenId.isDirectory)
			screenId.list().firstOrNull { it.name() == T::class.qualifiedName }?.let { windowId ->
				logger.debug("Found legacy window name: ${windowId.name()}, replacing with $name")
				windowId.moveTo(windowId.sibling(name))
			}
	}
}

/**
 * Created by Frotty on 20.05.2016.
 */
interface UIManager : Disposable {
	companion object {
		const val DEFAULT_TOAST_SECONDS = 3.5f
		const val IMPORTANT_TOAST_SECONDS = 5f
		const val ACTION_TOAST_SECONDS = 90f
	}

	var windowHandler: WindowHandler
	fun moveWindow(x: Int, y: Int)
	fun resize(width: Int, height: Int)

	fun <T : Screen> changeScreen(screenClass: KClass<T>)

	fun <T : MetaTab> changeTab(tabClass: KClass<T>)

	fun addTable(table: Table?, growX: Boolean, growY: Boolean)

	fun <T : Window> showWindow(windowClass: KClass<out T>): T

	fun hideOtherWindowsAndPreventNew(window: Window)
	fun restoreOtherWindowsAndAllowNew()
	fun <T : MetaDialog> showDialog(dialogClass: KClass<out T>, showBackdrop: Boolean): T
	fun setMainMenuBar(menuBar: MetaMenuBar?)
	fun <T : Window> getWindow(windowClass: KClass<out T>): T
	fun closeWindow(window: Window)
	fun updateWindow(window: Window)
	fun bringWindowsToFront()
	fun metaHas(name: String): Boolean
	fun <T : Any> metaGet(name: String, c: KClass<out T>): T?
	fun metaSave(name: String, windowData: Any)
	/** Notifies the manager that a dialog has been detached from the stage, so it can release the shared backdrop. */
	fun onDialogRemoved(dialog: MetaDialog)

	/** Shows a transient toast notification that always renders above windows, dialogs and the modal backdrop. */
	fun showToast(message: String, duration: Float = DEFAULT_TOAST_SECONDS)

	/** Shows a custom [Table] as a toast that always renders above windows, dialogs and the modal backdrop. */
	fun showToast(table: Table, duration: Float = DEFAULT_TOAST_SECONDS)

	/** Clears all visible toast notifications. Use sparingly, mainly when an actionable toast was accepted. */
	fun clearToasts()

	/**
	 * Sets the current screen's contextual bottom overlay. A modal dialog temporarily replaces this with its own
	 * [MetaDialog.bottomOverlay], or suppresses it when the dialog has no overlay. The manager owns presentation and
	 * z-order only; the consumer owns the actor and its content.
	 */
	fun setBottomOverlay(overlay: Actor?) = Unit

	/** Re-derives the contextual overlay after a shown dialog changes [MetaDialog.bottomOverlay]. */
	fun onDialogBottomOverlayChanged(dialog: MetaDialog) = Unit

	val currentlyActiveWindows: Array<Window>
	val windowConfig: WindowConfig

	/** UI surface size in UI units (DPI-scaled). Use these to place/size windows - not `Gdx.graphics.width/height`,
	 *  which are physical pixels and will be wrong on HiDPI (off by the active `uiScale`). */
	val uiWidth: Float
	val uiHeight: Float

	/**
	 * Reactive flag that is true while showing new windows is suppressed (a modal flow is active). Bind UI to it,
	 * e.g. `actor.bindDisabled { uiManager.preventShowWindowState() }`, instead of registering an observer.
	 */
	val preventShowWindowState: ReactiveValue<Boolean>
	val preventShowWindow: Boolean

	@Deprecated("Bind to preventShowWindowState instead, e.g. actor.bindDisabled { uiManager.preventShowWindowState() }.")
	val preventShowWindowObservers: Array<(Boolean) -> Unit>

	val screenConfig: ScreenConfig

	override fun dispose() = Unit
}

inline fun <reified T : Any> UIManager.metaGet(name: String): T? = metaGet(name, T::class)

inline fun <reified T : MetaWindow> UIManager.getWindow(config: T.() -> Unit = {}): T =
	getWindow(T::class).apply(config)

inline fun <reified T : MetaWindow> UIManager.showWindow(config: T.() -> Unit = {}): T =
	showWindow(T::class).apply(config)

inline fun <reified T : MetaDialog> UIManager.showDialog(config: T.() -> Unit = {}): T =
	showDialog(T::class, true).apply(config)

inline fun <reified T : MetaDialog> UIManager.showDialog(config: T.() -> Unit = {}, showBackdrop: Boolean): T =
	showDialog(T::class, showBackdrop).apply(config)

inline fun <reified T : Screen> UIManager.changeScreen(): Unit = changeScreen(T::class)

/**
 * Wires [actor] to open window [W] on click. Pass the owning presentation's [scope] (e.g. a `MetaWindow.reactiveScope`
 * or a screen-owned [ReactiveScope]) to also disable the actor while a modal flow suppresses new windows: the binding
 * observes the app-lifetime [UIManager.preventShowWindowState] signal, so it MUST be owned by a disposable scope or it
 * would retain the actor forever. Without a scope no binding is created (showing is still blocked by the manager).
 */
inline fun <reified W : MetaWindow> UIManager.showWindowOnClick(
	actor: Actor,
	button: Int = Input.Buttons.LEFT,
	scope: ReactiveScope? = null,
	crossinline config: W.() -> Unit = {},
): Actor {
	if (scope != null && actor is Disableable) scope.register(actor.bindDisabled { preventShowWindowState.value })
	actor.addListener(object : ClickListener(button) {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			showWindow(config)
		}
	})
	return actor
}

/**
 * Wires [actor] to open dialog [D] on click. See [showWindowOnClick] for the [scope] parameter: it owns the
 * disabled-while-modal binding; without a scope no binding is created (showing is still blocked by the manager).
 */
inline fun <reified D : MetaDialog> UIManager.showDialogOnClick(
	actor: Actor,
	button: Int = Input.Buttons.LEFT,
	scope: ReactiveScope? = null,
	crossinline config: D.() -> Unit = {},
): Actor {
	if (scope != null && actor is Disableable) scope.register(actor.bindDisabled { preventShowWindowState.value })
	actor.addListener(object : ClickListener(button) {
		override fun clicked(event: InputEvent, x: Float, y: Float) {
			showDialog(config)
		}
	})
	return actor
}
