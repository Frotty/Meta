package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

/** Scene2d-native validatable input field using Meta's generated field states and TTF font. */
open class MetaInputField @JvmOverloads constructor(
	text: String = "",
	size: Int = MetaType.BODY,
	fontProvider: FontProvider = inject(),
	placeholder: String = "",
	styleName: String = MetaSkin.TEXT_FIELD,
) : MetaTextField(text, size, fontProvider, placeholder) {
	private val inputFontSize = size
	private val inputFontProvider = fontProvider
	private val validators = Array<MetaInputValidator>()
	private val validStyle = textFieldStyle(size, fontProvider, styleName)
	private val invalidStyle = textFieldStyle(size, fontProvider, MetaSkin.TEXT_FIELD_ERROR)
	private var restoreLastValid = false
	private var lastValidText = text
	private var validating = false
	private var inputInitialized = false

	val inputValidValue: Signal<Boolean> = signal(true)
	val isInputValid: Boolean get() = inputValidValue.peek()

	init {
		inputInitialized = true
		installMetaTextFieldStyle(validStyle)
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				validateInput()
			}
		})
	}

	override fun setText(str: String) {
		super.setText(str)
		if (inputInitialized && !validating) validateInput()
	}

	override fun refreshFont() {
		val font = inputFontProvider.getFont(inputFontSize, FontType.REGULAR)
		validStyle.font = font
		validStyle.messageFont = font
		invalidStyle.font = font
		invalidStyle.messageFont = font
		super.refreshFont()
		installMetaTextFieldStyle(if (isInputValid) validStyle else invalidStyle)
	}

	constructor(firstValidator: MetaInputValidator, vararg validators: MetaInputValidator) : this() {
		addValidator(firstValidator)
		for (i in validators.indices) addValidator(validators[i])
	}

	constructor(restoreLastValid: Boolean, firstValidator: MetaInputValidator, vararg validators: MetaInputValidator) : this() {
		setRestoreLastValid(restoreLastValid)
		addValidator(firstValidator)
		for (i in validators.indices) addValidator(validators[i])
	}

	fun addValidator(validator: MetaInputValidator) {
		validators.add(validator)
		validateInput()
	}

	fun getValidators(): Array<MetaInputValidator> = validators

	fun setRestoreLastValid(restore: Boolean) {
		restoreLastValid = restore
		if (isInputValid) lastValidText = text
	}

	fun setInputValid(valid: Boolean) {
		batch {
			inputValidValue.value = valid
			installMetaTextFieldStyle(if (valid) validStyle else invalidStyle)
		}
	}

	fun validateInput(): Boolean {
		if (validating) return isInputValid
		var valid = true
		for (i in 0 until validators.size) valid = validators[i].validateInput(text) && valid
		if (valid) {
			lastValidText = text
		} else if (restoreLastValid) {
			validating = true
			setText(lastValidText)
			validating = false
			valid = true
		}
		setInputValid(valid)
		return valid
	}
}
