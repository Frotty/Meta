package de.fatox.meta.playground

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.HdpiUtils
import com.badlogic.gdx.scenes.scene2d.ui.Window
import de.fatox.meta.api.extensions.onChange
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
		if (isBuilt) return
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
		uiManager.setMainMenuBar(playgroundMenuBar())
		uiManager.addTable(workspaceBackground(), growX = true, growY = true)
		uiManager.setBottomOverlay(
			MetaBottomBar(MetaLabel("Meta UI editor playground", MetaType.CAPTION, MetaColor.TEXT))
				.bottomOverlay(MetaSpacing.SM),
		)
		showWorkspace(resetLayout = true)
	}

	private fun workspaceBackground() = MetaTable().apply {
		top().left()
		pad(MetaSpacing.LG)
		add(MetaLabel("META UI WORKSPACE", MetaType.CAPTION, MetaColor.TEXT_MUTED)).left().row()
		add(MetaLabel("Move, resize, overlap, close, and reopen the component windows.", MetaType.BODY, MetaColor.TEXT_MUTED))
			.left().padTop(MetaSpacing.XS)
	}

	private fun playgroundMenuBar(): MetaMenuBar = MetaMenuBar().apply {
		addMenu(MetaMenu("File").apply {
			addItem(action("New workspace", "ri-file-add-line") { showWorkspace(resetLayout = true) })
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
			addItem(action("Typography & icons", "ri-font-size") { uiManager.showWindow<TypographyPlaygroundWindow>() })
			addItem(action("Buttons & input", "ri-input-method-line") { uiManager.showWindow<ControlsPlaygroundWindow>() })
			addItem(action("Selection & progress", "ri-equalizer-line") { uiManager.showWindow<SelectionPlaygroundWindow>() })
			addItem(action("Lists & scrolling", "ri-list-check-2") { uiManager.showWindow<CollectionsPlaygroundWindow>() })
			addSeparator()
			addItem(action("Show all", "ri-layout-grid-line") { showWorkspace(resetLayout = false) })
			addItem(action("Reset workspace", "ri-layout-line") { showWorkspace(resetLayout = true) })
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

	private fun showWorkspace(resetLayout: Boolean) {
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
}
