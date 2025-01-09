package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.widget.VisSelectBox
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.ui.UiControlHelper

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaSelectBox<T>(fontSize: Int = 22) : VisSelectBox<T>() {

	private val uiControlHelper: UiControlHelper = inject()
	private var wasHelperActive = false
	private val fontProvider: FontProvider = inject()

	init {
		style.font = fontProvider.getFont(fontSize, FontType.REGULAR)
		list.style.font = fontProvider.getFont(fontSize, FontType.REGULAR)

	}

	override fun showScrollPane() {
		super.showScrollPane()


		if (uiControlHelper.activated) {
			wasHelperActive = true
			uiControlHelper.activated = false
		}
	}

	override fun onHide(scrollPane: Actor?) {
		super.onHide(scrollPane)

		if (wasHelperActive) {
			uiControlHelper.activated = true
			wasHelperActive = false
		}
	}


}
