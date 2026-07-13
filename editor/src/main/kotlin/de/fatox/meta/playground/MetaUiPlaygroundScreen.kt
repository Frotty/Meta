package de.fatox.meta.playground

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.extensions.tooltip
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.api.ui.showWindow
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.bindText
import de.fatox.meta.ui.components.MetaArrayAdapter
import de.fatox.meta.ui.components.MetaBottomBar
import de.fatox.meta.ui.components.MetaButtonContainer
import de.fatox.meta.ui.components.MetaCheckBox
import de.fatox.meta.ui.components.MetaIcon
import de.fatox.meta.ui.components.MetaIconButton
import de.fatox.meta.ui.components.MetaIconButtonGroup
import de.fatox.meta.ui.components.MetaIconTextButton
import de.fatox.meta.ui.components.MetaImageButton
import de.fatox.meta.ui.components.MetaInputField
import de.fatox.meta.ui.components.MetaIntSpinnerModel
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaListView
import de.fatox.meta.ui.components.MetaLoadingSpinner
import de.fatox.meta.ui.components.MetaMenu
import de.fatox.meta.ui.components.MetaMenuBar
import de.fatox.meta.ui.components.MetaMenuItem
import de.fatox.meta.ui.components.MetaScrollPane
import de.fatox.meta.ui.components.MetaSelectBox
import de.fatox.meta.ui.components.MetaSeparator
import de.fatox.meta.ui.components.MetaSpinner
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextArea
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.MetaTextField
import de.fatox.meta.ui.components.SliderWithButtons

class MetaUiPlaygroundScreen(
	private val beforeShow: () -> Unit = {},
) : ScreenAdapter() {
	private val uiRenderer: UIRenderer by lazyInject()
	private val uiManager: UIManager by lazyInject()
	private val scope = ReactiveScope()
	private var isBuilt = false
	private var loadingSpinner: MetaLoadingSpinner? = null

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
		scope.dispose()
		loadingSpinner?.dispose()
	}

	override fun dispose() {
		uiManager.setMainMenuBar(null)
		scope.dispose()
		loadingSpinner?.dispose()
	}

	private fun clearFrame() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
		Gdx.gl.glClearColor(0.122f, 0.126f, 0.145f, 1f)
		Gdx.gl.glClearDepthf(1f)
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT or
			if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0)
	}

	private fun build() {
		uiManager.setMainMenuBar(playgroundToolbar())
		val page = MetaTable().apply {
			top().left()
			pad(MetaSpacing.LG)
			defaults().growX().padBottom(MetaSpacing.LG)
		}
		page.add(header()).growX().padTop(MetaSpacing.LG).row()
		page.add(typographySection()).growX().row()
		page.add(buttonSection()).growX().row()
		page.add(inputSection()).growX().row()
		page.add(selectionSection()).growX().row()
		page.add(listSection()).growX().row()
		page.add(windowSection()).growX().row()

		val root = MetaTable().apply {
			add(MetaScrollPane(page)).grow()
		}
		// Keep screen content in MetaUiManager's layout so it occupies the row below the main menu. A separate
		// fill-parent actor would overlap the menu, intercept its clicks, and let scrolling content show beneath it.
		uiManager.addTable(root, growX = true, growY = true)
		uiRenderer.addActor(MetaBottomBar(MetaLabel("Meta UI Component Playground", MetaType.CAPTION, MetaColor.TEXT))
			.bottomOverlay(MetaSpacing.SM))
	}

	private fun playgroundToolbar(): MetaMenuBar = MetaMenuBar().apply {
		val file = MetaMenu("File")
		file.addItem(MetaMenuItem("New project", "ri-file-add-line").apply {
			onChange { uiManager.showToast("New project") }
		})
		file.addItem(MetaMenuItem("Open…", "ri-folder-open-line").apply {
			onChange { uiManager.showToast("Open") }
		})
		file.addItem(MetaMenuItem("Save", "ri-save-line").apply {
			onChange { uiManager.showToast("Save") }
		})
		file.addSeparator()
		file.addItem(MetaMenuItem("Recent files", "ri-history-line").apply {
			disabledValue.value = true
		})
		addMenu(file)

		val edit = MetaMenu("Edit")
		edit.addItem(MetaMenuItem("Undo", "ri-arrow-go-back-line").apply {
			onChange { uiManager.showToast("Undo") }
		})
		edit.addItem(MetaMenuItem("Redo", "ri-arrow-go-forward-line").apply {
			disabledValue.value = true
		})
		edit.addSeparator()
		edit.addItem(MetaMenuItem("Preferences", "ri-settings-3-line").apply {
			onChange { uiManager.showToast("Preferences") }
		})
		addMenu(edit)

		val view = MetaMenu("View")
		view.addItem(MetaMenuItem("Show sample window", "ri-window-line").apply {
			onChange { uiManager.showWindow<PlaygroundSampleWindow>() }
		})
		view.addItem(MetaMenuItem("Show toast", "ri-notification-3-line").apply {
			onChange { uiManager.showToast("Hello from the Meta menu") }
		})
		view.addSeparator()
		view.addItem(MetaMenuItem("Reset layout", "ri-layout-line").apply {
			onChange { uiManager.showToast("Layout reset") }
		})
		addMenu(view)
		table.add().growX()
	}

	private fun header(): Table = MetaTable().apply {
		left()
		add(MetaLabel("Meta UI Component Playground", MetaType.HEADING, MetaColor.TEXT)).left().growX().row()
		add(MetaLabel("Interactive runtime widgets using Meta TTF text, Remix icons, generated skin drawables, and reactive bindings.",
			MetaType.BODY, MetaColor.TEXT_MUTED)).left().growX()
		row()
		add(MetaLabel("Open File, Edit, or View in the top menu bar to test Meta dropdown menus.",
			MetaType.CAPTION, MetaColor.ACCENT)).left().growX().padTop(MetaSpacing.XS)
	}

	private fun typographySection(): Table = section("Typography & Icons").apply {
		val scale = MetaTable()
		scale.defaults().padRight(MetaSpacing.LG).padBottom(MetaSpacing.SM).left()
		scale.add(MetaLabel("Caption 12", MetaType.CAPTION, MetaColor.TEXT_MUTED))
		scale.add(MetaLabel("Body 16", MetaType.BODY, MetaColor.TEXT))
		scale.add(MetaLabel("Label 18", MetaType.LABEL, MetaColor.TEXT))
		scale.add(MetaLabel("Subtitle 21", MetaType.SUBTITLE, MetaColor.TEXT)).row()
		scale.add(MetaLabel("Title 24", MetaType.TITLE, MetaColor.TEXT))
		scale.add(MetaLabel("Heading 32", MetaType.HEADING, MetaColor.TEXT))
		scale.add(MetaLabel("Display 48", MetaType.DISPLAY, MetaColor.TEXT))
		scale.add(MetaIcon("ri-information-line", 28, MetaColor.ACCENT.cpy()))
		scale.add(MetaIcon("ri-magic-line", 28, MetaColor.WARNING.cpy()))
		add(scale).growX().row()
	}

	private fun buttonSection(): Table = section("Buttons, Icon Buttons & Tooltips").apply {
		val status = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT_MUTED)
		val group = MetaIconButtonGroup()
		val brushButton = MetaIconButton("ri-brush-line").apply {
			name = "Brush"
			tooltip("Brush tool")
		}
		val pencilButton = MetaIconButton("ri-pencil-line").apply {
			name = "Pencil"
			tooltip("Pencil tool")
		}
		val paintButton = MetaIconButton("ri-paint-line").apply {
			name = "Paint bucket"
			tooltip("Paint bucket")
		}
		val magicButton = MetaImageButton("ri-magic-line", size = 22, color = MetaColor.TEXT.cpy()).apply {
			tooltip("Subtle image button")
			onClick { uiManager.showToast("Image button clicked") }
		}
		val row = MetaTable()
		row.defaults().padRight(MetaSpacing.SM).height(42f)
		row.add(MetaTextButton("Text Button", MetaType.BODY)
			.onClick { uiManager.showToast("Text button clicked") })
		row.add(MetaIconTextButton("Open", "ri-folder-open-line", size = MetaType.CAPTION, iconSize = 22)
			.onClick { uiManager.showToast("Icon text button clicked") })
		row.add(group.add(brushButton, selected = true)).size(38f)
		row.add(group.add(pencilButton)).size(38f)
		row.add(group.add(paintButton)).size(38f)
		row.add(magicButton).size(34f)
		add(row).left().growX().row()
		add(status).left().growX()
		scope.bindText(status) { "Active icon: ${group.selectedButtonValue()?.name ?: "None"}" }
	}

	private fun inputSection(): Table = section("Inputs & Validation").apply {
		val textField = MetaTextField("", MetaType.BODY, placeholder = "Standard MetaTextField with placeholder")
		val inputField = MetaInputField("", MetaType.BODY, placeholder = "MetaInputField with placeholder")
		val area = MetaTextArea("Multiline MetaTextArea\nEdit me.", MetaType.BODY, prefRows = 3f)
		val check = MetaCheckBox(initialChecked = true)
		val checkLabel = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT_MUTED)

		val grid = MetaTable()
		grid.defaults().pad(MetaSpacing.SM).left()
		grid.add(MetaLabel("Standard field", MetaType.CAPTION, MetaColor.TEXT_MUTED)).width(120f)
		grid.add(textField).growX().height(34f).row()
		grid.add(MetaLabel("Validated field", MetaType.CAPTION, MetaColor.TEXT_MUTED)).width(120f)
		grid.add(inputField).growX().height(34f).row()
		grid.add(MetaLabel("Text area", MetaType.CAPTION, MetaColor.TEXT_MUTED)).width(120f).top()
		grid.add(area).growX().height(90f).row()
		grid.add(MetaLabel("Checkbox", MetaType.CAPTION, MetaColor.TEXT_MUTED)).width(120f)
		val checkRow = MetaTable()
		checkRow.add(check).size(28f).padRight(MetaSpacing.SM)
		checkRow.add(checkLabel).left()
		grid.add(checkRow).left().growX()
		add(grid).growX().row()
		add(MetaLabel(
			"Use MetaTextField for normal editing. MetaInputField adds validators and reactive valid/invalid state for MetaInputLayout forms.",
			MetaType.CAPTION,
			MetaColor.TEXT_MUTED,
		)).left().growX()
		scope.bindText(checkLabel) { if (check.checkedValue()) "Checked with Remix checkmark" else "Unchecked" }
	}

	private fun selectionSection(): Table = section("Selection, Spinner & Slider").apply {
		val select = MetaSelectBox<String>(MetaType.BODY).apply {
			setItems("Small", "Medium", "Large", "Screenshot")
			selected = "Medium"
		}
		val spinner = MetaSpinner(MetaIntSpinnerModel(4, 0, 12, 1), MetaType.BODY)
		val loading = MetaLoadingSpinner(32f, 3.5f).also { loadingSpinner = it }
		val slider = SliderWithButtons(0f, 100f, 5f, false)
		val sliderLabel = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT_MUTED)

		val grid = MetaTable()
		grid.defaults().pad(MetaSpacing.SM).left()
		grid.add(MetaLabel("Select", MetaType.CAPTION, MetaColor.TEXT_MUTED)).width(120f)
		grid.add(select).growX().height(34f).row()
		grid.add(MetaLabel("Spinner", MetaType.CAPTION, MetaColor.TEXT_MUTED)).width(120f)
		grid.add(spinner).width(170f).left().row()
		grid.add(MetaLabel("Loading", MetaType.CAPTION, MetaColor.TEXT_MUTED)).width(120f)
		grid.add(loading).size(32f).left().row()
		grid.add(MetaLabel("Slider", MetaType.CAPTION, MetaColor.TEXT_MUTED)).width(120f)
		grid.add(slider).growX().height(42f).row()
		grid.add()
		grid.add(sliderLabel).growX()
		add(grid).growX()
		scope.bindText(sliderLabel) { "Slider value: ${slider.valueValue().toInt()}" }
	}

	private fun listSection(): Table = section("Scroll Pane & List View").apply {
		val items = Array<String>().apply {
			add("Runtime widgets")
			add("Reactive bindings")
			add("Remix icons")
			add("Generated skin")
			add("Window chrome")
			add("Tooltips")
		}
		val selection = MetaLabel("Select an item", MetaType.CAPTION, MetaColor.TEXT_MUTED)
		val list = MetaListView(object : MetaArrayAdapter<String, MetaTextButton>(items) {
			override fun createView(item: String): MetaTextButton = MetaTextButton(item, MetaType.CAPTION).apply { leftText() }
			override fun selectView(view: Actor) {
				(view as? MetaTextButton)?.isChecked = true
			}
			override fun deselectView(view: Actor) {
				(view as? MetaTextButton)?.isChecked = false
			}
		})
		list.setItemClickListener { selection.setText("Selected: $it") }
		add(list.scrollPane).height(170f).growX().row()
		add(selection).left().growX()
	}

	private fun windowSection(): Table = section("Windows, Toasts & Custom Containers").apply {
		val row = MetaTable()
		row.defaults().padRight(MetaSpacing.SM).height(42f)
		row.add(MetaIconTextButton("Show Window", "ri-window-line", size = MetaType.CAPTION, iconSize = 22)
			.onClick { uiManager.showWindow<PlaygroundSampleWindow>() })
		row.add(MetaIconTextButton("Toast", "ri-notification-3-line", size = MetaType.CAPTION, iconSize = 22)
			.onClick { uiManager.showToast("Hello from the Meta playground") })
		row.add(MetaButtonContainer().apply {
			add(MetaIcon("ri-settings-3-line", 22, MetaColor.TEXT.cpy())).padRight(MetaSpacing.XS)
			add(MetaLabel("Custom Container", MetaType.CAPTION, MetaColor.TEXT))
		}).height(42f)
		add(row).left().growX()
	}

	private fun section(title: String): MetaTable = MetaTable().apply {
		top().left()
		defaults().growX()
		add(MetaLabel(title, MetaType.TITLE, MetaColor.TEXT)).left().growX().row()
		add(MetaSeparator()).height(1f).growX().padBottom(MetaSpacing.SM).row()
	}
}
