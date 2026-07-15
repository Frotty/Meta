package de.fatox.meta.playground

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.scenes.scene2d.ui.Window
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.ui.MetaDockConfig
import de.fatox.meta.api.ui.MetaDockSide
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.api.ui.showWindow
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
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
		uiManager.setBottomOverlay(
			MetaBottomBar(MetaLabel(
				"Drag a panel to an edge to dock · drag away to float · resize fixed panel dividers",
				MetaType.CAPTION,
				MetaColor.TEXT,
			))
				.bottomOverlay(MetaSpacing.SM),
		)
		val firstDockingLaunch = !uiManager.metaHas(DOCKING_INITIALIZED_KEY)
		showDockWorkspace(resetLayout = firstDockingLaunch, seedFloatingBounds = firstDockingLaunch)
		if (firstDockingLaunch) uiManager.metaSave(DOCKING_INITIALIZED_KEY, true)
	}

	private fun workspaceBackground() = MetaTable().apply {
		top().left()
		pad(MetaSpacing.LG)
		add(MetaLabel("META UI WORKSPACE", MetaType.CAPTION, MetaColor.TEXT_MUTED)).left().row()
		add(MetaLabel(
			"Test left/right docking, insertion order, fill reflow, drag-away restore, resize, persistence, and narrow windows.",
			MetaType.BODY,
			MetaColor.TEXT_MUTED,
		))
			.left().padTop(MetaSpacing.XS)
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
			addItem(action("Tools", "ri-tools-line") { uiManager.showWindow<DockToolsPlaygroundWindow>() })
			addItem(action("Layers", "ri-stack-line") { uiManager.showWindow<DockLayersPlaygroundWindow>() })
			addItem(action("Inspector", "ri-settings-3-line") { uiManager.showWindow<DockInspectorPlaygroundWindow>() })
			addItem(action("Activity", "ri-history-line") { uiManager.showWindow<DockActivityPlaygroundWindow>() })
			addSeparator()
			addItem(action("Show dock workspace", "ri-layout-column-line") { showDockWorkspace(resetLayout = false) })
			addItem(action("Reset dock workspace", "ri-layout-left-line") {
				showDockWorkspace(resetLayout = true, seedFloatingBounds = true)
			})
			addItem(action("Undock all panels", "ri-drag-move-2-line") { undockDockPanels() })
			addSeparator()
			addItem(action("Typography & icons", "ri-font-size") { uiManager.showWindow<TypographyPlaygroundWindow>() })
			addItem(action("Buttons & input", "ri-input-method-line") { uiManager.showWindow<ControlsPlaygroundWindow>() })
			addItem(action("Selection & progress", "ri-equalizer-line") { uiManager.showWindow<SelectionPlaygroundWindow>() })
			addItem(action("Lists & scrolling", "ri-list-check-2") { uiManager.showWindow<CollectionsPlaygroundWindow>() })
			addSeparator()
			addItem(action("Show component gallery", "ri-layout-grid-line") { showComponentGallery(resetLayout = true) })
		})

		addMenu(MetaMenu("Help").apply {
			addItem(action("Show toast", "ri-notification-3-line") {
				uiManager.showToast("Menus, windows, reactive controls, and overlays are active")
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
		if (resetLayout) closeComponentWindows()
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
				is CollectionsPlaygroundWindow -> uiManager.closeWindow(window)
			}
		}
	}

	private fun showComponentGallery(resetLayout: Boolean) {
		val typography = uiManager.showWindow<TypographyPlaygroundWindow>()
		val controls = uiManager.showWindow<ControlsPlaygroundWindow>()
		val selection = uiManager.showWindow<SelectionPlaygroundWindow>()
		val collections = uiManager.showWindow<CollectionsPlaygroundWindow>()
		if (!resetLayout) return

		val margin = MetaSpacing.MD
		val gap = MetaSpacing.MD
		val topInset = 42f
		val bottomInset = 34f
		val availableWidth = (uiManager.uiWidth - margin * 2f - gap).coerceAtLeast(720f)
		val availableHeight = (uiManager.uiHeight - topInset - bottomInset - margin * 2f - gap).coerceAtLeast(480f)
		val leftWidth = (availableWidth * 0.42f).coerceAtLeast(360f)
		val rightWidth = availableWidth - leftWidth
		// Keep the lower list window deliberately short so its default presentation exercises real scrolling.
		val topHeight = availableHeight * 0.55f
		val bottomHeight = availableHeight - topHeight
		val bottomY = bottomInset + margin
		val topY = bottomY + bottomHeight + gap
		val rightX = margin + leftWidth + gap

		place(typography, margin, topY, leftWidth, topHeight)
		place(controls, rightX, topY, rightWidth, topHeight)
		place(selection, margin, bottomY, leftWidth, bottomHeight)
		place(collections, rightX, bottomY, rightWidth, bottomHeight)
		uiManager.bringWindowsToFront()
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
