package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.VisTextArea
import com.kotcrab.vis.ui.util.InputValidator
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

/**
 * Multi-line text input using VisUI text-area behavior with Meta's generated drawables and TTF font.
 */
open class MetaTextArea @JvmOverloads constructor(
	text: String = "",
	size: Int = MetaType.BODY,
	fontProvider: FontProvider = inject(),
	placeholder: String = "",
	prefRows: Float = DEFAULT_PREF_ROWS,
) : VisTextArea(text, MetaInputField.inputFieldStyle(size, fontProvider, MetaSkin.TEXT_AREA)), MetaFocusable {
	private val validators = Array<InputValidator>()
	private val validStyle = MetaInputField.inputFieldStyle(size, fontProvider, MetaSkin.TEXT_AREA)
	private val invalidStyle = MetaInputField.inputFieldStyle(size, fontProvider, MetaSkin.TEXT_FIELD_ERROR)
	private var metaInitialized = false

	val textValue: Signal<String> = signal(text)
	val inputValidValue: Signal<Boolean> = signal(true)

	var isValidationEnabled: Boolean = true
		set(value) {
			field = value
			validateInput()
		}

	init {
		metaInitialized = true
		style = validStyle
		setPrefRows(prefRows)
		if (placeholder.isNotEmpty()) setMessageText(placeholder)
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				syncTextValue()
				validateInput()
			}
		})
		validateInput()
	}

	override fun setMetaFocused(focused: Boolean) {
		if (focused) focusGained() else focusLost()
	}

	override fun setInputValid(valid: Boolean) {
		super.setInputValid(valid)
		if (!metaInitialized) return
		batch {
			inputValidValue.value = valid
			val nextStyle = if (valid) validStyle else invalidStyle
			if (style !== nextStyle) style = nextStyle
		}
	}

	override fun setText(str: String) {
		super.setText(str)
		if (metaInitialized) {
			syncTextValue()
			validateInput()
		}
	}

	fun addValidator(validator: InputValidator) {
		validators.add(validator)
		validateInput()
	}

	fun addValidator(validator: MetaInputValidator) {
		addValidator(InputValidator { input -> validator.validateInput(input) })
	}

	fun getValidators(): Array<InputValidator> = validators

	fun validateInput() {
		if (isValidationEnabled) {
			for (i in 0 until validators.size) {
				if (!validators[i].validateInput(text)) {
					setInputValid(false)
					return
				}
			}
		}
		setInputValid(true)
	}

	private fun syncTextValue() {
		val current = text
		if (textValue.peek() != current) textValue.value = current
	}

	private companion object {
		const val DEFAULT_PREF_ROWS = 4f
	}
}
