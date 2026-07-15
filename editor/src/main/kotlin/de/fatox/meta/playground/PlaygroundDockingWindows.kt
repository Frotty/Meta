package de.fatox.meta.playground

import de.fatox.meta.api.extensions.tooltip
import de.fatox.meta.ui.MetaButtonTier
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.components.MetaButtonContainer
import de.fatox.meta.ui.components.MetaCheckBox
import de.fatox.meta.ui.components.MetaIcon
import de.fatox.meta.ui.components.MetaIconButton
import de.fatox.meta.ui.components.MetaIconButtonGroup
import de.fatox.meta.ui.components.MetaInputLayout
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaScrollPane
import de.fatox.meta.ui.components.MetaSeparator
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.windows.MetaWindow

/** Compact panels used to exercise Meta's edge docking without game-specific UI. */
class DockToolsPlaygroundWindow : MetaWindow("Tools", resizable = true, closeButton = true) {
	private val tools = MetaIconButtonGroup()

	init {
		setDefaultSize(250f, 190f)
		contentTable.defaults().left().growX()
		contentTable.add(MetaLabel("ACTIVE TOOL", MetaType.CAPTION, MetaColor.TEXT_MUTED))
			.padBottom(MetaSpacing.SM).row()
		contentTable.add(MetaTable().apply {
			defaults().size(38f).padRight(MetaSpacing.XS).padBottom(MetaSpacing.XS)
			add(tools.add(tool("Select", "ri-cursor-line"), selected = true))
			add(tools.add(tool("Brush", "ri-brush-line")))
			add(tools.add(tool("Erase", "ri-eraser-line"))).row()
			add(tools.add(tool("Move", "ri-drag-move-2-line")))
			add(tools.add(tool("Shape", "ri-shape-line")))
			add(tools.add(tool("Grid", "ri-layout-grid-line")))
		}).left()
	}

	private fun tool(label: String, icon: String) = MetaIconButton(icon).apply {
		name = label
		tooltip(label)
	}
}

class DockLayersPlaygroundWindow : MetaWindow("Layers", resizable = true, closeButton = true) {
	init {
		setDefaultSize(250f, 420f)
		val layers = MetaTable().apply {
			defaults().growX().padBottom(MetaSpacing.XS)
			layer("Lighting", "ri-sun-line")
			layer("Gameplay", "ri-gamepad-line")
			layer("Foreground", "ri-stack-line")
			layer("Collision", "ri-shape-line")
			layer("Background", "ri-image-line")
			layer("Guides", "ri-layout-grid-line")
			layer("Annotations", "ri-information-line")
		}
		contentTable.add(MetaScrollPane(layers)).grow()
	}

	private fun MetaTable.layer(label: String, icon: String) {
		add(MetaButtonContainer().apply {
			left()
			add(MetaIcon(icon, 18, MetaColor.TEXT_MUTED.cpy())).size(28f)
			add(MetaLabel(label, MetaType.CAPTION, MetaColor.TEXT)).left().growX()
			add(MetaIconButton("ri-eye-line", MetaButtonTier.TERTIARY).apply {
				tooltip("Toggle $label visibility")
			}).size(30f)
		}).height(36f).row()
	}
}

class DockInspectorPlaygroundWindow : MetaWindow("Inspector", resizable = true, closeButton = true) {
	init {
		setDefaultSize(310f, 285f)
		contentTable.defaults().growX().left().padBottom(MetaSpacing.SM)
		contentTable.add(MetaLabel("SELECTION", MetaType.CAPTION, MetaColor.ACCENT)).row()
		contentTable.add(MetaInputLayout.field("Name", "Portal frame")).row()
		contentTable.add(MetaInputLayout.field("Position", "128, 64")).row()
		contentTable.add(MetaTable().apply {
			add(MetaCheckBox(initialChecked = true)).size(28f).padRight(MetaSpacing.SM)
			add(MetaLabel("Visible in editor", MetaType.CAPTION, MetaColor.TEXT)).left().growX()
		}).row()
		contentTable.add(MetaSeparator()).height(1f).padTop(MetaSpacing.XS).row()
		contentTable.add(MetaLabel("Resize this divider, then drag the panel away from the edge.",
			MetaType.CAPTION, MetaColor.TEXT_MUTED)).row()
	}
}

class DockActivityPlaygroundWindow : MetaWindow("Activity", resizable = true, closeButton = true) {
	init {
		setDefaultSize(310f, 360f)
		val activity = MetaTable().apply {
			defaults().growX().left().padBottom(MetaSpacing.SM)
			entry("Workspace opened", "ri-folder-open-line")
			entry("Dock layout restored", "ri-layout-column-line")
			entry("Selection changed", "ri-cursor-line")
			entry("Inspector refreshed", "ri-settings-3-line")
			entry("Layer visibility changed", "ri-eye-line")
			entry("Autosave completed", "ri-save-line")
			entry("History checkpoint created", "ri-history-line")
		}
		contentTable.add(MetaScrollPane(activity)).grow()
	}

	private fun MetaTable.entry(label: String, icon: String) {
		add(MetaTable().apply {
			add(MetaIcon(icon, 18, MetaColor.TEXT_MUTED.cpy())).size(28f).top()
			add(MetaTable().apply {
				left()
				add(MetaLabel(label, MetaType.CAPTION, MetaColor.TEXT)).left().row()
				add(MetaLabel("Just now", MetaType.CAPTION, MetaColor.TEXT_MUTED)).left()
			}).left().growX()
		}).row()
	}
}
