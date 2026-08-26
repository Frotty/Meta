package de.fatox.meta.test

import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import de.fatox.meta.ScreenConfig
import de.fatox.meta.api.NoWindowHandler
import de.fatox.meta.api.WindowHandler
import de.fatox.meta.api.ui.MetaDockConfig
import de.fatox.meta.api.ui.MetaDockSide
import de.fatox.meta.api.ui.MetaToastSpec
import de.fatox.meta.api.ui.MetaWindowInteraction
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.WindowConfig
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.components.MetaMenuBar
import de.fatox.meta.ui.tabs.MetaTab
import de.fatox.meta.ui.windows.MetaDialog
import kotlin.reflect.KClass

/**
 * A [UIManager] that records what it was asked and manages nothing.
 *
 * It exists so a `MetaDialog` subclass can be tested at all. `MetaDialog` and `MetaWindow` reach the manager on paths
 * a test cannot avoid — `close()` calls `onDialogRemoved`, detaching from the stage calls it again, setting a bottom
 * overlay calls `onDialogBottomOverlayChanged` — so without something answering for this interface a dialog could be
 * constructed and never shown or closed. `MetaKeyRebindDialog` shipped with no coverage for exactly that reason.
 *
 * Only the members a dialog or window actually touches do anything: [onDialogRemoved],
 * [onDialogBottomOverlayChanged], [closeWindow], [previewWindowDock] and all three `updateWindow` overloads. Those
 * record. Everything
 * else is a no-op or an empty value, and the three that cannot honestly return one — [showWindow], [getWindow],
 * [showDialog] — throw with a message saying so, because a fixture inventing a window would be lying about the thing
 * a test is checking.
 *
 * ```
 * MetaHeadlessUi.install(input = { DispatchingInput() }, uiManager = { RecordingUiManager() })
 * ```
 *
 * Toasts land in [toasts] rather than being dropped, so a test can assert a notification was raised without standing
 * up a `MetaToastManager` and a stage for it.
 *
 * `open`, because a test wanting one member to behave differently — a `dispose` that records, say — should subclass
 * rather than reimplement forty-three of them.
 */
open class RecordingUiManager : UIManager {

	/** Dialogs whose removal this manager was told about, in order. */
	val removedDialogs = ArrayList<MetaDialog>()

	/** Dialogs that reported a change of bottom overlay. */
	val overlayChanges = ArrayList<MetaDialog>()

	/** Windows this manager was asked to close. */
	val closedWindows = ArrayList<Window>()

	/**
	 * Every `updateWindow` notification, live and committed.
	 *
	 * All three overloads are overridden, which is not redundant: `MetaWindow.draw` reports a move or resize in
	 * progress as `updateWindow(window, interaction, finished = false)`, and the interface default for that drops it
	 * unless `finished` — so overriding only the one-argument form recorded nothing for the whole of a gesture while
	 * the manager was in fact being notified throughout.
	 */
	val windowUpdates = ArrayList<WindowUpdate>()

	/** Dock-preview notifications. `null` means the preview was cleared. Kept apart from [windowUpdates]. */
	val dockPreviews = ArrayList<Window?>()

	/** One notification: which window, which gesture if any, and whether it was the committing call. */
	data class WindowUpdate(val window: Window, val interaction: MetaWindowInteraction?, val finished: Boolean)

	/** Toast messages raised, in order. */
	val toasts = ArrayList<String>()

	override var windowHandler: WindowHandler = NoWindowHandler

	override fun onDialogRemoved(dialog: MetaDialog) {
		removedDialogs.add(dialog)
	}

	override fun onDialogBottomOverlayChanged(dialog: MetaDialog) {
		overlayChanges.add(dialog)
	}

	override fun closeWindow(window: Window) {
		closedWindows.add(window)
	}

	override fun updateWindow(window: Window) {
		windowUpdates.add(WindowUpdate(window, interaction = null, finished = true))
	}

	override fun updateWindow(window: Window, interaction: MetaWindowInteraction) {
		windowUpdates.add(WindowUpdate(window, interaction, finished = true))
	}

	override fun updateWindow(window: Window, interaction: MetaWindowInteraction, finished: Boolean) {
		windowUpdates.add(WindowUpdate(window, interaction, finished))
	}

	override fun previewWindowDock(window: Window?) {
		dockPreviews.add(window)
	}

	override fun showToast(message: String, duration: Float) {
		toasts.add(message)
	}

	override fun showToast(table: Table, duration: Float) {
		toasts.add(table.toString())
	}

	override fun showToast(spec: MetaToastSpec) {
		toasts.add(spec.message)
	}

	override fun clearToasts() {
		toasts.clear()
	}

	// ── Deliberately unanswerable ─────────────────────────────────────────────

	override fun <T : Window> showWindow(windowClass: KClass<out T>): T =
		unsupported("showWindow(${windowClass.simpleName})")

	override fun <T : Window> getWindow(windowClass: KClass<out T>): T =
		unsupported("getWindow(${windowClass.simpleName})")

	override fun <T : MetaDialog> showDialog(dialogClass: KClass<out T>, showBackdrop: Boolean): T =
		unsupported("showDialog(${dialogClass.simpleName}) — construct the dialog and add it to a Stage instead")

	private fun unsupported(what: String): Nothing = throw UnsupportedOperationException(
		"RecordingUiManager records; it does not manage windows. $what has no honest answer here, and returning an " +
			"invented one would make a test pass against a build where the real manager would have failed.",
	)

	// ── No-ops ────────────────────────────────────────────────────────────────

	override fun moveWindow(x: Int, y: Int) = Unit
	override fun resize(width: Int, height: Int) = Unit
	override fun <T : Screen> changeScreen(screenClass: KClass<T>) = Unit
	override fun <T : MetaTab> changeTab(tabClass: KClass<T>) = Unit
	override fun addTable(table: Table?, growX: Boolean, growY: Boolean) = Unit
	override fun hideOtherWindowsAndPreventNew(window: Window) = Unit
	override fun restoreOtherWindowsAndAllowNew() = Unit
	override fun setMainMenuBar(menuBar: MetaMenuBar?) = Unit
	override fun configureWindowDocking(config: MetaDockConfig?) = Unit
	override fun dockWindow(window: Window, side: MetaDockSide, order: Int, height: Float, fill: Boolean) = Unit
	override fun undockWindow(window: Window) = Unit
	override fun bringWindowsToFront() = Unit
	override fun metaHas(name: String): Boolean = false
	override fun <T : Any> metaGet(name: String, c: KClass<out T>): T? = null
	override fun metaSave(name: String, windowData: Any) = Unit
	override fun setBottomOverlay(overlay: Actor?) = Unit

	override val currentlyActiveWindows: Array<Window> = Array()
	override val windowConfig: WindowConfig = WindowConfig()
	override val uiWidth: Float = SURFACE_WIDTH
	override val uiHeight: Float = SURFACE_HEIGHT
	override val preventShowWindowState: ReactiveValue<Boolean> = signal(false)
	override val preventShowWindow: Boolean get() = false

	@Deprecated("Bind to preventShowWindowState instead, e.g. actor.bindDisabled { uiManager.preventShowWindowState() }.")
	override val preventShowWindowObservers: Array<(Boolean) -> Unit> = Array()
	override val screenConfig: ScreenConfig = ScreenConfig()

	private companion object {
		/** A plausible desktop surface, so a dialog that centres itself lands somewhere sane. */
		const val SURFACE_WIDTH = 1920f
		const val SURFACE_HEIGHT = 1080f
	}
}

/**
 * A [Stage] for a real [de.fatox.meta.ui.MetaToastManager], owned by [MetaHeadlessUi] and disposed with it.
 *
 * A `Stage` creates a `SpriteBatch`, and the documented `MetaToastManager(toastStage())` one-liner gives the caller
 * nowhere to keep the reference — so an install/dispose cycle per test would retain a batch each time. Handing it to
 * the harness keeps the one-liner and puts the lifetime where the rest of the fixture's already is.
 */
fun toastStage(): Stage = Stage().also { MetaHeadlessUi.own(it) }
