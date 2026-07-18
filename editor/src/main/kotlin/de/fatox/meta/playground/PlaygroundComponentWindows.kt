package de.fatox.meta.playground

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.extensions.tooltip
import de.fatox.meta.api.ui.showWindow
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaButtonTier
import de.fatox.meta.ui.MetaControlSize
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.bindColor
import de.fatox.meta.ui.bindText
import de.fatox.meta.ui.components.MetaArrayAdapter
import de.fatox.meta.ui.components.MetaActionRow
import de.fatox.meta.ui.components.MetaButtonContainer
import de.fatox.meta.ui.components.MetaCheckBox
import de.fatox.meta.ui.components.MetaColorPicker
import de.fatox.meta.ui.components.MetaColorPickerListener
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
import de.fatox.meta.ui.components.MetaMenuItem
import de.fatox.meta.ui.components.MetaSelectBox
import de.fatox.meta.ui.components.MetaSeparator
import de.fatox.meta.ui.components.MetaSpinner
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextArea
import de.fatox.meta.ui.components.MetaTextButton
import de.fatox.meta.ui.components.MetaTextField
import de.fatox.meta.ui.components.SliderWithButtons
import de.fatox.meta.ui.windows.MetaWindow
import kotlin.math.roundToInt

class TypographyPlaygroundWindow : MetaWindow("Typography & Icons", resizable = true, closeButton = true) {
	init {
		setDefaultSize(430f, 300f)
		contentTable.defaults().left().growX()
		contentTable.add(MetaLabel("TYPE SCALE", MetaType.CAPTION, MetaColor.ACCENT)).padBottom(MetaSpacing.SM).row()
		contentTable.add(MetaLabel("Display", MetaType.DISPLAY, MetaColor.TEXT)).row()
		contentTable.add(MetaLabel("Heading", MetaType.HEADING, MetaColor.TEXT)).row()
		contentTable.add(MetaLabel("Title", MetaType.TITLE, MetaColor.TEXT)).row()
		contentTable.add(MetaLabel("Subtitle", MetaType.SUBTITLE, MetaColor.TEXT)).row()
		contentTable.add(MetaLabel("Label", MetaType.LABEL, MetaColor.TEXT)).row()
		contentTable.add(MetaLabel("Body text for normal interface content.", MetaType.BODY, MetaColor.TEXT)).row()
		contentTable.add(MetaLabel("Caption and supporting information", MetaType.CAPTION, MetaColor.TEXT_MUTED)).row()
		contentTable.add(MetaSeparator()).height(1f).padTop(MetaSpacing.MD).padBottom(MetaSpacing.MD).row()

		val icons = MetaTable().apply {
			defaults().size(MetaControlSize.COMPACT.iconTarget).padRight(MetaSpacing.SM)
			add(MetaIcon("ri-information-line", 24, MetaColor.ACCENT.cpy()))
			add(MetaIcon("ri-magic-line", 24, MetaColor.WARNING.cpy()))
			add(MetaIcon("ri-folder-open-line", 24, MetaColor.TEXT.cpy()))
			add(MetaIcon("ri-settings-3-line", 24, MetaColor.TEXT_MUTED.cpy()))
			add(MetaIcon("ri-checkbox-circle-line", 24, MetaColor.POSITIVE.cpy()))
		}
		contentTable.add(icons).left()
	}
}

class ControlsPlaygroundWindow : MetaWindow("Buttons & Input", resizable = true, closeButton = true) {
	val navigationStart = MetaIconTextButton(
		"Save changes",
		"ri-save-line",
		iconSize = 20,
		tier = MetaButtonTier.PRIMARY,
	).onClick { uiManager.showToast("Primary action") }
	private val toolGroup = MetaIconButtonGroup()
	private val activeTool = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT_MUTED)
	private val checkBox = MetaCheckBox(initialChecked = true)
	private val checkState = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT_MUTED)

	init {
		setDefaultSize(650f, 430f)
		contentTable.defaults().left().growX().padBottom(MetaSpacing.SM)

		contentTable.add(MetaLabel("ACTION HIERARCHY", MetaType.CAPTION, MetaColor.ACCENT)).row()
		val actions = MetaTable().apply {
			defaults().height(MetaControlSize.STANDARD.height).padRight(MetaSpacing.SM)
			add(navigationStart)
			add(MetaTextButton("Preview", tier = MetaButtonTier.SECONDARY)
				.onClick { uiManager.showToast("Secondary action") })
			add(MetaTextButton("Reset", tier = MetaButtonTier.TERTIARY)
				.onClick { uiManager.showToast("Tertiary action") })
		}
		contentTable.add(actions).row()
		contentTable.add(MetaLabel(
			"One primary action per decision area; secondary for alternatives; tertiary for low-emphasis utilities.",
			MetaType.CAPTION,
			MetaColor.TEXT_MUTED,
		)).row()

		val iconActions = MetaTable().apply {
			defaults().height(MetaControlSize.STANDARD.height).padRight(MetaSpacing.XS)
			add(MetaIconButton("ri-check-line", MetaButtonTier.PRIMARY).apply { tooltip("Primary icon action") })
				.width(MetaControlSize.STANDARD.iconTarget)
			add(MetaIconButton("ri-settings-3-line", MetaButtonTier.SECONDARY).apply { tooltip("Secondary icon action") })
				.width(MetaControlSize.STANDARD.iconTarget)
			add(MetaIconButton("ri-more-line", MetaButtonTier.TERTIARY).apply { tooltip("Tertiary icon action") })
				.width(MetaControlSize.STANDARD.iconTarget)
			add(MetaIconTextButton("Disabled", "ri-forbid-line", iconSize = 20).apply { isDisabled = true })
		}
		contentTable.add(iconActions).row()
		contentTable.add(MetaSeparator()).height(1f).row()

		val tools = MetaTable().apply {
			defaults().size(MetaControlSize.STANDARD.iconTarget).padRight(MetaSpacing.XS)
			add(toolGroup.add(tool("Brush", "ri-brush-line"), selected = true))
			add(toolGroup.add(tool("Pencil", "ri-pencil-line")))
			add(toolGroup.add(tool("Paint", "ri-paint-line")))
			add(MetaImageButton("ri-magic-line", size = 20).apply {
				tooltip("Lightweight image button")
				onClick { uiManager.showToast("Magic") }
			})
			add(activeTool).growX().padLeft(MetaSpacing.SM)
		}
		contentTable.add(tools).row()

		val fields = MetaTable().apply {
			defaults().left().padBottom(MetaSpacing.SM)
			add(fieldLabel("Text field")).width(110f)
			add(MetaTextField("", placeholder = "Editable text")).growX().height(MetaControlSize.STANDARD.height).row()
			add(fieldLabel("Input field")).width(110f)
			add(MetaInputField("", placeholder = "Validation-ready input")).growX().height(MetaControlSize.STANDARD.height).row()
			add(fieldLabel("Text area")).width(110f).top()
			add(MetaTextArea("Multiline editor\nResize the window around me.", prefRows = 2f)).growX().height(72f).row()
			add(fieldLabel("Checkbox")).width(110f)
			add(MetaTable().apply {
				add(checkBox).size(MetaControlSize.COMPACT.iconTarget).padRight(MetaSpacing.SM)
				add(checkState).left()
			}).left()
		}
		contentTable.add(fields).grow()
	}

	override fun onShown() {
		reactiveScope.bindText(activeTool) { "Active tool: ${toolGroup.selectedButtonValue()?.name ?: "None"}" }
		reactiveScope.bindText(checkState) { if (checkBox.checkedValue()) "Enabled" else "Disabled" }
	}

	private fun tool(label: String, icon: String) = MetaIconButton(icon).apply {
		name = label
		tooltip("$label tool")
	}
}

class SelectionPlaygroundWindow : MetaWindow("Selection & Progress", resizable = true, closeButton = true), Disposable {
	private val playgroundUiRenderer: UIRenderer by lazyInject()
	private val select = MetaSelectBox<String>().apply {
		setItems("Compact", "Comfortable", "Spacious", "Presentation")
		selected = "Comfortable"
	}
	private val spinnerModel = MetaIntSpinnerModel(4, 0, 12, 1)
	private val slider = SliderWithButtons(0f, 100f, 5f, false).apply { value = 35f }
	private val uiScaleSlider = SliderWithButtons(0.5f, 2f, 0.05f, false)
	private val uiScaleLabel = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT_MUTED)
	private val loading = MetaLoadingSpinner(32f, 3.5f)
	private val summary = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT_MUTED)
	private val pickedColor = signal(Color.valueOf("4F9DDEFF"))
	private val colorSwatch = MetaTable().apply {
		background = de.fatox.meta.ui.MetaSkin.skin().getDrawable(de.fatox.meta.ui.MetaSkin.COLOR_FILL)
	}
	private val colorLabel = MetaLabel("", MetaType.CAPTION, MetaColor.TEXT_MUTED)
	private val pickerListener = object : MetaColorPickerListener {
		override fun changed(newColor: Color) { pickedColor.value = newColor.cpy() }
		override fun canceled(oldColor: Color) { pickedColor.value = oldColor.cpy() }
	}

	init {
		setDefaultSize(430f, 310f)
		val grid = MetaTable().apply {
			defaults().left().padBottom(MetaSpacing.MD)
			add(fieldLabel("Select")).width(105f)
			add(select).growX().height(MetaControlSize.STANDARD.height).row()
			add(fieldLabel("Spinner")).width(105f)
			add(MetaSpinner(spinnerModel)).width(180f).left().row()
			add(fieldLabel("Loading")).width(105f)
			add(loading).left().row()
			add(fieldLabel("Color")).width(105f)
			add(MetaButtonContainer().apply {
				add(colorSwatch).size(42f, 22f).padRight(MetaSpacing.SM)
				add(colorLabel).left().growX()
				onClick { openColorPicker() }
			}).growX().height(MetaControlSize.STANDARD.height).row()
			add(fieldLabel("Slider")).width(105f)
			add(slider).growX().height(MetaControlSize.STANDARD.height).row()
			add(fieldLabel("UI scale")).width(105f)
			add(MetaTable().apply {
				add(uiScaleSlider).growX().height(MetaControlSize.STANDARD.height)
				add(uiScaleLabel).width(48f).right().padLeft(MetaSpacing.SM)
			}).growX().row()
			add()
			add(summary).growX()
		}
		contentTable.add(grid).grow()
	}

	override fun onShown() {
		uiScaleSlider.value = playgroundUiRenderer.uiScale.peek().coerceIn(0.5f, 2f)
		reactiveScope.subscribe(uiScaleSlider.committedValue) {
			playgroundUiRenderer.uiScale.value = uiScaleSlider.committedValue.peek()
			uiManager.resize(Gdx.graphics.width, Gdx.graphics.height)
		}
		reactiveScope.effect("playgroundUiScaleSlider") {
			uiScaleSlider.value = playgroundUiRenderer.uiScale().coerceIn(0.5f, 2f)
		}
		reactiveScope.bindText(uiScaleLabel) { "${(uiScaleSlider.valueValue() * 100f).roundToInt()}%" }
		reactiveScope.bindColor(colorSwatch) { pickedColor() }
		reactiveScope.bindText(colorLabel) { "#${pickedColor()}" }
		reactiveScope.bindText(summary) {
			"${select.selectedValue() ?: "—"} · count ${spinnerModel.valueValue().toInt()} · value ${slider.valueValue().toInt()}"
		}
	}

	private fun openColorPicker() {
		uiManager.showWindow<MetaColorPicker>().apply {
			selectedColor = pickedColor.peek()
			setListener(pickerListener)
			centerWindow()
			fadeIn()
		}
	}

	override fun dispose() {
		loading.dispose()
	}
}

class CollectionsPlaygroundWindow : MetaWindow("Lists & Scrolling", resizable = true, closeButton = true), Disposable {
	private val selected = MetaLabel("Select an entry", MetaType.CAPTION, MetaColor.TEXT_MUTED)
	private val menuTarget = signal<String?>(null)
	private val menuAnchor = Vector2()
	private val entryMenu = MetaMenu("").apply {
		addItem(MetaMenuItem("Open", "ri-folder-open-line").apply {
			onChange { uiManager.showToast("Open ${menuTarget.peek() ?: "entry"}") }
		})
		addItem(MetaMenuItem("Duplicate", "ri-file-copy-line").apply {
			onChange { uiManager.showToast("Duplicate ${menuTarget.peek() ?: "entry"}") }
		})
		addSeparator()
		addItem(MetaMenuItem("Delete", "ri-delete-bin-line").apply {
			onChange { uiManager.showToast("Delete ${menuTarget.peek() ?: "entry"}") }
		})
	}
	private val items = Array<String>().apply {
		add("Runtime components")
		add("Reactive bindings")
		add("Remix icon catalogue")
		add("Generated Meta skin")
		add("Window chrome and resize overlay")
		add("Tooltips across re-layout")
		add("Nested scroll focus")
		add("Menus and popup lifecycle")
	}
	private val list = MetaListView(object : MetaArrayAdapter<String, MetaButtonContainer>(items) {
		override fun createView(item: String): MetaButtonContainer = MetaButtonContainer().apply {
			left()
			add(MetaLabel(item, MetaType.CAPTION, MetaColor.TEXT)).left().growX().padLeft(MetaSpacing.SM)
			val menuButton = MetaImageButton("ri-menu-line", size = 18).apply menuButton@ {
				tooltip("Actions for $item")
				onClick {
					it.stop()
					showEntryMenu(item, this@menuButton)
				}
			}
			add(menuButton).size(MetaControlSize.COMPACT.iconTarget).pad(MetaSpacing.XXS)
		}

		override fun selectView(view: Actor) { (view as? MetaButtonContainer)?.isChecked = true }
		override fun deselectView(view: Actor) { (view as? MetaButtonContainer)?.isChecked = false }
	})
	private val nestedTooltipRow = MetaActionRow(
		title = "Nested tooltip regression",
		leading = MetaIcon("ri-file-line", 16, MetaColor.TEXT_MUTED),
		trailing = MetaTable().apply {
			add(MetaIcon("ri-team-line", 12, MetaColor.ACCENT)).size(12f).pad(2f)
			tooltip("Nested badge tooltip: Co-op")
		},
		interactiveTrailing = true,
		actions = { menu ->
			menu.addItem(MetaMenuItem("Regression action", "ri-check-line").apply {
				onChange { uiManager.showToast("Compact action works") }
			})
		},
	).apply {
		tooltip("Parent row tooltip: nested action row")
	}

	init {
		setDefaultSize(520f, 310f)

		contentTable.defaults().growX().left()
		contentTable.add(MetaLabel(
			"Hover the regression row, its Co-op badge, and its compact menu independently.",
			MetaType.CAPTION,
			MetaColor.TEXT_MUTED,
		)).padBottom(MetaSpacing.SM).row()
		contentTable.add(nestedTooltipRow).growX().padBottom(MetaSpacing.SM).row()
		contentTable.add(MetaSeparator()).height(1f).padBottom(MetaSpacing.SM).row()
		contentTable.add(list.scrollPane).grow().row()
		contentTable.add(selected).padTop(MetaSpacing.SM)
	}

	override fun onShown() {
		reactiveScope.bindText(selected) {
			list.selectedItemValue()?.let { "Selected: $it" } ?: "Select an entry"
		}
	}

	override fun onRemovedFromStage() {
		entryMenu.remove()
	}

	private fun showEntryMenu(item: String, button: Actor) {
		val stage = button.stage ?: return
		menuTarget.value = item
		entryMenu.pack()
		button.localToStageCoordinates(menuAnchor.set(button.width - entryMenu.width, 0f))
		entryMenu.showMenu(stage, menuAnchor.x, menuAnchor.y)
	}

	override fun dispose() {
		entryMenu.remove()
		list.dispose()
	}
}

private fun fieldLabel(text: String) = MetaLabel(text, MetaType.CAPTION, MetaColor.TEXT_MUTED)
