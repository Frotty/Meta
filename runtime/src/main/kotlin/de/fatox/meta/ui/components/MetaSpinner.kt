package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisValidatableTextField
import com.kotcrab.vis.ui.widget.spinner.Spinner
import com.kotcrab.vis.ui.widget.spinner.SpinnerModel
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.ui.UiControlHelper

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaSpinner(spinnerModel: SpinnerModel, fontSize: Int = 22) : Spinner("", spinnerModel) {

	private val uiControlHelper: UiControlHelper = inject()
	private var wasHelperActive = false
	private val fontProvider: FontProvider = inject()

	init {
		cells.find { it.actor is VisValidatableTextField }?.let {
			(it.actor as VisValidatableTextField).style.font = fontProvider.getFont(fontSize, FontType.REGULAR)
		}
	}



}
