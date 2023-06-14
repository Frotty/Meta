package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFontCache
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.StringBuilder
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

/**
 * A text label, with optional word wrapping.
 *
 *
 * The preferred size of the label is determined by the actual text bounds, unless [word wrap][.setWrap] is enabled.
 *
 * @author Nathan Sweet
 */
class MetaLabel @JvmOverloads constructor(
	text: CharSequence,
	size: Int,
	color: Color? = Color.WHITE,
	type: FontType = FontType.REGULAR
) : Widget() {
	val glyphLayout: GlyphLayout = GlyphLayout()
	private val prefSize = Vector2()
	val text: StringBuilder = StringBuilder()
	private var font: BitmapFont
	private var size = size.toFloat()

	/** Allows subclasses to access the cache in [.draw].  */
	private var bitmapFontCache: BitmapFontCache
	var labelAlign: Int = Align.left
		private set
	var lineAlign: Int = Align.left
		private set
	private var wrap = false
	private var lastPrefHeight = 0f
	private var prefSizeInvalid = true
	private var fontScaleX = 1f
	private var fontScaleY = 1f
	private var ellipsis: String? = null

	private val metaFontProvider: FontProvider by lazyInject()

	private val fontColor: Color?
	private val type: FontType = type

	fun setText(newText: CharSequence = "") {
		if (newText is StringBuilder) {
			if (text == newText) return
			text.setLength(0)
			text.append(newText)
		} else {
			if (textEquals(newText)) return
			text.setLength(0)
			text.append(newText)
		}
		invalidateHierarchy()
	}

	fun textEquals(other: CharSequence): Boolean {
		val length = text.length
		if (length != other.length) return false

		val chars = text.chars
		for (i in 0 until length) if (chars[i] != other[i]) return false
		return true
	}

	override fun invalidate() {
		super.invalidate()
		prefSizeInvalid = true
	}

	private fun scaleAndComputePrefSize() {
		val font = bitmapFontCache.font
		val oldScaleX = font.scaleX
		val oldScaleY = font.scaleY
		if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.data.setScale(fontScaleX, fontScaleY)
		computePrefSize()
		if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.data.setScale(oldScaleX, oldScaleY)
	}

	private fun computePrefSize() {
		prefSizeInvalid = false
		val prefSizeLayout = prefSizeLayout
		if (wrap && ellipsis == null) {
			val width = width
			prefSizeLayout.setText(bitmapFontCache.font, text, Color.WHITE, width, Align.left, true)
		} else prefSizeLayout.setText(bitmapFontCache.font, text)
		prefSize[prefSizeLayout.width] = prefSizeLayout.height
	}

	override fun layout() {
		val font = bitmapFontCache.font
		val oldScaleX = font.scaleX
		val oldScaleY = font.scaleY
		if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.data.setScale(fontScaleX, fontScaleY)
		val wrap = wrap && ellipsis == null
		if (wrap) {
			val prefHeight = prefHeight
			if (prefHeight != lastPrefHeight) {
				lastPrefHeight = prefHeight
				invalidateHierarchy()
			}
		}
		val width = width
		val height = height
		var x = 0f
		var y = 0f
		val layout = glyphLayout
		val textWidth: Float
		val textHeight: Float
		if (wrap || text.indexOf("\n") != -1) {
			// If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
			layout.setText(font, text, 0, text.length, Color.WHITE, width, lineAlign, wrap, ellipsis)
			textWidth = layout.width
			textHeight = layout.height
			if (labelAlign and Align.left == 0) {
				x += if (labelAlign and Align.right != 0) width - textWidth else (width - textWidth) / 2
			}
		} else {
			textWidth = width
			textHeight = font.data.capHeight
		}
		when {
			labelAlign and Align.top != 0 -> {
				y += if (bitmapFontCache.font.isFlipped) 0f else height - textHeight
				y += font.descent
			}
			labelAlign and Align.bottom != 0 -> {
				y += if (bitmapFontCache.font.isFlipped) height - textHeight else 0f
				y -= font.descent
			}
			else -> y += (height - textHeight) / 2
		}
		if (!bitmapFontCache.font.isFlipped) y += textHeight
		layout.setText(font, text, 0, text.length, Color.WHITE, textWidth, lineAlign, wrap, ellipsis)
		bitmapFontCache.setText(layout, x, y)
		if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.data.setScale(oldScaleX, oldScaleY)
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		validate()
		val color = tempColor.set(color)
		color.a *= parentAlpha
		if (fontColor != null) color.mul(fontColor)
		bitmapFontCache.tint(color)
		bitmapFontCache.setPosition(x, y)
		bitmapFontCache.draw(batch)
	}

	override fun getPrefWidth(): Float {
		if (wrap) return 0f
		if (prefSizeInvalid) scaleAndComputePrefSize()
		return prefSize.x
	}

	override fun getPrefHeight(): Float {
		if (prefSizeInvalid) scaleAndComputePrefSize()
		return prefSize.y - font.descent * fontScaleY * 2
	}

	/**
	 * If false, the text will only wrap where it contains newlines (\n). The preferred size of the label will be the text bounds.
	 * If true, the text will word wrap using the width of the label. The preferred width of the label will be 0, it is expected
	 * that something external will set the width of the label. Wrapping will not occur when ellipsis is enabled. Default is false.
	 *
	 *
	 * When wrap is enabled, the label's preferred height depends on the width of the label. In some cases the parent of the label
	 * will need to layout twice: once to set the width of the label and a second time to adjust to the label's new preferred
	 * height.
	 */
	fun setWrap(wrap: Boolean) {
		this.wrap = wrap
		invalidateHierarchy()
	}

	/**
	 * @param alignment Aligns all the text within the label (default left center) and each line of text horizontally (default
	 * left).
	 * @see Align
	 */
	fun setAlignment(alignment: Int) {
		setAlignment(alignment, alignment)
	}

	/**
	 * @param labelAlign Aligns all the text within the label (default left center).
	 * @param lineAlign  Aligns each line of text horizontally (default left).
	 * @see Align
	 */
	fun setAlignment(labelAlign: Int, lineAlign: Int) {
		this.labelAlign = labelAlign
		when {
			lineAlign and Align.left != 0 -> this.lineAlign = Align.left
			lineAlign and Align.right != 0 -> this.lineAlign = Align.right
			else -> this.lineAlign = Align.center
		}
		invalidate()
	}

	fun setFontScale(fontScale: Float) {
		fontScaleX = fontScale
		fontScaleY = fontScale
		invalidateHierarchy()
	}

	fun setFontScale(fontScaleX: Float, fontScaleY: Float) {
		this.fontScaleX = fontScaleX
		this.fontScaleY = fontScaleY
		invalidateHierarchy()
	}

	fun getFontScaleX(): Float {
		return fontScaleX
	}

	fun setFontScaleX(fontScaleX: Float) {
		this.fontScaleX = fontScaleX
		invalidateHierarchy()
	}

	fun getFontScaleY(): Float {
		return fontScaleY
	}

	fun setFontScaleY(fontScaleY: Float) {
		this.fontScaleY = fontScaleY
		invalidateHierarchy()
	}

	/**
	 * When non-null the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur
	 * when ellipsis is enabled. Default is false.
	 */
	fun setEllipsis(ellipsis: String?) {
		this.ellipsis = ellipsis
	}

	/**
	 * When true the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur when
	 * ellipsis is true. Default is false.
	 */
	fun setEllipsis(ellipsis: Boolean) {
		if (ellipsis) this.ellipsis = "..." else this.ellipsis = null
	}

	override fun toString(): String {
		return super.toString() + ": " + text
	}

	fun setMaxWidth(maxWidth: Int) {
		while (glyphLayout.width > maxWidth) {
			size *= 0.95f
			updateFont()
		}
	}

	private fun updateFont() {
		font = metaFontProvider.getFont(size.roundToInt(), type)
		setText(text)
		bitmapFontCache = font.newFontCache()
		layout()
	}

	fun setFontSize(size: Int) {
		this.size = size.toFloat()
		updateFont()
	}

	/**
	 * The style for a label, see [MetaLabel].
	 *
	 * @property font
	 * @property fontColor *Optional*
	 * @property background *Optional*
	 * @author Nathan Sweet
	 */
	class LabelStyle(
		var font: BitmapFont,
		var fontColor: Color? = null,
		var background: Drawable? = null,
	) {
		constructor(style: LabelStyle) : this(
			style.font,
			if (style.fontColor != null) Color(style.fontColor) else null,
			style.background,
		)
	}

	companion object {
		private val tempColor = Color()
		private val prefSizeLayout = GlyphLayout()
	}

	init {
		font = metaFontProvider.getFont(size, type)
		setAlignment(Align.center)
		fontColor = color
		setText(text)
		bitmapFontCache = font.newFontCache()
		layout()
	}
}

@Suppress("FunctionName")
@OptIn(ExperimentalContracts::class)
inline fun MetaLabel(
	text: CharSequence,
	size: Int,
	color: Color? = Color.WHITE,
	type: FontType = FontType.REGULAR,
	init: MetaLabel.() -> Unit
): MetaLabel {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MetaLabel(text, size, color, type).apply(init)
}