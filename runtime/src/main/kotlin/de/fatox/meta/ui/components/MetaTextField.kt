package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.FontGenerationTracker
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaType

/**
 * Text input using Meta's generated field drawables and TTF font. Prefer this over raw Scene2D/VisUI fields.
 */
open class MetaTextField @JvmOverloads constructor(
	text: String = "",
	size: Int = MetaType.BODY,
	fontProvider: FontProvider = inject(),
) : TextField(text, textFieldStyle(size, fontProvider)), MetaFocusable, FontRefreshable {
	private val focusStyle = MetaTextFieldFocusStyle(this, style, MetaSkin::focusedTextFieldStyle)
	private var metaInitialized = false
	private val fontSize = size
	private val fontProvider = fontProvider
	private val fontTracker = FontGenerationTracker()

	val textValue: Signal<String> = signal(text)
	val disabledValue: Signal<Boolean> = signal(isDisabled)

	var isFocusBorderEnabled: Boolean = true
		set(value) {
			field = value
			focusStyle.setFocusEnabled(value)
		}

	init {
		metaInitialized = true
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				syncTextValue()
			}
		})
	}

	protected fun installMetaTextFieldStyle(style: TextFieldStyle) {
		focusStyle.install(style, isFocusBorderEnabled)
	}

	/** Re-fetches the font into the (cloned) normal/focused styles after a UI-scale change. */
	override fun refreshFont() {
		fontTracker.markFresh()
		focusStyle.refreshFont(fontProvider.getFont(fontSize, FontType.REGULAR))
		invalidateHierarchy()
	}

	/** Self-heal on (re)attach: a field that was detached during a UI-scale change holds a disposed font. */
	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) fontTracker.refreshIfStale(this)
	}

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	override fun setText(str: String) {
		super.setText(str)
		if (metaInitialized) syncTextValue()
	}

	override fun setDisabled(disabled: Boolean) {
		super.setDisabled(disabled)
		disabledValue.value = disabled
	}

	private fun syncTextValue() {
		val current = text
		if (textValue.peek() != current) textValue.value = current
	}

	companion object {
		fun textFieldStyle(
			size: Int = MetaType.BODY,
			fontProvider: FontProvider = inject(),
			styleName: String = MetaSkin.TEXT_FIELD,
		): TextFieldStyle {
			val skin = MetaSkin.skin()
			val baseStyle = if (skin.has(styleName, TextFieldStyle::class.java)) {
				skin.get(styleName, TextFieldStyle::class.java)
			} else {
				skin.get(TextFieldStyle::class.java)
			}
			val font = fontProvider.getFont(size, FontType.REGULAR)
			return TextFieldStyle(baseStyle).apply {
				this.font = font
				messageFont = font
			}
		}
	}
}
