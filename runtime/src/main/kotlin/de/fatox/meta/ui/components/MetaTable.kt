package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing

/** Meta-facing table wrapper. Prefer this over raw VisTable/Table in UI code. */
class MetaTable @JvmOverloads constructor(defaultSpacing: Boolean = false) : Table(MetaSkin.skin()) {
	init {
		if (defaultSpacing) defaults().pad(MetaSpacing.SM)
	}
}
