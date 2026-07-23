package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Disposable
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.extensions.tooltip
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaControlSize
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.bindText
import de.fatox.meta.ui.windows.MetaWindow
import kotlin.math.max
import kotlin.math.min

/** Compact Meta-owned HSV + RGBA color picker. */
open class MetaColorPicker @JvmOverloads constructor(
	title: String = "Pick a color",
	val isAllowAlphaEdit: Boolean = false,
	listener: MetaColorPickerListener? = null,
) : MetaWindow(title, resizable = false, closeButton = true), Disposable {
	private val original = Color.WHITE.cpy()
	private val working = Color.WHITE.cpy()
	private val parsedColor = Color()
	internal val state = MetaColorState()
	private val scope = ReactiveScope()
	private val mutableColorValue: Signal<Color> = signal(working.cpy()) { a, b -> a == b }
	val colorValue: ReactiveValue<Color> get() = mutableColorValue
	internal val colorField = MetaColorField(state, ::publishUserChange)
	internal val hueField = MetaHueField(state, ::publishUserChange)
	internal val preview = MetaColorSwatch()
	internal val brightnessSlider = MetaColorRampSlider(0f, 1f, 0.01f) { position, out ->
		MetaHsv.toColor(state.hue.peek(), state.saturation.peek(), position, 1f, out)
	}
	internal val alphaSlider = MetaColorRampSlider(0f, 1f, 0.01f, checkerboard = true) { position, out ->
		MetaHsv.toColor(state.hue.peek(), state.saturation.peek(), state.value.peek(), position, out)
	}
	private val inputModeValue: Signal<ColorInputMode> = signal(ColorInputMode.HEX)
	private val inputModeButton = MetaTextButton("HEX", MetaType.CAPTION, tier = de.fatox.meta.ui.MetaButtonTier.TERTIARY)
	private val colorInput = MetaInputField(
		placeholder = if (isAllowAlphaEdit) "#RRGGBBAA or rgba(255, 128, 0, 1)" else "#RRGGBB or rgb(255, 128, 0)",
	)
	private var syncingPresentation = false
	private var dismissing = false

	var metaListener: MetaColorPickerListener? = listener
	var selectedColor: Color
		get() = state.toColor(working).cpy()
		set(value) {
			original.set(value)
			state.setColor(value, isAllowAlphaEdit)
		}

	init {
		// A picker is a decision surface: while open, neither its parent UI nor a game canvas behind it is interactive.
		isModal = true
		// Width remains comfortably usable; height is resolved from the actual rows by MetaWindow on attachment.
		setDefaultSize(500f, 0f)

		val inputRow = MetaFlexBox(mainGap = MetaSpacing.XS, align = MetaFlexAlign.STRETCH).apply {
			addItem(inputModeButton.apply {
				tooltip("Switch between HEX and ${if (isAllowAlphaEdit) "RGBA" else "RGB"} display")
				onChange { toggleInputMode() }
			}, basisWidth = 76f, basisHeight = MetaControlSize.STANDARD.height, shrink = 0f)
			addItem(colorInput, basisHeight = MetaControlSize.STANDARD.height, grow = 1f, minWidth = 0f)
		}
		val actions = MetaFlexBox(
			mainGap = MetaSpacing.SM,
			justify = MetaFlexJustify.END,
			align = MetaFlexAlign.CENTER,
		).apply {
			addItem(MetaTextButton("Cancel").onChange { cancelPicker() }, shrink = 0f)
			addItem(MetaTextButton("Reset").onChange {
				state.setColor(original, isAllowAlphaEdit)
				metaListener?.reset(selectedColor, original.cpy())
			}, shrink = 0f)
			addItem(MetaTextButton("OK").onChange {
				try {
					metaListener?.finished(selectedColor)
				} finally {
					dismiss()
				}
			}, shrink = 0f)
		}
		val colorArea = MetaFlexBox(mainGap = MetaSpacing.SM, align = MetaFlexAlign.STRETCH).apply {
			addItem(colorField, basisHeight = COLOR_FIELD_HEIGHT, grow = 1f, minWidth = COLOR_FIELD_WIDTH)
			addItem(hueField, basisWidth = HUE_FIELD_WIDTH, basisHeight = COLOR_FIELD_HEIGHT, shrink = 0f)
		}
		val layout = MetaFlexBox(
			direction = MetaFlexDirection.COLUMN,
			mainGap = MetaSpacing.SM,
			align = MetaFlexAlign.STRETCH,
		).apply {
			addItem(colorArea, basisHeight = COLOR_FIELD_HEIGHT, minWidth = COLOR_FIELD_WIDTH + MetaSpacing.SM + HUE_FIELD_WIDTH)
			addItem(preview, basisHeight = PREVIEW_HEIGHT, grow = 0f)
			addItem(sliderRow("Brightness", brightnessSlider, state.value), basisHeight = SLIDER_ROW_HEIGHT, grow = 0f)
			if (isAllowAlphaEdit) {
				addItem(sliderRow("Alpha", alphaSlider, state.alpha), basisHeight = SLIDER_ROW_HEIGHT, grow = 0f)
			}
			addItem(inputRow, basisHeight = MetaControlSize.STANDARD.height, grow = 0f)
			addItem(actions, basisHeight = MetaControlSize.STANDARD.height, grow = 0f)
		}
		contentTable.add(layout).grow().pad(MetaSpacing.SM)

		colorInput.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				if (syncingPresentation) return
				val valid = MetaColorCodec.parse(colorInput.text, isAllowAlphaEdit, parsedColor)
				colorInput.setInputValid(valid)
				if (valid) {
					state.setColor(parsedColor, isAllowAlphaEdit)
					publishUserChange()
				}
			}
		})

		scope.subscribe(brightnessSlider.valueValue) {
			if (syncingPresentation) return@subscribe
			state.value.value = brightnessSlider.valueValue.peek()
			publishUserChange()
		}
		scope.subscribe(alphaSlider.valueValue) {
			if (syncingPresentation || !isAllowAlphaEdit) return@subscribe
			state.alpha.value = alphaSlider.valueValue.peek()
			publishUserChange()
		}
		scope.effect("MetaColorPicker.presentation") {
			state.hue()
			state.saturation()
			state.value()
			state.alpha()
			inputModeValue()
			syncPresentation()
		}
		state.setColor(Color.WHITE, isAllowAlphaEdit)
	}

	fun setListener(listener: MetaColorPickerListener?) {
		metaListener = listener
	}

	fun fadeIn(): MetaColorPicker {
		clearActions()
		color.a = 1f
		return this
	}

	/** Retained for API compatibility; dismissal is immediate so every exit path reliably updates UIManager. */
	fun fadeOut(): MetaColorPicker {
		dismiss()
		return this
	}

	override fun close() = cancelPicker()

	private fun syncPresentation() {
		state.toColor(working)
		syncingPresentation = true
		brightnessSlider.valueValue.value = state.value.peek()
		if (isAllowAlphaEdit) alphaSlider.valueValue.value = state.alpha.peek()
		colorInput.setText(formatInput())
		colorInput.setInputValid(true)
		inputModeButton.setText(
			if (inputModeValue.peek() == ColorInputMode.HEX) "HEX" else if (isAllowAlphaEdit) "RGBA" else "RGB"
		)
		preview.selectedColor = working
		mutableColorValue.value = working.cpy()
		syncingPresentation = false
	}

	private fun toggleInputMode() {
		inputModeValue.value = if (inputModeValue.peek() == ColorInputMode.HEX) ColorInputMode.RGB else ColorInputMode.HEX
	}

	private fun formatInput(): String = when (inputModeValue.peek()) {
		ColorInputMode.HEX -> MetaColorCodec.format(working, isAllowAlphaEdit)
		ColorInputMode.RGB -> MetaColorCodec.formatRgb(working, isAllowAlphaEdit)
	}

	private fun publishUserChange() = metaListener?.changed(selectedColor)

	private fun sliderRow(
		label: String,
		slider: Actor,
		value: Signal<Float>,
	): MetaFlexBox {
		val valueLabel = MetaLabel("", MetaType.CAPTION)
		scope.bindText(valueLabel) { "${(value() * 100f + 0.5f).toInt()}%" }
		return MetaFlexBox(mainGap = MetaSpacing.SM, align = MetaFlexAlign.CENTER).apply {
			addItem(MetaLabel(label, MetaType.CAPTION), basisWidth = SLIDER_LABEL_WIDTH, shrink = 0f)
			addItem(slider, grow = 1f, minWidth = 0f)
			addItem(valueLabel, basisWidth = SLIDER_VALUE_WIDTH, shrink = 0f)
		}
	}

	private fun cancelPicker() {
		state.setColor(original, isAllowAlphaEdit)
		try {
			metaListener?.canceled(original.cpy())
		} finally {
			dismiss()
		}
	}

	private fun dismiss() {
		if (dismissing) return
		dismissing = true
		clearActions()
		color.a = 1f
		isVisible = false
		try {
			uiManager.closeWindow(this)
		} finally {
			// Also covers direct-stage use where the picker was not opened through UIManager.
			remove()
			dismissing = false
		}
	}

	override fun dispose() {
		scope.dispose()
		colorField.dispose()
		hueField.dispose()
		brightnessSlider.dispose()
		alphaSlider.dispose()
	}

	private companion object {
		const val COLOR_FIELD_WIDTH = 360f
		const val COLOR_FIELD_HEIGHT = 210f
		const val HUE_FIELD_WIDTH = 28f
		const val PREVIEW_HEIGHT = 36f
		const val SLIDER_ROW_HEIGHT = 36f
		const val SLIDER_LABEL_WIDTH = 84f
		const val SLIDER_VALUE_WIDTH = 44f
	}
}

private enum class ColorInputMode { HEX, RGB }

/** Reactive HSV(A) source of truth shared by the pointer field, sliders and text representation. */
internal class MetaColorState {
	val hue: Signal<Float> = signal(0f)
	val saturation: Signal<Float> = signal(0f)
	val value: Signal<Float> = signal(1f)
	val alpha: Signal<Float> = signal(1f)
	private val converted = FloatArray(3)

	fun setColor(color: Color, allowAlpha: Boolean) {
		MetaHsv.fromColor(color, converted)
		batch {
			hue.value = converted[0]
			saturation.value = converted[1]
			value.value = converted[2]
			alpha.value = if (allowAlpha) color.a.coerceIn(0f, 1f) else 1f
		}
	}

	fun setSaturationValue(saturation: Float, value: Float) {
		batch {
			this.saturation.value = saturation.coerceIn(0f, 1f)
			this.value.value = value.coerceIn(0f, 1f)
		}
	}

	fun toColor(out: Color): Color =
		MetaHsv.toColor(hue.peek(), saturation.peek(), value.peek(), alpha.peek(), out)
}

/** Saturation/value field drawn directly as interpolated shape geometry. */
internal class MetaColorField(
	private val state: MetaColorState,
	private val changed: () -> Unit,
) : Widget(), Disposable {
	private val shapeRendererDelegate = lazy { ShapeRenderer() }
	private val shapeRenderer by shapeRendererDelegate
	private val origin = Vector2()
	private val oppositeCorner = Vector2()
	private val hueColor = Color.RED.cpy()
	private val blackDraw = Color.BLACK.cpy()
	private val whiteDraw = Color.WHITE.cpy()
	private var disposed = false
	private var dragPointer = -1

	init {
		cursorPointer()
		addListener(object : InputListener() {
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
				if (pointer != 0 || button != 0) return false
				dragPointer = pointer
				event.stop()
				updateFromPointer(x, y)
				return true
			}

			override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
				if (pointer != dragPointer) return
				event.stop()
				updateFromPointer(x, y)
			}

			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
				if (pointer != dragPointer) return
				dragPointer = -1
				event.stop()
			}
		})
	}

	override fun getPrefWidth(): Float = 360f
	override fun getPrefHeight(): Float = 210f

	override fun draw(batch: Batch, parentAlpha: Float) {
		if (disposed || color.a <= 0f) return
		val stage = stage ?: return
		localToStageCoordinates(origin.set(0f, 0f))
		localToStageCoordinates(oppositeCorner.set(width, height))
		val drawWidth = oppositeCorner.x - origin.x
		val drawHeight = oppositeCorner.y - origin.y
		val alpha = color.a * parentAlpha
		val hue = state.hue.peek()
		val saturation = state.saturation.peek()
		val value = state.value.peek()
		MetaHsv.toColor(hue, 1f, 1f, 1f, hueColor)
		hueColor.a = alpha
		blackDraw.a = alpha
		whiteDraw.a = alpha

		batch.end()
		shapeRenderer.projectionMatrix = stage.camera.combined
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
		shapeRenderer.rect(
			origin.x, origin.y, drawWidth, drawHeight,
			blackDraw, blackDraw, hueColor, whiteDraw,
		)
		shapeRenderer.color = Color.WHITE
		shapeRenderer.circle(origin.x + saturation * drawWidth, origin.y + value * drawHeight, MARKER_OUTER, 20)
		shapeRenderer.color = Color.BLACK
		shapeRenderer.circle(origin.x + saturation * drawWidth, origin.y + value * drawHeight, MARKER_INNER, 20)
		shapeRenderer.end()
		batch.begin()
	}

	private fun updateFromPointer(x: Float, y: Float) {
		state.setSaturationValue(x / width, y / height)
		changed()
	}

	override fun dispose() {
		if (disposed) return
		disposed = true
		if (shapeRendererDelegate.isInitialized()) shapeRenderer.dispose()
	}

	internal companion object {
		const val MARKER_OUTER = 6f
		const val MARKER_INNER = 4f
	}
}

/** Independently laid-out hue control whose visible bounds are also its complete pointer hit zone. */
internal class MetaHueField(
	private val state: MetaColorState,
	private val changed: () -> Unit,
) : Widget(), Disposable {
	private val shapeRendererDelegate = lazy { ShapeRenderer() }
	private val shapeRenderer by shapeRendererDelegate
	private val origin = Vector2()
	private val oppositeCorner = Vector2()
	private val hueColors = Array(7) { index -> Color().also { MetaHsv.toColor(index * 60f, 1f, 1f, 1f, it) } }
	private var disposed = false
	private var dragPointer = -1

	init {
		cursorPointer()
		addListener(object : InputListener() {
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
				if (pointer != 0 || button != 0) return false
				dragPointer = pointer
				event.stop()
				updateFromPointer(y)
				return true
			}

			override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
				if (pointer != dragPointer) return
				event.stop()
				updateFromPointer(y)
			}

			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
				if (pointer != dragPointer) return
				dragPointer = -1
				event.stop()
			}
		})
	}

	override fun getPrefWidth(): Float = 28f
	override fun getPrefHeight(): Float = 210f

	override fun draw(batch: Batch, parentAlpha: Float) {
		if (disposed || color.a <= 0f || width <= 0f || height <= 0f) return
		val stage = stage ?: return
		localToStageCoordinates(origin.set(0f, 0f))
		localToStageCoordinates(oppositeCorner.set(width, height))
		val drawWidth = oppositeCorner.x - origin.x
		val drawHeight = oppositeCorner.y - origin.y
		val alpha = color.a * parentAlpha
		val segmentHeight = drawHeight / 6f

		batch.end()
		shapeRenderer.projectionMatrix = stage.camera.combined
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
		for (index in 0 until 6) {
			hueColors[index].a = alpha
			hueColors[index + 1].a = alpha
			shapeRenderer.rect(
				origin.x,
				origin.y + index * segmentHeight,
				drawWidth,
				segmentHeight,
				hueColors[index],
				hueColors[index],
				hueColors[index + 1],
				hueColors[index + 1],
			)
		}
		val markerY = markerY(origin.y, origin.y + drawHeight, state.hue.peek())
		shapeRenderer.color = Color.WHITE
		shapeRenderer.rect(origin.x - 2f, markerY - 2f, drawWidth + 4f, 4f)
		shapeRenderer.color = Color.BLACK
		shapeRenderer.rect(origin.x - 1f, markerY - 1f, drawWidth + 2f, 2f)
		shapeRenderer.end()
		batch.begin()
	}

	private fun updateFromPointer(y: Float) {
		state.hue.value = (y / height).coerceIn(0f, 1f) * 360f
		changed()
	}

	override fun dispose() {
		if (disposed) return
		disposed = true
		if (shapeRendererDelegate.isInitialized()) shapeRenderer.dispose()
	}

	internal companion object {
		fun markerY(bottom: Float, top: Float, hue: Float): Float =
			bottom + (top - bottom) * (hue / 360f).coerceIn(0f, 1f)
	}
}

/** Allocation-free HSV conversion shared by the picker geometry and channel synchronization. */
internal object MetaHsv {
	fun toColor(hue: Float, saturation: Float, value: Float, alpha: Float, out: Color): Color {
		val h = ((hue % 360f) + 360f) % 360f
		val s = saturation.coerceIn(0f, 1f)
		val v = value.coerceIn(0f, 1f)
		val chroma = v * s
		val section = h / 60f
		val x = chroma * (1f - kotlin.math.abs(section % 2f - 1f))
		val m = v - chroma
		when (section.toInt().coerceIn(0, 5)) {
			0 -> out.set(chroma + m, x + m, m, alpha)
			1 -> out.set(x + m, chroma + m, m, alpha)
			2 -> out.set(m, chroma + m, x + m, alpha)
			3 -> out.set(m, x + m, chroma + m, alpha)
			4 -> out.set(x + m, m, chroma + m, alpha)
			else -> out.set(chroma + m, m, x + m, alpha)
		}
		return out
	}

	fun fromColor(color: Color, out: FloatArray): FloatArray {
		val maxChannel = max(color.r, max(color.g, color.b))
		val minChannel = min(color.r, min(color.g, color.b))
		val delta = maxChannel - minChannel
		val hue = when {
			delta == 0f -> 0f
			maxChannel == color.r -> 60f * (((color.g - color.b) / delta) % 6f)
			maxChannel == color.g -> 60f * ((color.b - color.r) / delta + 2f)
			else -> 60f * ((color.r - color.g) / delta + 4f)
		}
		out[0] = if (hue < 0f) hue + 360f else hue
		out[1] = if (maxChannel == 0f) 0f else delta / maxChannel
		out[2] = maxChannel
		return out
	}
}

/** Parser/formatter for the picker's combined precision field. */
internal object MetaColorCodec {
	private val componentSeparator = Regex("[,;/\\s]+")
	private val cssFunction = Regex("(?i)rgba?\\s*\\((.*)\\)")
	private const val HEX = "0123456789ABCDEF"

	fun parse(text: String, allowAlpha: Boolean, out: Color): Boolean {
		val input = text.trim()
		if (input.isEmpty()) return false
		val hexadecimal = if (input[0] == '#') input.substring(1) else input
		if ((input[0] == '#' || hexadecimal.length == 6 || hexadecimal.length == 8) &&
			hexadecimal.all { it.digitToIntOrNull(16) != null }) {
			return parseHex(hexadecimal, allowAlpha, out)
		}

		val componentInput = cssFunction.matchEntire(input)?.groupValues?.get(1) ?: input
		val components = componentInput.split(componentSeparator).filter { it.isNotEmpty() }
		if (components.size != 3 && components.size != 4) return false
		val red = parseRgbChannel(components[0]) ?: return false
		val green = parseRgbChannel(components[1]) ?: return false
		val blue = parseRgbChannel(components[2]) ?: return false
		val alpha = if (components.size == 4) parseAlpha(components[3]) ?: return false else 1f
		out.set(red, green, blue, if (allowAlpha) alpha else 1f)
		return true
	}

	fun format(color: Color, includeAlpha: Boolean): String = buildString(if (includeAlpha) 9 else 7) {
		append('#')
		appendChannel(color.r)
		appendChannel(color.g)
		appendChannel(color.b)
		if (includeAlpha) appendChannel(color.a)
	}

	fun formatRgb(color: Color, includeAlpha: Boolean): String = buildString(32) {
		append(if (includeAlpha) "rgba(" else "rgb(")
		append(byteChannel(color.r)).append(", ")
		append(byteChannel(color.g)).append(", ")
		append(byteChannel(color.b))
		if (includeAlpha) {
			val alpha = (color.a * 1000f + 0.5f).toInt().coerceIn(0, 1000)
			append(", ").append(alpha / 1000f)
		}
		append(')')
	}

	private fun parseRgbChannel(component: String): Float? {
		if (component.endsWith('%')) {
			val percent = component.dropLast(1).toFloatOrNull() ?: return null
			if (percent !in 0f..100f) return null
			return percent / 100f
		}
		val value = component.toFloatOrNull() ?: return null
		if (value !in 0f..255f) return null
		return value / 255f
	}

	private fun parseAlpha(component: String): Float? {
		if (component.endsWith('%')) {
			val percent = component.dropLast(1).toFloatOrNull() ?: return null
			if (percent !in 0f..100f) return null
			return percent / 100f
		}
		val value = component.toFloatOrNull() ?: return null
		return when {
			value in 0f..1f -> value
			value in 0f..255f -> value / 255f
			else -> null
		}
	}

	private fun byteChannel(value: Float): Int = (value * 255f + 0.5f).toInt().coerceIn(0, 255)

	private fun parseHex(hex: String, allowAlpha: Boolean, out: Color): Boolean {
		if (hex.length != 3 && hex.length != 4 && hex.length != 6 && hex.length != 8) return false
		val short = hex.length <= 4
		val red = component(hex, 0, short)
		val green = component(hex, 1, short)
		val blue = component(hex, 2, short)
		val hasAlpha = hex.length == 4 || hex.length == 8
		val alpha = if (hasAlpha) component(hex, 3, short) else 255
		out.set(red / 255f, green / 255f, blue / 255f, if (allowAlpha) alpha / 255f else 1f)
		return true
	}

	private fun component(hex: String, index: Int, short: Boolean): Int {
		if (short) {
			val nibble = hex[index].digitToInt(16)
			return nibble * 17
		}
		val offset = index * 2
		return hex.substring(offset, offset + 2).toInt(16)
	}

	private fun StringBuilder.appendChannel(value: Float) {
		val channel = (value * 255f + 0.5f).toInt().coerceIn(0, 255)
		append(HEX[channel ushr 4])
		append(HEX[channel and 15])
	}
}

interface MetaColorPickerListener {
	fun changed(newColor: Color) {}
	fun finished(newColor: Color) = changed(newColor)
	fun canceled(oldColor: Color) {}
	fun reset(newColor: Color, oldColor: Color) {}
}
