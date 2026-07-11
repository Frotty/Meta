package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaTextButton @JvmOverloads constructor(
	text: String = "",
	size: Int = MetaType.BODY,
	type: FontType = FontType.REGULAR
) :
	Button(MetaSkin.skin().get(MetaSkin.BUTTON, Button.ButtonStyle::class.java)), MetaFocusable {

	private var labelCell: Cell<MetaLabel>
	private val label: MetaLabel = MetaLabel(text, size, Color.WHITE, type) { setAlignment(Align.center) }
	private val focusStyle = MetaButtonFocusStyle(this, style, MetaSkin::focusedButtonStyle)
	private val disabledTint = MetaDisabledTint(this)

	val textValue: Signal<String> = signal(text)
	val checkedValue: Signal<Boolean> = signal(isChecked)
	val disabledValue: Signal<Boolean> = signal(isDisabled)

	fun setText(text: String = "") {
		label.setText(text)
		textValue.value = text
	}

	val text: CharSequence = label.text


	init {
		pad(MetaSpacing.SM, MetaSpacing.MD, MetaSpacing.SM, MetaSpacing.MD)
		labelCell = add(label)
		centerText()
		cursorPointer()
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				checkedValue.value = isChecked
			}
		})
	}

	protected fun installMetaStyle(style: Button.ButtonStyle) {
		focusStyle.install(style)
	}

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	override fun setDisabled(isDisabled: Boolean) {
		super.setDisabled(isDisabled)
		batch {
			disabledValue.value = isDisabled
			disabledTint.apply(isDisabled)
		}
	}

	override fun setChecked(isChecked: Boolean) {
		super.setChecked(isChecked)
		checkedValue.value = this.isChecked
	}

	fun centerText() {
		labelCell.center().grow()
	}

	fun leftText() {
		labelCell.left().grow()
	}

	final override fun pad(top: Float, left: Float, bottom: Float, right: Float): Table {
		return super.pad(top, left, bottom, right)
	}

	final override fun <T : Actor?> add(actor: T): Cell<T> {
		return super.add(actor)
	}
}

@Suppress("FunctionName")
@OptIn(ExperimentalContracts::class)
inline fun MetaTextButton(
	text: String = "",
	size: Int = MetaType.BODY,
	type: FontType = FontType.REGULAR,
	init: MetaTextButton.() -> Unit
): MetaTextButton {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MetaTextButton(text, size, type).apply(init)
}
