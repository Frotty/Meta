package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.kotcrab.vis.ui.widget.Menu
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.MenuItem

class MetaMenuBar : MenuBar()

class MetaMenu(title: String) : Menu(title)

class MetaMenuItem : MenuItem {
	constructor(text: String) : super(text)
	constructor(text: String, image: Image?) : super(text, image)
}
