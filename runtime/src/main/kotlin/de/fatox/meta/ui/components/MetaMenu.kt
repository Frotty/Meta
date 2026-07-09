package de.fatox.meta.ui.components

import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import com.kotcrab.vis.ui.widget.Menu
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.MenuItem

class MetaMenuBar : MenuBar() {
	init {
		table.background = MetaSkin.skin().getDrawable("meta.panel")
		table.pad(MetaSpacing.XS, MetaSpacing.SM, MetaSpacing.XS, MetaSpacing.SM)
		table.defaults().padRight(MetaSpacing.XS)
	}
}

class MetaMenu(title: String) : Menu(title)

class MetaMenuItem : MenuItem {
	constructor(text: String) : super(text)
	constructor(text: String, icon: String, size: Int = 18) : super(text, MetaIconDrawable(icon, size))
}
