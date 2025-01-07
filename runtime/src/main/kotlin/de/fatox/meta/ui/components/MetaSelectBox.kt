package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.widget.VisSelectBox
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.ui.UiControlHelper

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaSelectBox<T> : VisSelectBox<T>() {

	private val uiControlHelper: UiControlHelper = inject()
	private var wasHelperActive = false

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
