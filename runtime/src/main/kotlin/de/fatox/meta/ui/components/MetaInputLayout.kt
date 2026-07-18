package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.error.MetaErrorHandler
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.computed
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaControlSize
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType

/**
 * Label + input + feedback composition for form-like UI. The input remains the focusable control; this wrapper only
 * owns the surrounding text and spacing.
 */
class MetaInputLayout<T : Actor> @JvmOverloads constructor(
	labelText: String,
	val input: T,
	helperText: String = "",
) : Table(MetaSkin.skin()) {
	val labelTextValue: Signal<String> = signal(labelText)
	val helperTextValue: Signal<String> = signal(helperText)
	val errorTextValue: Signal<String> = signal("")
	val feedbackOverrideText: Signal<String?> = signal(null)
	val feedbackOverrideColor: Signal<Color?> = signal(null)
	val inputValidValue: ReactiveValue<Boolean> = when (input) {
		is MetaInputField -> input.inputValidValue
		is MetaTextArea -> input.inputValidValue
		else -> signal(true)
	}
	val feedbackTextValue: ReactiveValue<String> = computed {
		feedbackOverrideText.value
			?: if (!inputValidValue.value && errorTextValue.value.isNotEmpty()) errorTextValue.value else helperTextValue.value
	}
	val feedbackColorValue: ReactiveValue<Color> = computed {
		feedbackOverrideColor.value
			?: if (!inputValidValue.value && errorTextValue.value.isNotEmpty()) MetaColor.NEGATIVE else MetaColor.TEXT_MUTED
	}

	val label: MetaLabel = MetaLabel(labelTextValue.peek(), MetaType.CAPTION, Color.WHITE).apply {
		color.set(MetaColor.TEXT_MUTED)
		setAlignment(Align.left)
	}
	val feedback: MetaLabel = MetaLabel(feedbackTextValue.value, MetaType.CAPTION, Color.WHITE).apply {
		color.set(MetaColor.TEXT_MUTED)
		setAlignment(Align.left)
		isVisible = feedbackTextValue.value.isNotEmpty()
	}

	private var reactiveScope = ReactiveScope()
	private val feedbackCell: Cell<MetaLabel>

	init {
		defaults().left()
		add(label).growX().padBottom(MetaSpacing.XS)
		row()
		add(input).growX().minHeight(if (input is MetaTextArea) TEXT_AREA_MIN_HEIGHT else FIELD_MIN_HEIGHT)
		row()
		feedbackCell = add(feedback).growX()
		updateFeedbackVisibility(feedbackTextValue.value.isNotEmpty())
		installReactiveBindings()
	}

	fun setHelperText(text: String) {
		helperTextValue.value = text
		clearFeedbackOverride()
	}

	fun setFeedback(text: String, color: Color = MetaColor.TEXT_MUTED) {
		feedbackOverrideText.value = text
		feedbackOverrideColor.value = Color(color)
	}

	fun clearFeedbackOverride() {
		feedbackOverrideText.value = null
		feedbackOverrideColor.value = null
	}

	fun addValidator(validator: MetaInputValidator) {
		val wrapped = object : MetaInputValidator() {
			override fun validateInput(input: String, errors: MetaErrorHandler) {
				validator.validateInput(input, errors)
				errorTextValue.value = errors.labelText
			}
		}
		when (input) {
			is MetaInputField -> input.addValidator(wrapped)
			is MetaTextArea -> input.addValidator(wrapped)
			else -> throw IllegalStateException("MetaInputLayout validators require MetaInputField or MetaTextArea")
		}
	}

	override fun setStage(stage: Stage?) {
		val wasOnStage = this.stage != null
		super.setStage(stage)
		if (stage != null && reactiveScope.isDisposed) {
			reactiveScope = ReactiveScope()
			installReactiveBindings()
		} else if (wasOnStage && stage == null) {
			reactiveScope.dispose()
		}
	}

	private fun installReactiveBindings() {
		reactiveScope.effect("MetaInputLayout.labelText") {
			label.setText(labelTextValue.value)
		}
		reactiveScope.effect("MetaInputLayout.feedbackText") {
			feedback.setText(feedbackTextValue.value)
		}
		reactiveScope.effect("MetaInputLayout.feedbackColor") {
			feedback.color.set(feedbackColorValue.value)
		}
		reactiveScope.effect("MetaInputLayout.feedbackVisible") {
			updateFeedbackVisibility(feedbackTextValue.value.isNotEmpty())
		}
		reactiveScope.effect("MetaInputLayout.clearError") {
			if (inputValidValue.value) errorTextValue.value = ""
		}
	}

	private fun updateFeedbackVisibility(visible: Boolean) {
		feedback.isVisible = visible
		feedbackCell.padTop(if (visible) MetaSpacing.XS else MetaSpacing.NONE)
		feedbackCell.height(if (visible) Value.prefHeight else Value.Fixed(0f))
		invalidateHierarchy()
	}

	companion object {
		private val FIELD_MIN_HEIGHT = MetaControlSize.STANDARD.height
		private const val TEXT_AREA_MIN_HEIGHT = 112f

		@JvmStatic
		@JvmOverloads
		fun field(
			labelText: String,
			text: String = "",
			placeholder: String = "",
			helperText: String = "",
			size: Int = MetaType.BODY,
			fontProvider: FontProvider = inject(),
		): MetaInputLayout<MetaInputField> =
			MetaInputLayout(labelText, MetaInputField(text, size, fontProvider, placeholder), helperText)

		@JvmStatic
		@JvmOverloads
		fun area(
			labelText: String,
			text: String = "",
			placeholder: String = "",
			helperText: String = "",
			size: Int = MetaType.BODY,
			fontProvider: FontProvider = inject(),
			prefRows: Float = 4f,
		): MetaInputLayout<MetaTextArea> =
			MetaInputLayout(labelText, MetaTextArea(text, size, fontProvider, placeholder, prefRows), helperText)
	}
}
