package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.windows.MetaWindow

/** Meta-owned RGBA color picker. */
open class MetaColorPicker @JvmOverloads constructor(
	title: String = "Pick a color",
	val isAllowAlphaEdit: Boolean = false,
	listener: MetaColorPickerListener? = null,
) : MetaWindow(title, resizable = false, closeButton = true) {
	private val original = Color.WHITE.cpy()
	private val working = Color.WHITE.cpy()
	val colorValue: Signal<Color> = signal(working.cpy()) { a, b -> a == b }
	private val red = MetaIntSpinnerModel(255, 0, 255)
	private val green = MetaIntSpinnerModel(255, 0, 255)
	private val blue = MetaIntSpinnerModel(255, 0, 255)
	private val alpha = MetaIntSpinnerModel(255, 0, 255)
	private val preview = MetaTable().apply { background = MetaSkin.skin().getDrawable("meta.panel") }
	private var syncing = false

	var metaListener: MetaColorPickerListener? = listener
	var selectedColor: Color
		get() = working.cpy()
		set(value) {
			original.set(value)
			working.set(value)
			syncModels()
		}

	init {
		setDefaultSize(360f, if (isAllowAlphaEdit) 320f else 280f)
		contentTable.defaults().growX().padBottom(MetaSpacing.XS)
		contentTable.add(preview).height(56f).row()
		addChannel("Red", red)
		addChannel("Green", green)
		addChannel("Blue", blue)
		if (isAllowAlphaEdit) addChannel("Alpha", alpha)
		contentTable.add(MetaTable().apply {
			add(MetaTextButton("Cancel").onClick { cancel() }).padRight(MetaSpacing.SM)
			add(MetaTextButton("Reset").onClick {
				selectedColor = original
				metaListener?.reset(selectedColor, original.cpy())
			})
				.padRight(MetaSpacing.SM)
			add(MetaTextButton("OK").onClick { metaListener?.finished(selectedColor); fadeOut() })
		}).right().padTop(MetaSpacing.SM)
		syncModels()
	}

	fun setListener(listener: MetaColorPickerListener?) {
		metaListener = listener
	}

	fun fadeIn(): MetaColorPicker {
		clearActions()
		this.color.a = 0f
		addAction(Actions.fadeIn(0.15f))
		return this
	}

	fun fadeOut(): MetaColorPicker {
		clearActions()
		addAction(Actions.sequence(Actions.fadeOut(0.12f), Actions.removeActor()))
		return this
	}

	override fun close() = cancel()

	private fun addChannel(label: String, model: MetaIntSpinnerModel) {
		val spinner = MetaSpinner(model)
		spinner.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: com.badlogic.gdx.scenes.scene2d.Actor) {
				if (!syncing) updateColor()
			}
		})
		contentTable.add(MetaTable().apply {
			add(MetaLabel(label, MetaType.CAPTION)).width(52f)
			add(spinner).growX()
		}).row()
	}

	private fun syncModels() {
		syncing = true
		red.value = (working.r * 255f).toInt()
		green.value = (working.g * 255f).toInt()
		blue.value = (working.b * 255f).toInt()
		alpha.value = (working.a * 255f).toInt()
		preview.color.set(working)
		colorValue.value = working.cpy()
		syncing = false
	}

	private fun updateColor() {
		working.set(red.value.toInt() / 255f, green.value.toInt() / 255f, blue.value.toInt() / 255f,
			if (isAllowAlphaEdit) alpha.value.toInt() / 255f else 1f)
		preview.color.set(working)
		colorValue.value = working.cpy()
		metaListener?.changed(working.cpy())
	}

	private fun cancel() {
		working.set(original)
		metaListener?.canceled(original.cpy())
		fadeOut()
	}
}

interface MetaColorPickerListener {
	fun changed(newColor: Color) {}
	fun finished(newColor: Color) = changed(newColor)
	fun canceled(oldColor: Color) {}
	fun reset(newColor: Color, oldColor: Color) {}
}
