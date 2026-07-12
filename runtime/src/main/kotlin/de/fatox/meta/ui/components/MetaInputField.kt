package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.util.InputValidator
import com.kotcrab.vis.ui.widget.VisTextField.VisTextFieldStyle
import com.kotcrab.vis.ui.widget.VisValidatableTextField
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.FontGenerationTracker
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

/**
 * VisUI-compatible validatable input field using Meta's generated field drawables and TTF font.
 */
open class MetaInputField @JvmOverloads constructor(
	text: String = "",
	size: Int = MetaType.BODY,
	fontProvider: FontProvider = inject(),
	placeholder: String = "",
	styleName: String = MetaSkin.TEXT_FIELD,
) : VisValidatableTextField(text, inputFieldStyle(size, fontProvider, styleName)), MetaFocusable, FontRefreshable {
	private val validStyle = inputFieldStyle(size, fontProvider, styleName)
	private val invalidStyle = inputFieldStyle(size, fontProvider, MetaSkin.TEXT_FIELD_ERROR)
	private var metaInitialized = false
	private val fontSize = size
	private val fontProvider = fontProvider
	private val fontTracker = FontGenerationTracker()

	val textValue: Signal<String> = signal(text)
	val inputValidValue: Signal<Boolean> = signal(true)

	init {
		metaInitialized = true
		style = validStyle
		if (placeholder.isNotEmpty()) setMessageText(placeholder)
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				syncTextValue()
			}
		})
	}

	constructor(firstValidator: InputValidator, vararg validators: InputValidator) : this() {
		addValidator(firstValidator)
		for (i in validators.indices) addValidator(validators[i])
	}

	constructor(restoreLastValid: Boolean, firstValidator: InputValidator, vararg validators: InputValidator) : this() {
		addValidator(firstValidator)
		for (i in validators.indices) addValidator(validators[i])
		setRestoreLastValid(restoreLastValid)
	}

	fun addValidator(validator: MetaInputValidator) {
		addValidator(InputValidator { input -> validator.validateInput(input) })
	}

	override fun setMetaFocused(focused: Boolean) {
		if (focused) focusGained() else focusLost()
	}

	/** Re-fetches the font into both (cloned) valid/invalid styles after a UI-scale change. */
	override fun refreshFont() {
		fontTracker.markFresh()
		val font = fontProvider.getFont(fontSize, FontType.REGULAR)
		validStyle.font = font
		validStyle.messageFont = font
		invalidStyle.font = font
		invalidStyle.messageFont = font
		// Re-apply so the field re-derives its text metrics from the new font.
		setStyle(style)
		invalidateHierarchy()
	}

	/** Self-heal on (re)attach: a field that was detached during a UI-scale change holds a disposed font. */
	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) fontTracker.refreshIfStale(this)
	}

	override fun setText(str: String) {
		super.setText(str)
		if (metaInitialized) syncTextValue()
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

	private fun syncTextValue() {
		val current = text
		if (textValue.peek() != current) textValue.value = current
	}

	companion object {
		fun inputFieldStyle(
			size: Int = MetaType.BODY,
			fontProvider: FontProvider = inject(),
			styleName: String = MetaSkin.TEXT_FIELD,
		): VisTextFieldStyle {
			val skin = MetaSkin.skin()
			val baseStyle = if (skin.has(styleName, VisTextFieldStyle::class.java)) {
				skin.get(styleName, VisTextFieldStyle::class.java)
			} else {
				skin.get(VisTextFieldStyle::class.java)
			}
			val font = fontProvider.getFont(size, FontType.REGULAR)
			return VisTextFieldStyle(baseStyle).apply {
				this.font = font
				messageFont = font
			}
		}
	}
}
