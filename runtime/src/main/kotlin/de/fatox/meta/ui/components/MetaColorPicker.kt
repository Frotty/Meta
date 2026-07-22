package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Disposable
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.extensions.tooltip
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaControlSize
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType
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
	val colorValue: Signal<Color> = signal(working.cpy()) { a, b -> a == b }
	private val colorField = MetaColorField { hue, saturation, value -> updateFromField(hue, saturation, value) }
	private val preview = MetaColorSwatch()
	private val inputModeValue: Signal<ColorInputMode> = signal(ColorInputMode.HEX)
	private val inputModeButton = MetaTextButton("HEX", MetaType.CAPTION, tier = de.fatox.meta.ui.MetaButtonTier.TERTIARY)
	private val colorInput = MetaInputField(
		placeholder = if (isAllowAlphaEdit) "#RRGGBBAA or rgba(255, 128, 0, 1)" else "#RRGGBB or rgb(255, 128, 0)",
	)
	private var syncing = false
	private var dismissing = false
	@Suppress("unused")
	private val colorBinding = colorValue.subscribe {
		if (syncing) return@subscribe
		working.set(colorValue.peek())
		syncModels()
	}
	@Suppress("unused")
	private val inputModeBinding = inputModeValue.subscribe { syncInputMode() }

	var metaListener: MetaColorPickerListener? = listener
	var selectedColor: Color
		get() = working.cpy()
		set(value) {
			original.set(value)
			working.set(value)
			syncModels()
		}

	init {
		setDefaultSize(440f, 420f)
		contentTable.defaults().growX().padBottom(MetaSpacing.XS)
		contentTable.add(colorField).height(210f).padLeft(MetaSpacing.SM).padRight(MetaSpacing.SM).row()
		contentTable.add(preview).height(36f).padTop(MetaSpacing.XS).padBottom(MetaSpacing.SM)
			.padLeft(MetaSpacing.SM).padRight(MetaSpacing.SM).row()
		contentTable.add(MetaFlexBox(mainGap = MetaSpacing.XS, align = MetaFlexAlign.STRETCH).apply {
			addItem(inputModeButton.apply {
				tooltip("Switch between HEX and ${if (isAllowAlphaEdit) "RGBA" else "RGB"} display")
				onChange { toggleInputMode() }
			}, basisWidth = 76f, basisHeight = MetaControlSize.STANDARD.height, shrink = 0f)
			addItem(colorInput, basisHeight = MetaControlSize.STANDARD.height, grow = 1f, minWidth = 0f)
		}).row()
		colorInput.addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: com.badlogic.gdx.scenes.scene2d.Actor) {
				if (syncing) return
				val valid = MetaColorCodec.parse(colorInput.text, isAllowAlphaEdit, parsedColor)
				colorInput.setInputValid(valid)
				if (valid) {
					working.set(parsedColor)
					colorField.setSelectedColor(working)
					publish(notifyListener = true)
				}
			}
		})
		contentTable.add(MetaFlexBox(
			mainGap = MetaSpacing.SM,
			justify = MetaFlexJustify.END,
			align = MetaFlexAlign.CENTER,
		).apply {
			addItem(MetaTextButton("Cancel").apply {
				// Dismiss on the press itself. This deliberately bypasses Button's checked/change machinery so Cancel
				// remains an unconditional escape hatch even when focus or another listener consumes touch-up.
				addListener(object : InputListener() {
					override fun touchDown(
						event: InputEvent,
						x: Float,
						y: Float,
						pointer: Int,
						button: Int,
					): Boolean {
						if (pointer != 0 || button != 0) return false
						event.stop()
						this@MetaColorPicker.cancelPicker()
						return true
					}
				})
			}, shrink = 0f)
			addItem(MetaTextButton("Reset").onChange {
				working.set(original)
				syncModels()
				metaListener?.reset(selectedColor, original.cpy())
			}, shrink = 0f)
			addItem(MetaTextButton("OK").onChange {
				try {
					metaListener?.finished(selectedColor)
				} finally {
					dismiss()
				}
			}, shrink = 0f)
		}).right().padTop(MetaSpacing.SM)
		syncModels()
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

	private fun syncModels() {
		syncing = true
		colorField.setSelectedColor(working)
		colorInput.setText(formatInput())
		colorInput.setInputValid(true)
		publish(notifyListener = false)
		syncing = false
	}

	private fun updateFromField(hue: Float, saturation: Float, value: Float) {
		MetaHsv.toColor(hue, saturation, value, working.a, working)
		syncing = true
		colorInput.setText(formatInput())
		colorInput.setInputValid(true)
		syncing = false
		publish(notifyListener = true)
	}

	private fun toggleInputMode() {
		inputModeValue.value = if (inputModeValue.peek() == ColorInputMode.HEX) ColorInputMode.RGB else ColorInputMode.HEX
	}

	private fun syncInputMode() {
		val inputMode = inputModeValue.peek()
		inputModeButton.setText(if (inputMode == ColorInputMode.HEX) "HEX" else if (isAllowAlphaEdit) "RGBA" else "RGB")
		syncing = true
		colorInput.setText(formatInput())
		colorInput.setInputValid(true)
		syncing = false
	}

	private fun formatInput(): String = when (inputModeValue.peek()) {
		ColorInputMode.HEX -> MetaColorCodec.format(working, isAllowAlphaEdit)
		ColorInputMode.RGB -> MetaColorCodec.formatRgb(working, isAllowAlphaEdit)
	}

	private fun publish(notifyListener: Boolean) {
		preview.selectedColor = working
		colorValue.value = working.cpy()
		if (notifyListener) metaListener?.changed(working.cpy())
	}

	private fun cancelPicker() {
		working.set(original)
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
		colorField.dispose()
	}

}

private enum class ColorInputMode { HEX, RGB }

/** Saturation/value square with a vertical hue strip, drawn directly as interpolated shape geometry. */
internal class MetaColorField(
	private val changed: (hue: Float, saturation: Float, value: Float) -> Unit,
) : Widget(), Disposable {
	private val shapeRendererDelegate = lazy { ShapeRenderer() }
	private val shapeRenderer by shapeRendererDelegate
	private val origin = Vector2()
	private val hueColor = Color.RED.cpy()
	private val converted = FloatArray(3)
	private val hueColors = Array(7) { index -> Color().also { MetaHsv.toColor(index * 60f, 1f, 1f, 1f, it) } }
	private var hue = 0f
	private var saturation = 0f
	private var value = 1f
	private var disposed = false

	init {
		cursorPointer()
		addListener(object : InputListener() {
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
				if (pointer != 0 || button != 0) return false
				event.stop()
				updateFromPointer(x, y)
				return true
			}

			override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
				if (pointer == 0) {
					event.stop()
					updateFromPointer(x, y)
				}
			}
		})
	}

	override fun getPrefWidth(): Float = 360f
	override fun getPrefHeight(): Float = 210f

	fun setSelectedColor(color: Color) {
		MetaHsv.fromColor(color, converted)
		hue = converted[0]
		saturation = converted[1]
		value = converted[2]
		MetaHsv.toColor(hue, 1f, 1f, 1f, hueColor)
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		if (disposed || color.a <= 0f) return
		val stage = stage ?: return
		localToStageCoordinates(origin.set(0f, 0f))
		val squareWidth = squareWidth()
		val alpha = color.a * parentAlpha

		batch.end()
		shapeRenderer.projectionMatrix = stage.camera.combined
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
		shapeRenderer.rect(
			origin.x, origin.y, squareWidth, height,
			BLACK.withAlpha(alpha), BLACK.withAlpha(alpha), hueColor.withAlpha(alpha), WHITE.withAlpha(alpha),
		)
		val segmentHeight = height / 6f
		val hueX = origin.x + squareWidth + HUE_GAP
		for (i in 0 until 6) {
			shapeRenderer.rect(
				hueX, origin.y + i * segmentHeight, HUE_WIDTH, segmentHeight + 0.5f,
				hueColors[i], hueColors[i], hueColors[i + 1], hueColors[i + 1],
			)
		}
		shapeRenderer.color = Color.WHITE
		shapeRenderer.circle(origin.x + saturation * squareWidth, origin.y + value * height, MARKER_OUTER, 20)
		shapeRenderer.color = Color.BLACK
		shapeRenderer.circle(origin.x + saturation * squareWidth, origin.y + value * height, MARKER_INNER, 20)
		val hueMarkerY = origin.y + hue / 360f * height
		shapeRenderer.color = Color.WHITE
		shapeRenderer.rect(hueX - 2f, hueMarkerY - 2f, HUE_WIDTH + 4f, 4f)
		shapeRenderer.color = Color.BLACK
		shapeRenderer.rect(hueX - 1f, hueMarkerY - 1f, HUE_WIDTH + 2f, 2f)
		shapeRenderer.end()
		batch.begin()
	}

	private fun updateFromPointer(x: Float, y: Float) {
		val squareWidth = squareWidth()
		if (regionAt(x, width) == ColorFieldRegion.SATURATION_VALUE) {
			saturation = (x / squareWidth).coerceIn(0f, 1f)
			value = (y / height).coerceIn(0f, 1f)
		} else if (regionAt(x, width) == ColorFieldRegion.HUE) {
			hue = (y / height).coerceIn(0f, 1f) * 360f
			MetaHsv.toColor(hue, 1f, 1f, 1f, hueColor)
		} else return
		changed(hue, saturation, value)
	}

	private fun squareWidth(): Float = (width - HUE_GAP - HUE_WIDTH).coerceAtLeast(1f)

	override fun dispose() {
		if (disposed) return
		disposed = true
		if (shapeRendererDelegate.isInitialized()) shapeRenderer.dispose()
	}

	private fun Color.withAlpha(alpha: Float): Color {
		a = alpha
		return this
	}

	internal companion object {
		const val HUE_WIDTH = 28f
		const val HUE_GAP = 8f
		const val MARKER_OUTER = 6f
		const val MARKER_INNER = 4f
		val WHITE = Color.WHITE.cpy()
		val BLACK = Color.BLACK.cpy()

		fun regionAt(x: Float, totalWidth: Float): ColorFieldRegion {
			if (x < 0f || x > totalWidth) return ColorFieldRegion.OUTSIDE
			val squareWidth = (totalWidth - HUE_GAP - HUE_WIDTH).coerceAtLeast(1f)
			return if (x <= squareWidth) {
				ColorFieldRegion.SATURATION_VALUE
			} else {
				// Give the visual gap to the hue slider as deliberate hit slop.
				ColorFieldRegion.HUE
			}
		}
	}
}

internal enum class ColorFieldRegion {
	SATURATION_VALUE,
	HUE,
	OUTSIDE
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
