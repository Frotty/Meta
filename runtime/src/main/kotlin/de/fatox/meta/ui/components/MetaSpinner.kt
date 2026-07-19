package de.fatox.meta.ui.components

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.MetaControlSize
import java.math.BigDecimal
import java.math.RoundingMode

interface MetaSpinnerModel {
	var value: Number
	var min: Number
	var max: Number
	val valueValue: Signal<Number>
	fun increment()
	fun decrement()
	fun parse(text: String): Number?
	fun format(value: Number): String
}

class MetaIntSpinnerModel(
	initial: Int,
	min: Int,
	max: Int,
	private val step: Int = 1,
) : MetaSpinnerModel {
	private var minimum = min
	private var maximum = max
	override val valueValue: Signal<Number> = signal(initial.coerceIn(minimum, maximum))

	override var value: Number
		get() = valueValue.peek()
		set(value) { valueValue.value = value.toInt().coerceIn(minimum, maximum) }
	override var min: Number
		get() = minimum
		set(value) { minimum = value.toInt(); this.value = this.value }
	override var max: Number
		get() = maximum
		set(value) { maximum = value.toInt(); this.value = this.value }
	override fun increment() { value = value.toInt() + step }
	override fun decrement() { value = value.toInt() - step }
	override fun parse(text: String): Number? = text.trim().toIntOrNull()
	override fun format(value: Number): String = value.toInt().toString()
}

class MetaFloatSpinnerModel(
	initial: Float,
	min: Float,
	max: Float,
	private val step: Float,
	precision: Int = 2,
) : MetaSpinnerModel {
	private var minimum = min
	private var maximum = max
	override val valueValue: Signal<Number> = signal(initial.coerceIn(minimum, maximum)) { a, b -> a.toFloat() == b.toFloat() }
	var precision: Int = precision.coerceAtLeast(0)

	override var value: Number
		get() = valueValue.peek()
		set(value) { valueValue.value = value.toFloat().coerceIn(minimum, maximum) }
	override var min: Number
		get() = minimum
		set(value) { minimum = value.toFloat(); this.value = this.value }
	override var max: Number
		get() = maximum
		set(value) { maximum = value.toFloat(); this.value = this.value }
	override fun increment() { value = value.toFloat() + step }
	override fun decrement() { value = value.toFloat() - step }
	override fun parse(text: String): Number? = text.trim().toFloatOrNull()
	override fun format(value: Number): String = BigDecimal.valueOf(value.toDouble())
		.setScale(precision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

/** Scene2d-native numeric field with compact step controls and reactive model ownership. */
open class MetaSpinner @JvmOverloads constructor(
	val metaModel: MetaSpinnerModel,
	fontSize: Int = MetaType.BODY,
) : MetaFlexBox(
	direction = MetaFlexDirection.ROW,
	mainGap = MetaSpacing.NONE,
	align = MetaFlexAlign.STRETCH,
) {
	val textField = MetaTextField(metaModel.format(metaModel.value), fontSize, styleName = de.fatox.meta.ui.MetaSkin.SPINNER_TEXT_FIELD)
	// Squishing one segment of the composite would detach it from the field; the spinner reacts as one control.
	private val decrementButton = MetaIconButton("ri-subtract-line", de.fatox.meta.ui.MetaSkin.SPINNER_DECREMENT, 14).apply { pressSquish = false }
	private val incrementButton = MetaIconButton("ri-add-line", de.fatox.meta.ui.MetaSkin.SPINNER_INCREMENT, 14).apply { pressSquish = false }
	private var scope = ReactiveScope()
	private var syncing = false

	init {
		addItem(decrementButton, basisWidth = STEP_BUTTON_WIDTH, basisHeight = FIELD_HEIGHT, shrink = 0f,
			minWidth = STEP_BUTTON_WIDTH, minHeight = FIELD_HEIGHT)
		// Vanilla TextField advertises a 150px preferred width. Give flex a useful basis for common multi-digit values,
		// while its explicit zero minimum lets the field shrink in tighter form rows without overflowing neighbours.
		addItem(textField, basisWidth = PREF_FIELD_WIDTH, basisHeight = FIELD_HEIGHT, grow = 1f,
			minWidth = 0f, minHeight = FIELD_HEIGHT)
		addItem(incrementButton, basisWidth = STEP_BUTTON_WIDTH, basisHeight = FIELD_HEIGHT, shrink = 0f,
			minWidth = STEP_BUTTON_WIDTH, minHeight = FIELD_HEIGHT)
		decrementButton.addListener(stepListener { metaModel.decrement() })
		incrementButton.addListener(stepListener { metaModel.increment() })
		// TextField ChangeEvents describe edits, not committed spinner values. Keep them from bubbling through this
		// control; MetaSpinner emits its own ChangeEvent when the model actually changes.
		textField.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) = event.stop()
		})
		textField.addListener(object : FocusListener() {
			override fun keyboardFocusChanged(event: FocusEvent, actor: Actor, focused: Boolean) {
				if (!focused) commitText()
			}
		})
		textField.addListener(object : InputListener() {
			override fun keyDown(event: InputEvent, keycode: Int): Boolean {
				when (keycode) {
					Keys.ENTER, Keys.NUMPAD_ENTER -> commitText()
					Keys.ESCAPE -> cancelTextEdit()
					else -> return false
				}
				event.stop()
				return true
			}
		})
		installBinding()
	}

	/**
	 * Parses and commits the current edit. Bounds are applied by the model only at this commit boundary, so an
	 * in-progress value may temporarily be empty, invalid, or outside the configured range. Invalid input restores
	 * the last committed value; valid out-of-range input is clamped and displayed in its canonical format.
	 */
	fun commitText() {
		if (syncing) return
		val parsed = metaModel.parse(textField.text)
		if (parsed != null) metaModel.value = parsed
		syncFromModel()
	}

	/** Discards the current edit and restores the model's last committed value. */
	fun cancelTextEdit() = syncFromModel()

	override fun setStage(stage: Stage?) {
		val wasOnStage = this.stage != null
		super.setStage(stage)
		if (stage != null && scope.isDisposed) {
			scope = ReactiveScope()
			installBinding()
		} else if (wasOnStage && stage == null) scope.dispose()
	}

	private fun installBinding() {
		syncFromModel()
		scope.subscribe(metaModel.valueValue) {
			syncFromModel()
			fire(ChangeListener.ChangeEvent())
		}
	}

	private fun syncFromModel() {
		val formatted = metaModel.format(metaModel.value)
		if (textField.text == formatted) return
		syncing = true
		textField.setText(formatted)
		syncing = false
	}

	private fun stepListener(step: () -> Unit) = object : ChangeListener() {
		override fun changed(event: ChangeEvent, actor: Actor) {
			event.stop()
			commitText()
			step()
		}
	}

	private companion object {
		val STEP_BUTTON_WIDTH = MetaControlSize.STANDARD.iconTarget - MetaSpacing.XS
		val FIELD_HEIGHT = MetaControlSize.STANDARD.height
		const val PREF_FIELD_WIDTH = 72f
	}
}
