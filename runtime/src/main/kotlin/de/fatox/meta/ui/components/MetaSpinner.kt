package de.fatox.meta.ui.components

import com.kotcrab.vis.ui.VisUI
import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle
import com.kotcrab.vis.ui.widget.VisValidatableTextField
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel
import com.kotcrab.vis.ui.widget.spinner.Spinner
import com.kotcrab.vis.ui.widget.spinner.SpinnerModel
import com.kotcrab.vis.ui.widget.spinner.SimpleFloatSpinnerModel
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

interface MetaSpinnerModel {
	fun getSpinnerModel(): SpinnerModel
	var value: Number
	var min: Number
	var max: Number
}

/**
 * Integer-typed spinner model with VisUI-backed behavior, surfaced through Meta's typed API for oxrox-level usage.
 */
class MetaIntSpinnerModel(
	initial: Int,
	min: Int,
	max: Int,
	step: Int = 1,
) : MetaSpinnerModel {
	private val delegate = IntSpinnerModel(initial, min, max, step)

	override fun getSpinnerModel(): SpinnerModel = delegate
	override var value: Number
		get() = delegate.value
		set(value) {
			delegate.value = value.toInt()
		}

	override var min: Number
		get() = delegate.min
		set(value) {
			delegate.min = value.toInt()
		}

	override var max: Number
		get() = delegate.max
		set(value) {
			delegate.max = value.toInt()
		}
}

/**
 * Float-typed spinner model with VisUI-backed behavior, surfaced through Meta's typed API for oxrox-level usage.
 */
class MetaFloatSpinnerModel(
	initial: Float,
	min: Float,
	max: Float,
	step: Float,
	precision: Int = 2,
) : MetaSpinnerModel {
	private val delegate = SimpleFloatSpinnerModel(initial, min, max, step, precision)

	override fun getSpinnerModel(): SpinnerModel = delegate
	override var value: Number
		get() = delegate.value
		set(value) {
			delegate.value = value.toFloat()
		}

	override var min: Number
		get() = delegate.min
		set(value) {
			delegate.min = value.toFloat()
		}

	override var max: Number
		get() = delegate.max
		set(value) {
			delegate.max = value.toFloat()
		}

	var precision: Int
		get() = delegate.precision
		set(value) = run { delegate.precision = value }
}

private class MetaSpinnerModelAdapter(
	private val spinnerModel: SpinnerModel,
) : MetaSpinnerModel {
	private fun unsupported(): Nothing {
		error("Unsupported SpinnerModel type: ${spinnerModel.javaClass.name}")
	}

	override fun getSpinnerModel(): SpinnerModel = spinnerModel
	override var value: Number
		get() = when (spinnerModel) {
			is IntSpinnerModel -> spinnerModel.value
			is SimpleFloatSpinnerModel -> spinnerModel.value
			else -> unsupported()
		}
		set(value) {
			when (spinnerModel) {
				is IntSpinnerModel -> spinnerModel.value = value.toInt()
				is SimpleFloatSpinnerModel -> spinnerModel.value = value.toFloat()
				else -> unsupported()
			}
		}

	override var min: Number
		get() = when (spinnerModel) {
			is IntSpinnerModel -> spinnerModel.min
			is SimpleFloatSpinnerModel -> spinnerModel.min
			else -> unsupported()
		}
		set(value) {
			when (spinnerModel) {
				is IntSpinnerModel -> spinnerModel.min = value.toInt()
				is SimpleFloatSpinnerModel -> spinnerModel.min = value.toFloat()
				else -> unsupported()
			}
		}

	override var max: Number
		get() = when (spinnerModel) {
			is IntSpinnerModel -> spinnerModel.max
			is SimpleFloatSpinnerModel -> spinnerModel.max
			else -> unsupported()
		}
		set(value) {
			when (spinnerModel) {
				is IntSpinnerModel -> spinnerModel.max = value.toInt()
				is SimpleFloatSpinnerModel -> spinnerModel.max = value.toFloat()
				else -> unsupported()
			}
		}
}

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaSpinner @JvmOverloads constructor(
	spinnerModel: MetaSpinnerModel,
	private val fontSize: Int = MetaType.BODY,
) : Spinner("", spinnerModel.getSpinnerModel()), FontRefreshable {
	val metaModel: MetaSpinnerModel = spinnerModel

	@Deprecated(
		message = "Pass a MetaSpinnerModel (e.g. MetaIntSpinnerModel/MetaFloatSpinnerModel) instead.",
		level = DeprecationLevel.WARNING,
	)
	constructor(spinnerModel: SpinnerModel, fontSize: Int = MetaType.BODY) : this(
		MetaSpinnerModelAdapter(spinnerModel),
		fontSize
	)

	private val fontProvider: FontProvider = inject()

	init {
		applyFieldFont()
	}

	/** Re-fetches the text field font after a UI-scale change. Rare event, so re-cloning the style is fine. */
	override fun refreshFont() {
		applyFieldFont()
		invalidateHierarchy()
	}

	private fun applyFieldFont() {
		cells.find { it.actor is VisValidatableTextField }?.let {
			val field = it.actor as VisValidatableTextField
			val skin = VisUI.getSkin()
			// Clone before mutating - never write into the shared skin style (see MetaTextField for the pattern).
			val baseStyle = if (skin.has(MetaSkin.TEXT_FIELD, VisTextFieldStyle::class.java)) {
				skin.get(MetaSkin.TEXT_FIELD, VisTextFieldStyle::class.java)
			} else {
				field.style as VisTextFieldStyle
			}
			field.style = VisTextFieldStyle(baseStyle).apply {
				font = fontProvider.getFont(fontSize, FontType.REGULAR)
			}
		}
	}
}
