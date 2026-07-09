package de.fatox.meta.playground

import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.components.MetaCheckBox
import de.fatox.meta.ui.components.MetaIconTextButton
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaSelectBox
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextField
import de.fatox.meta.ui.windows.MetaWindow

class PlaygroundSampleWindow : MetaWindow("Sample Window", true, true) {
	init {
		setDefaultSize(360f, 220f)
		contentTable.defaults().pad(MetaSpacing.SM).left()
		contentTable.add(MetaLabel("Resizable MetaWindow", MetaType.BODY, MetaColor.TEXT)).growX().row()
		contentTable.add(MetaLabel("Header, close button, separator, resize grip.", MetaType.CAPTION, MetaColor.TEXT_MUTED)).growX().row()

		val row = MetaTable()
		row.defaults().padRight(MetaSpacing.SM)
		row.add(MetaCheckBox(initialChecked = true)).size(28f)
		row.add(MetaLabel("Font icon checkbox", MetaType.CAPTION, MetaColor.TEXT_MUTED))
		contentTable.add(row).growX().row()

		contentTable.add(MetaTextField("Editable field", MetaType.BODY)).growX().height(34f).row()

		val select = MetaSelectBox<String>(MetaType.BODY)
		select.setItems("Compact", "Comfortable", "Dense")
		contentTable.add(select).growX().height(34f).row()

		contentTable.add(MetaIconTextButton("Close", "ri-close-line", size = MetaType.CAPTION, iconSize = 20)
			.onClick { close() })
			.align(Align.right)
	}
}
