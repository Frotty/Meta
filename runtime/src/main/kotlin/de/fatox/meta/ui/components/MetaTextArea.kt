package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextArea
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.cursorText
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.drawPixelSnapped
import de.fatox.meta.api.graphics.physicalPixelsPerUnit
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.FontGenerationTracker
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

/** Scene2d-native multiline input with Meta validation, TTF styling, and reactive state. */
open class MetaTextArea @JvmOverloads constructor(
	text: String = "",
	private val fontSize: Int = MetaType.BODY,
	private val fontProvider: FontProvider = inject(),
	placeholder: String = "",
	prefRows: Float = DEFAULT_PREF_ROWS,
) : TextArea(text, MetaTextField.textFieldStyle(fontSize, fontProvider, MetaSkin.TEXT_AREA)), MetaFocusable, FontRefreshable {
	private val validators = Array<MetaInputValidator>()
	private val validStyle = MetaTextField.textFieldStyle(fontSize, fontProvider, MetaSkin.TEXT_AREA)
	private val invalidStyle = MetaTextField.textFieldStyle(fontSize, fontProvider, MetaSkin.TEXT_FIELD_ERROR)
	private val fontTracker = FontGenerationTracker()
	private var metaInitialized = false

	val textValue: Signal<String> = signal(text)
	val inputValidValue: Signal<Boolean> = signal(true)
	val isInputValid: Boolean get() = inputValidValue.peek()

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
		cursorText()
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				syncTextValue()
				validateInput()
			}
		})
	}

	override fun setMetaFocused(focused: Boolean) {
		stage?.keyboardFocus = if (focused) this else stage?.keyboardFocus?.takeUnless { it === this }
	}

	override fun refreshFont() {
		fontTracker.markFresh()
		val font = fontProvider.getFont(fontSize, FontType.REGULAR)
		validStyle.font = font
		validStyle.messageFont = font
		invalidStyle.font = font
		invalidStyle.messageFont = font
		setStyle(style)
		invalidateHierarchy()
	}

	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) fontTracker.refreshIfStale(this)
	}

	override fun setText(str: String) {
		super.setText(str)
		if (metaInitialized) {
			syncTextValue()
			validateInput()
		}
	}

	fun setInputValid(valid: Boolean) {
		batch {
			inputValidValue.value = valid
			val next = if (valid) validStyle else invalidStyle
			if (style !== next) style = next
		}
	}

	fun addValidator(validator: MetaInputValidator) {
		validators.add(validator)
		validateInput()
	}

	fun getValidators(): Array<MetaInputValidator> = validators

	fun validateInput(): Boolean {
		var valid = true
		if (isValidationEnabled) for (i in 0 until validators.size) valid = validators[i].validateInput(text) && valid
		setInputValid(valid)
		return valid
	}

	private fun syncTextValue() {
		if (textValue.peek() != text) textValue.value = text
	}

	// Vanilla TextArea (via TextField) positions its internal BitmapFontCache straight from this actor's own x/y
	// with no pixel-grid awareness, unlike MetaLabel - snap it so multi-line input stays crisp at every UI scale.
	override fun draw(batch: Batch, parentAlpha: Float) {
		drawPixelSnapped(batch, parentAlpha, style.font.physicalPixelsPerUnit()) { b, a -> super.draw(b, a) }
	}

	private companion object {
		const val DEFAULT_PREF_ROWS = 4f
	}
}
