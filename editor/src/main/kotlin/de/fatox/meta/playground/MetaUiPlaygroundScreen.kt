package de.fatox.meta.playground

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.scenes.scene2d.ui.Window
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.ui.MetaDockConfig
import de.fatox.meta.api.ui.MetaDockSide
import de.fatox.meta.api.ui.MetaToastSpec
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.api.ui.showWindow
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.UiControlHelper
import de.fatox.meta.ui.components.MetaBottomBar
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaMenu
import de.fatox.meta.ui.components.MetaMenuBar
import de.fatox.meta.ui.components.MetaMenuItem
import de.fatox.meta.ui.components.MetaTable

class MetaUiPlaygroundScreen(
	private val beforeShow: () -> Unit = {},
) : ScreenAdapter() {
	private val uiRenderer: UIRenderer by lazyInject()
	private val uiManager: UIManager by lazyInject()
	private val uiControlHelper: UiControlHelper by lazyInject()
	private val backgroundTitle = MetaLabel("META UI WORKSPACE", MetaType.CAPTION, MetaColor.TEXT_MUTED)
	private val backgroundDescription = MetaLabel("", MetaType.BODY, MetaColor.TEXT_MUTED)
	private val bottomCaption = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT)
	private var isBuilt = false

	override fun show() {
		if (isBuilt) {
			configureDocking()
			return
		}
		beforeShow()
		uiRenderer.load()
		uiManager.resize(Gdx.graphics.width, Gdx.graphics.height)
		build()
		isBuilt = true
	}

	override fun render(delta: Float) {
		uiRenderer.update()
		clearFrame()
		uiRenderer.draw()
	}

	override fun resize(width: Int, height: Int) {
		uiManager.resize(width, height)
	}

	override fun hide() {
		uiManager.setMainMenuBar(null)
		uiManager.setBottomOverlay(null)
	}

	override fun dispose() {
		uiManager.setMainMenuBar(null)
		uiManager.setBottomOverlay(null)
	}

	private fun clearFrame() {
		HdpiUtils.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
		Gdx.gl.glClearColor(0.105f, 0.109f, 0.125f, 1f)
		Gdx.gl.glClearDepthf(1f)
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT or
			if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0)
	}

	private fun build() {
		configureDocking()
		uiManager.setMainMenuBar(playgroundMenuBar())
		uiManager.addTable(workspaceBackground(), growX = true, growY = true)
		uiManager.setBottomOverlay(MetaBottomBar(bottomCaption).bottomOverlay(MetaSpacing.SM))
		val firstDockingLaunch = !uiManager.metaHas(DOCKING_INITIALIZED_KEY)
		showDockWorkspace(resetLayout = firstDockingLaunch, seedFloatingBounds = firstDockingLaunch)
		if (firstDockingLaunch) uiManager.metaSave(DOCKING_INITIALIZED_KEY, true)
	}

	private fun workspaceBackground() = MetaTable().apply {
		center()
		pad(MetaSpacing.LG)
		add(backgroundTitle).center().row()
		add(backgroundDescription)
			.center().padTop(MetaSpacing.XS)
	}

	private fun playgroundMenuBar(): MetaMenuBar = MetaMenuBar().apply {
		addMenu(MetaMenu("File").apply {
			addItem(action("New workspace", "ri-file-add-line") {
				showDockWorkspace(resetLayout = true, seedFloatingBounds = true)
			})
			addItem(action("Open…", "ri-folder-open-line") { uiManager.showToast("Open action") })
			addItem(action("Save", "ri-save-line") { uiManager.showToast("Workspace saved") })
			addSeparator()
			addItem(MetaMenuItem("Recent workspaces", "ri-history-line").apply { disabledValue.value = true })
		})

		addMenu(MetaMenu("Edit").apply {
			addItem(action("Undo", "ri-arrow-go-back-line") { uiManager.showToast("Undo") })
			addItem(MetaMenuItem("Redo", "ri-arrow-go-forward-line").apply { disabledValue.value = true })
			addSeparator()
			addItem(action("Preferences", "ri-settings-3-line") { uiManager.showToast("Preferences") })
		})

		addMenu(MetaMenu("Window").apply {
			addItem(action("Show dock workspace", "ri-layout-column-line") {
				showDockWorkspace(resetLayout = false)
			})
			addItem(action("Show component showcase", "ri-layout-grid-line") { showComponentShowcase() })
			addSeparator()
			addItem(action("Tools", "ri-tools-line") { uiManager.showWindow<DockToolsPlaygroundWindow>() })
			addItem(action("Layers", "ri-stack-line") { uiManager.showWindow<DockLayersPlaygroundWindow>() })
			addItem(action("Inspector", "ri-settings-3-line") { uiManager.showWindow<DockInspectorPlaygroundWindow>() })
			addItem(action("Activity", "ri-history-line") { uiManager.showWindow<DockActivityPlaygroundWindow>() })
			addItem(action("Reset dock workspace", "ri-layout-left-line") {
				showDockWorkspace(resetLayout = true, seedFloatingBounds = true)
			})
			addItem(action("Undock all panels", "ri-drag-move-2-line") { undockDockPanels() })
			addSeparator()
			addItem(action("Typography & icons", "ri-font-size") { uiManager.showWindow<TypographyPlaygroundWindow>() })
			addItem(action("Buttons & input", "ri-input-method-line") { uiManager.showWindow<ControlsPlaygroundWindow>() })
			addItem(action("Selection & progress", "ri-equalizer-line") { uiManager.showWindow<SelectionPlaygroundWindow>() })
			addItem(action("Color picker", "ri-palette-line") {
				uiManager.showWindow<ColorPickerPlaygroundWindow>().openColorPicker()
			})
			addItem(action("Lists & scrolling", "ri-list-check-2") { uiManager.showWindow<CollectionsPlaygroundWindow>() })
		})

		addMenu(MetaMenu("Help").apply {
			addItem(action("Notification toast", "ri-notification-3-line") {
				uiManager.showToast("Menus, windows, reactive controls, and overlays are active")
			})
			addItem(action("Persistent invite", "ri-team-line") {
				uiManager.showToast(MetaToastSpec.invite("Frotty invited you to join a game.", onAccept = {
					uiManager.showToast("Invite accepted")
				}))
			})
			addItem(action("Persistent error", "ri-error-warning-line") {
				uiManager.showToast(MetaToastSpec.error("Could not connect to the game session."))
			})
			addSeparator()
			addItem(MetaMenuItem("Meta UI Playground", "ri-information-line").apply { disabledValue.value = true })
		})
		table.add().growX()
	}

	private fun action(text: String, icon: String, action: () -> Unit) = MetaMenuItem(text, icon).apply {
		onChange { action() }
	}

	private fun configureDocking() {
		uiManager.configureWindowDocking(DOCK_CONFIG)
	}

	private fun showDockWorkspace(resetLayout: Boolean, seedFloatingBounds: Boolean = false) {
		configureDocking()
		closeComponentWindows()
		showWorkspacePresentation()
		val tools = uiManager.showWindow<DockToolsPlaygroundWindow>()
		val layers = uiManager.showWindow<DockLayersPlaygroundWindow>()
		val inspector = uiManager.showWindow<DockInspectorPlaygroundWindow>()
		val activity = uiManager.showWindow<DockActivityPlaygroundWindow>()
		if (resetLayout) {
			if (seedFloatingBounds) seedFloatingDockPanelBounds(tools, layers, inspector, activity)
			uiManager.dockWindow(tools, MetaDockSide.LEFT, order = 0, height = 190f)
			uiManager.dockWindow(layers, MetaDockSide.LEFT, order = 100, fill = true)
			uiManager.dockWindow(inspector, MetaDockSide.RIGHT, order = 0, height = 285f)
			uiManager.dockWindow(activity, MetaDockSide.RIGHT, order = 100, fill = true)
			uiManager.metaSave(DOCKING_INITIALIZED_KEY, true)
		}
		uiManager.bringWindowsToFront()
	}

	private fun showComponentShowcase() {
		closeDockWindows()
		closeComponentWindows()
		backgroundTitle.setText("META UI COMPONENT SHOWCASE")
		backgroundDescription.setText(
			"Typography, icons, controls, reactive input, selection, progress, menus, lists, and scrolling.",
		)
		bottomCaption.setText("Arrow keys / D-pad: navigate  ·  Enter / A: activate  ·  Esc / B: back")
		backgroundTitle.isVisible = false
		backgroundDescription.isVisible = false
		showComponentGallery(resetLayout = true)
	}

	private fun showWorkspacePresentation() {
		backgroundTitle.isVisible = true
		backgroundDescription.isVisible = true
		backgroundTitle.setText("META UI WORKSPACE")
		backgroundDescription.setText(
			"Test left/right docking, insertion order, fill reflow, drag-away restore, resize, persistence, and narrow windows.",
		)
		bottomCaption.setText("Drag a panel to an edge to dock · drag away to float · resize fixed panel dividers")
	}

	private fun seedFloatingDockPanelBounds(
		tools: Window,
		layers: Window,
		inspector: Window,
		activity: Window,
	) {
		val center = uiManager.uiWidth / 2f
		val leftX = (center - 280f).coerceAtLeast(MetaSpacing.SM)
		val rightX = (center + 40f).coerceAtMost(uiManager.uiWidth - 318f)
		place(tools, leftX, (uiManager.uiHeight - 250f).coerceAtLeast(80f), 250f, 190f)
		place(layers, leftX, 80f, 250f, 360f)
		place(inspector, rightX, (uiManager.uiHeight - 345f).coerceAtLeast(80f), 310f, 285f)
		place(activity, rightX, 80f, 310f, 360f)
	}

	private fun undockDockPanels() {
		val active = uiManager.currentlyActiveWindows
		for (index in 0 until active.size) {
			when (val window = active[index]) {
				is DockToolsPlaygroundWindow,
				is DockLayersPlaygroundWindow,
				is DockInspectorPlaygroundWindow,
				is DockActivityPlaygroundWindow -> uiManager.undockWindow(window)
			}
		}
		uiManager.showToast("Dock panels restored to their last floating bounds")
	}

	private fun closeComponentWindows() {
		val active = uiManager.currentlyActiveWindows
		for (index in active.size - 1 downTo 0) {
			when (val window = active[index]) {
				is TypographyPlaygroundWindow,
				is ControlsPlaygroundWindow,
				is SelectionPlaygroundWindow,
				is ColorPickerPlaygroundWindow,
				is CollectionsPlaygroundWindow -> uiManager.closeWindow(window)
			}
		}
	}

	private fun closeDockWindows() {
		val active = uiManager.currentlyActiveWindows
		for (index in active.size - 1 downTo 0) {
			when (val window = active[index]) {
				is DockToolsPlaygroundWindow,
				is DockLayersPlaygroundWindow,
				is DockInspectorPlaygroundWindow,
				is DockActivityPlaygroundWindow -> uiManager.closeWindow(window)
			}
		}
	}

	private fun showComponentGallery(resetLayout: Boolean) {
		val typography = uiManager.showWindow<TypographyPlaygroundWindow>()
		val controls = uiManager.showWindow<ControlsPlaygroundWindow>()
		val selection = uiManager.showWindow<SelectionPlaygroundWindow>()
		val collections = uiManager.showWindow<CollectionsPlaygroundWindow>()
		if (!resetLayout) return
		// Screenshot mode is deliberately floating, regardless of dock state persisted by an earlier session.
		uiManager.undockWindow(typography)
		uiManager.undockWindow(controls)
		uiManager.undockWindow(selection)
		uiManager.undockWindow(collections)
		uiManager.configureWindowDocking(null)

		val margin = MetaSpacing.MD
		val gap = MetaSpacing.MD
		val topInset = 42f
		val bottomInset = 34f
		val availableWidth = (uiManager.uiWidth - margin * 2f - gap).coerceAtLeast(720f)
		val availableHeight = (uiManager.uiHeight - topInset - bottomInset - margin * 2f - gap).coerceAtLeast(480f)
		val leftWidth = availableWidth * 0.5f
		val rightWidth = availableWidth - leftWidth
		val topHeight = availableHeight * 0.5f
		val bottomHeight = availableHeight - topHeight
		val bottomY = bottomInset + margin
		val topY = bottomY + bottomHeight + gap
		val rightX = margin + leftWidth + gap

		place(typography, margin, topY, leftWidth, topHeight)
		place(controls, rightX, topY, rightWidth, topHeight)
		place(selection, margin, bottomY, leftWidth, bottomHeight)
		place(collections, rightX, bottomY, rightWidth, bottomHeight)
		uiManager.bringWindowsToFront()
		uiControlHelper.focusFirstIn(controls, controls.navigationStart)
	}

	private fun place(window: Window, x: Float, y: Float, width: Float, height: Float) {
		window.setBounds(x, y, width, height)
		uiManager.updateWindow(window)
	}

	private companion object {
		const val DOCKING_INITIALIZED_KEY = "DockingPlaygroundInitialized"
		val DOCK_CONFIG = MetaDockConfig(
			leftWidth = 250f,
			rightWidth = 310f,
			margin = MetaSpacing.SM,
			gap = MetaSpacing.XS,
			topInset = 42f,
			bottomInset = 38f,
			snapDistance = 28f,
			minimumCenterWidth = 420f,
		)
	}
}
