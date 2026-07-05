package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.VisUI

/**
 * Runtime-generated Meta skin resources. These are deliberately created from code instead of bundled UI textures so
 * downstream games get a polished default look without shipping a full custom atlas for basic controls.
 */
object MetaSkin {
	const val BUTTON = "meta.button"
	const val BUTTON_TOGGLE = "meta.button.toggle"
	const val ICON_BUTTON = "meta.iconButton"
	const val CHECKBOX = "meta.checkbox"
	const val TEXT_FIELD = "meta.textField"
	const val TEXT_FIELD_ERROR = "meta.textField.error"
	const val SELECT_BOX = "meta.selectBox"
	const val SCROLL_PANE = "meta.scrollPane"
	const val SLIDER_HORIZONTAL = "meta.slider.horizontal"
	const val PROGRESS_HORIZONTAL = "meta.progress.horizontal"
	const val SEPARATOR = "meta.separator"
	const val WINDOW = "meta.window"
	const val WINDOW_RESIZABLE = "meta.window.resizable"

	const val ICON_PLUS = "meta.icon.plus"
	const val ICON_MINUS = "meta.icon.minus"
	const val ICON_CLOSE = "meta.icon.close"

	private const val INSTALLED_COLOR = "meta.skin.installed"
	private const val PATCH_SIZE = 32
	private const val ICON_SIZE = 24

	fun skin(): Skin = VisUI.getSkin()

	fun install(skin: Skin) {
		if (skin.has(INSTALLED_COLOR, Color::class.java)) return

		addPalette(skin)
		addPanelDrawables(skin)
		addControlDrawables(skin)
		addIcons(skin)
		addStyles(skin)

		skin.add(INSTALLED_COLOR, Color.WHITE.cpy())
	}

	private fun addPalette(skin: Skin) {
		skin.add("meta.background", MetaColor.BACKGROUND.cpy())
		skin.add("meta.surface", MetaColor.SURFACE.cpy())
		skin.add("meta.surfaceRaised", MetaColor.SURFACE_RAISED.cpy())
		skin.add("meta.border", MetaColor.BORDER.cpy())
		skin.add("meta.text", MetaColor.TEXT.cpy())
		skin.add("meta.textMuted", MetaColor.TEXT_MUTED.cpy())
		skin.add("meta.textDisabled", MetaColor.TEXT_DISABLED.cpy())
		skin.add("meta.accent", MetaColor.ACCENT.cpy())
	}

	private fun addPanelDrawables(skin: Skin) {
		rounded(skin, "meta.panel", MetaColor.SURFACE, MetaColor.BORDER, radius = 7, border = 1, padding = 8f)
		rounded(skin, "meta.panel.raised", MetaColor.SURFACE_RAISED, MetaColor.BORDER, radius = 7, border = 1, padding = 8f)
		rounded(skin, "meta.tooltip", Color.valueOf("18191DEE"), MetaColor.BORDER, radius = 6, border = 1, padding = 8f)
	}

	private fun addControlDrawables(skin: Skin) {
		rounded(skin, "meta.button.up", Color.valueOf("34363CFF"), Color.valueOf("4A4D56FF"), radius = 6, border = 1, padding = 9f)
		rounded(skin, "meta.button.over", Color.valueOf("3E424BFF"), Color.valueOf("5A6572FF"), radius = 6, border = 1, padding = 9f)
		rounded(skin, "meta.button.down", Color.valueOf("25282EFF"), MetaColor.ACCENT, radius = 6, border = 1, padding = 9f)
		rounded(skin, "meta.button.checked", Color.valueOf("253849FF"), MetaColor.ACCENT, radius = 6, border = 1, padding = 9f)
		rounded(skin, "meta.button.disabled", Color.valueOf("292A2EFF"), Color.valueOf("34363AFF"), radius = 6, border = 1, padding = 9f)

		rounded(skin, "meta.field.up", Color.valueOf("202126FF"), Color.valueOf("42454DFF"), radius = 5, border = 1, padding = 7f)
		rounded(skin, "meta.field.over", Color.valueOf("24262BFF"), Color.valueOf("596170FF"), radius = 5, border = 1, padding = 7f)
		rounded(skin, "meta.field.focus", Color.valueOf("202126FF"), MetaColor.ACCENT, radius = 5, border = 2, padding = 7f)
		rounded(skin, "meta.field.error", Color.valueOf("241E20FF"), MetaColor.NEGATIVE, radius = 5, border = 2, padding = 7f)
		rounded(skin, "meta.selection", Color.valueOf("2F5D86FF"), null, radius = 3, border = 0, padding = 4f)

		rounded(skin, "meta.scroll.track", Color.valueOf("20212688"), null, radius = 4, border = 0, padding = 0f)
		rounded(skin, "meta.scroll.knob", Color.valueOf("5B626EFF"), null, radius = 4, border = 0, padding = 0f)
		rounded(skin, SEPARATOR, Color.valueOf("444850FF"), null, radius = 1, border = 0, padding = 0f)
		rounded(skin, "meta.slider.track", Color.valueOf("24262BFF"), Color.valueOf("363A42FF"), radius = 4, border = 1, padding = 0f)
		rounded(skin, "meta.slider.fill", Color.valueOf("376F9FFF"), null, radius = 4, border = 0, padding = 0f)
		rounded(skin, "meta.slider.knob", MetaColor.ACCENT, Color.valueOf("A7D7FFFF"), radius = 8, border = 1, padding = 0f)

		checkbox(skin, "meta.checkbox.off", checked = false, over = false, down = false)
		checkbox(skin, "meta.checkbox.over", checked = false, over = true, down = false)
		checkbox(skin, "meta.checkbox.down", checked = false, over = true, down = true)
		checkbox(skin, "meta.checkbox.on", checked = true, over = false, down = false)
		checkbox(skin, "meta.checkbox.onOver", checked = true, over = true, down = false)
		checkbox(skin, "meta.checkbox.disabled", checked = false, over = false, down = false, disabled = true)
		checkbox(skin, "meta.checkbox.onDisabled", checked = true, over = false, down = false, disabled = true)
	}

	private fun addIcons(skin: Skin) {
		icon(skin, ICON_MINUS, plus = false)
		icon(skin, ICON_PLUS, plus = true)
		closeIcon(skin, ICON_CLOSE)
	}

	private fun addStyles(skin: Skin) {
		skin.add(BUTTON, Button.ButtonStyle().apply {
			up = skin.getDrawable("meta.button.up")
			over = skin.getDrawable("meta.button.over")
			down = skin.getDrawable("meta.button.down")
			checked = skin.getDrawable("meta.button.checked")
			checkedOver = skin.getDrawable("meta.button.over")
			disabled = skin.getDrawable("meta.button.disabled")
		})
		skin.add(BUTTON_TOGGLE, Button.ButtonStyle(skin.get(BUTTON, Button.ButtonStyle::class.java)).apply {
			up = skin.getDrawable("meta.button.up")
			checked = skin.getDrawable("meta.button.checked")
		})
		skin.add(ICON_BUTTON, Button.ButtonStyle(skin.get(BUTTON, Button.ButtonStyle::class.java)))
		skin.add(CHECKBOX, Button.ButtonStyle().apply {
			up = skin.getDrawable("meta.checkbox.off")
			over = skin.getDrawable("meta.checkbox.over")
			down = skin.getDrawable("meta.checkbox.down")
			checked = skin.getDrawable("meta.checkbox.on")
			checkedOver = skin.getDrawable("meta.checkbox.onOver")
			disabled = skin.getDrawable("meta.checkbox.disabled")
		})

		if (skin.has("default", com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle::class.java)) {
			val base = skin.get("default", com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle::class.java)
			skin.add(WINDOW, com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(base).apply {
				background = skin.getDrawable("meta.panel.raised")
			})
			skin.add(WINDOW_RESIZABLE, com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(base).apply {
				background = skin.getDrawable("meta.panel.raised")
			})
		}

		if (skin.has("default", TextField.TextFieldStyle::class.java)) {
			val base = skin.get("default", TextField.TextFieldStyle::class.java)
			skin.add(TEXT_FIELD, TextField.TextFieldStyle(base).apply {
				background = skin.getDrawable("meta.field.up")
				focusedBackground = skin.getDrawable("meta.field.focus")
				selection = skin.getDrawable("meta.selection")
			})
			skin.add(TEXT_FIELD_ERROR, TextField.TextFieldStyle(base).apply {
				background = skin.getDrawable("meta.field.error")
				focusedBackground = skin.getDrawable("meta.field.error")
				selection = skin.getDrawable("meta.selection")
			})
		}

		if (skin.has("default", ScrollPane.ScrollPaneStyle::class.java)) {
			skin.add(SCROLL_PANE, ScrollPane.ScrollPaneStyle(skin.get(ScrollPane.ScrollPaneStyle::class.java)).apply {
				background = skin.getDrawable("meta.panel")
				vScroll = skin.getDrawable("meta.scroll.track")
				hScroll = skin.getDrawable("meta.scroll.track")
				vScrollKnob = skin.getDrawable("meta.scroll.knob")
				hScrollKnob = skin.getDrawable("meta.scroll.knob")
			})
		}

		if (skin.has("default", SelectBox.SelectBoxStyle::class.java)) {
			val base = skin.get(SelectBox.SelectBoxStyle::class.java)
			skin.add(SELECT_BOX, SelectBox.SelectBoxStyle(base).apply {
				background = skin.getDrawable("meta.field.up")
				backgroundOver = skin.getDrawable("meta.field.over")
				listStyle = List.ListStyle(base.listStyle).apply {
					background = skin.getDrawable("meta.panel.raised")
					selection = skin.getDrawable("meta.selection")
				}
				scrollStyle = if (skin.has(SCROLL_PANE, ScrollPane.ScrollPaneStyle::class.java)) {
					skin.get(SCROLL_PANE, ScrollPane.ScrollPaneStyle::class.java)
				} else {
					base.scrollStyle
				}
			})
		}

		if (skin.has("default-horizontal", Slider.SliderStyle::class.java)) {
			skin.add(SLIDER_HORIZONTAL, Slider.SliderStyle(skin.get("default-horizontal", Slider.SliderStyle::class.java)).apply {
				background = skin.getDrawable("meta.slider.track")
				knobBefore = skin.getDrawable("meta.slider.fill")
				knob = skin.getDrawable("meta.slider.knob")
				knobOver = skin.getDrawable("meta.slider.knob")
				knobDown = skin.getDrawable("meta.slider.knob")
			})
		}
		if (skin.has("default-horizontal", ProgressBar.ProgressBarStyle::class.java)) {
			skin.add(PROGRESS_HORIZONTAL, ProgressBar.ProgressBarStyle(skin.get("default-horizontal", ProgressBar.ProgressBarStyle::class.java)).apply {
				background = skin.getDrawable("meta.slider.track")
				knob = skin.getDrawable("meta.slider.fill")
				knobBefore = skin.getDrawable("meta.slider.fill")
			})
		}
	}

	private fun rounded(
		skin: Skin,
		name: String,
		fill: Color,
		stroke: Color?,
		radius: Int,
		border: Int,
		padding: Float,
	) {
		val pixmap = Pixmap(PATCH_SIZE, PATCH_SIZE, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		val fillBits = Color.rgba8888(fill)
		val strokeBits = if (stroke != null && border > 0) Color.rgba8888(stroke) else 0
		val innerRadius = (radius - border).coerceAtLeast(0)
		for (y in 0 until PATCH_SIZE) {
			for (x in 0 until PATCH_SIZE) {
				val outer = insideRoundRect(x + 0.5f, y + 0.5f, PATCH_SIZE.toFloat(), PATCH_SIZE.toFloat(), radius.toFloat())
				if (!outer) continue
				val inner = border <= 0 || insideRoundRect(
					x + 0.5f - border,
					y + 0.5f - border,
					(PATCH_SIZE - border * 2).toFloat(),
					(PATCH_SIZE - border * 2).toFloat(),
					innerRadius.toFloat(),
				)
				pixmap.drawPixel(x, y, if (!inner && stroke != null) strokeBits else fillBits)
			}
		}
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		val split = radius + 2
		val drawable = NinePatchDrawable(NinePatch(texture, split, split, split, split)).apply {
			leftWidth = padding
			rightWidth = padding
			topHeight = padding
			bottomHeight = padding
			minWidth = 24f
			minHeight = 24f
		}
		skin.add(name, drawable, Drawable::class.java)
	}

	private fun checkbox(
		skin: Skin,
		name: String,
		checked: Boolean,
		over: Boolean,
		down: Boolean,
		disabled: Boolean = false,
	) {
		val pixmap = Pixmap(ICON_SIZE, ICON_SIZE, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		val fill = when {
			disabled -> Color.valueOf("292A2EFF")
			down -> Color.valueOf("253849FF")
			over -> Color.valueOf("343842FF")
			else -> Color.valueOf("202126FF")
		}
		val stroke = when {
			disabled -> Color.valueOf("3A3A40FF")
			checked -> MetaColor.ACCENT
			over -> Color.valueOf("6D7584FF")
			else -> Color.valueOf("4A4D56FF")
		}
		drawRoundedPixels(pixmap, fill, stroke, radius = 5, border = 2)
		if (checked) drawCheck(pixmap, if (disabled) MetaColor.TEXT_DISABLED else MetaColor.TEXT)
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		skin.add(name, TextureRegionDrawable(TextureRegion(texture)).apply {
			minWidth = ICON_SIZE.toFloat()
			minHeight = ICON_SIZE.toFloat()
		}, Drawable::class.java)
	}

	private fun icon(skin: Skin, name: String, plus: Boolean) {
		val pixmap = Pixmap(ICON_SIZE, ICON_SIZE, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		val bits = Color.rgba8888(MetaColor.TEXT)
		for (x in 6 until ICON_SIZE - 6) for (y in 11..13) pixmap.drawPixel(x, y, bits)
		if (plus) for (x in 11..13) for (y in 6 until ICON_SIZE - 6) pixmap.drawPixel(x, y, bits)
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		skin.add(name, TextureRegionDrawable(TextureRegion(texture)).apply {
			minWidth = ICON_SIZE.toFloat()
			minHeight = ICON_SIZE.toFloat()
		}, Drawable::class.java)
	}

	private fun closeIcon(skin: Skin, name: String) {
		val pixmap = Pixmap(ICON_SIZE, ICON_SIZE, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		val bits = Color.rgba8888(MetaColor.TEXT)
		drawLine(pixmap, 7, 7, 17, 17, bits)
		drawLine(pixmap, 17, 7, 7, 17, bits)
		drawLine(pixmap, 8, 7, 18, 17, bits)
		drawLine(pixmap, 18, 7, 8, 17, bits)
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		skin.add(name, TextureRegionDrawable(TextureRegion(texture)).apply {
			minWidth = ICON_SIZE.toFloat()
			minHeight = ICON_SIZE.toFloat()
		}, Drawable::class.java)
	}

	private fun drawRoundedPixels(pixmap: Pixmap, fill: Color, stroke: Color, radius: Int, border: Int) {
		val fillBits = Color.rgba8888(fill)
		val strokeBits = Color.rgba8888(stroke)
		for (y in 0 until pixmap.height) {
			for (x in 0 until pixmap.width) {
				val outer = insideRoundRect(x + 0.5f, y + 0.5f, pixmap.width.toFloat(), pixmap.height.toFloat(), radius.toFloat())
				if (!outer) continue
				val inner = insideRoundRect(
					x + 0.5f - border,
					y + 0.5f - border,
					(pixmap.width - border * 2).toFloat(),
					(pixmap.height - border * 2).toFloat(),
					(radius - border).coerceAtLeast(0).toFloat(),
				)
				pixmap.drawPixel(x, y, if (inner) fillBits else strokeBits)
			}
		}
	}

	private fun drawCheck(pixmap: Pixmap, color: Color) {
		val bits = Color.rgba8888(color)
		drawLine(pixmap, 6, 12, 10, 16, bits)
		drawLine(pixmap, 10, 16, 18, 7, bits)
		drawLine(pixmap, 6, 13, 10, 17, bits)
		drawLine(pixmap, 10, 17, 18, 8, bits)
	}

	private fun drawLine(pixmap: Pixmap, x0: Int, y0: Int, x1: Int, y1: Int, colorBits: Int) {
		var x = x0
		var y = y0
		val dx = kotlin.math.abs(x1 - x0)
		val sx = if (x0 < x1) 1 else -1
		val dy = -kotlin.math.abs(y1 - y0)
		val sy = if (y0 < y1) 1 else -1
		var err = dx + dy
		while (true) {
			pixmap.drawPixel(x, y, colorBits)
			if (x == x1 && y == y1) break
			val e2 = 2 * err
			if (e2 >= dy) {
				err += dy
				x += sx
			}
			if (e2 <= dx) {
				err += dx
				y += sy
			}
		}
	}

	private fun insideRoundRect(px: Float, py: Float, width: Float, height: Float, radius: Float): Boolean {
		if (width <= 0f || height <= 0f) return false
		val r = radius.coerceAtMost(width * 0.5f).coerceAtMost(height * 0.5f)
		val cx = px.coerceIn(r, width - r)
		val cy = py.coerceIn(r, height - r)
		val dx = px - cx
		val dy = py - cy
		return dx * dx + dy * dy <= r * r
	}
}
