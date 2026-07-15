package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
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
) : MetaTable() {
	val textField = MetaTextField(metaModel.format(metaModel.value), fontSize)
	private val decrementButton = MetaImageButton("ri-subtract-line", 14)
	private val incrementButton = MetaImageButton("ri-add-line", 14)
	private var scope = ReactiveScope()
	private var syncing = false

	init {
		defaults().space(0f)
		add(decrementButton).size(STEP_BUTTON_SIZE)
		// Vanilla TextField advertises a 150px preferred width. Letting that leak through made this compact spinner
		// report a ~206px pref width even when callers assigned 100-112px, so Table laid its children outside the cell
		// and overlapped neighbouring controls. Override both metrics at the cell boundary: prefer the designed field
		// width, but allow it to shrink when the two fixed step buttons share a tighter form row.
		add(textField).growX().minWidth(0f).prefWidth(PREF_FIELD_WIDTH).height(FIELD_HEIGHT)
		add(incrementButton).size(STEP_BUTTON_SIZE)
		decrementButton.addListener(stepListener { metaModel.decrement() })
		incrementButton.addListener(stepListener { metaModel.increment() })
		textField.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				event.stop()
				if (syncing) return
				val parsed = metaModel.parse(textField.text)
				if (parsed != null) metaModel.value = parsed else syncFromModel()
			}
		})
		installBinding()
	}

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
			step()
		}
	}

	private companion object {
		const val STEP_BUTTON_SIZE = 28f
		const val FIELD_HEIGHT = 34f
		const val PREF_FIELD_WIDTH = 56f
	}
}
