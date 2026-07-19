package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.BitmapFont
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

/**
 * Runtime-generated Meta skin resources. These are deliberately created from code instead of bundled UI textures so
 * downstream games get a polished default look without shipping a full custom atlas for basic controls.
 */
object MetaSkin {
	const val BUTTON_PRIMARY = "meta.button.primary"
	const val BUTTON_SECONDARY = "meta.button"
	const val BUTTON_TERTIARY = "meta.button.tertiary"
	const val BUTTON = BUTTON_SECONDARY
	const val BUTTON_TOGGLE = "meta.button.toggle"
	const val ICON_BUTTON = "meta.iconButton"
	const val IMAGE_BUTTON = "meta.imageButton"
	const val CHECKBOX = "meta.checkbox"
	const val TEXT_FIELD = "meta.textField"
	const val TEXT_FIELD_ERROR = "meta.textField.error"
	const val SPINNER_TEXT_FIELD = "meta.spinner.textField"
	const val SPINNER_DECREMENT = "meta.spinner.decrement"
	const val SPINNER_INCREMENT = "meta.spinner.increment"
	const val TEXT_AREA = "meta.textArea"
	const val SELECT_BOX = "meta.selectBox"
	const val DROPDOWN = "meta.dropdown"
	const val SCROLL_PANE = "meta.scrollPane"
	const val SCROLL_PANE_FLAT = "meta.scrollPane.flat"
	const val SLIDER_HORIZONTAL = "meta.slider.horizontal"
	const val PROGRESS_HORIZONTAL = "meta.progress.horizontal"
	const val SEPARATOR = "meta.separator"
	const val TOAST = "meta.toast"
	const val TOAST_PRIMARY = "meta.toast.primary"
	const val TOAST_WARNING = "meta.toast.warning"
	const val TOAST_ERROR = "meta.toast.error"
	const val TOAST_MUTED = "meta.toast.muted"
	const val BOTTOM_BAR = "meta.bottomBar"
	const val COLOR_FILL = "meta.color.fill"
	const val LOADING_RING = "meta.loading.ring"
	const val WINDOW = "meta.window"
	const val WINDOW_RESIZABLE = "meta.window.resizable"
	const val WINDOW_SHADOW = "meta.window.shadow"
	private const val WINDOW_BACKGROUND = "meta.window.background"

	private const val INSTALLED_COLOR = "meta.skin.installed"
	private const val PATCH_SIZE = 32
	private const val SHAPE_AA_SAMPLES = 4
	private const val FIELD_RADIUS = 5
	private const val ICON_SIZE = 24
	private const val ICON_PIXMAP_SCALE = 3
	private const val ICON_PIXMAP_SIZE = ICON_SIZE * ICON_PIXMAP_SCALE
	private const val LOADING_RING_PIXMAP_SIZE = 64
	private var activeSkin: Skin? = null

	fun skin(): Skin = activeSkin ?: error("MetaSkin has not been initialized")

	fun buttonStyle(tier: MetaButtonTier): Button.ButtonStyle = skin().get(
		when (tier) {
			MetaButtonTier.PRIMARY -> BUTTON_PRIMARY
			MetaButtonTier.SECONDARY -> BUTTON_SECONDARY
			MetaButtonTier.TERTIARY -> BUTTON_TERTIARY
		},
		Button.ButtonStyle::class.java,
	)

	fun initialize(skin: Skin = Skin(), installDefaults: Boolean = true): Skin {
		activeSkin = skin
		if (installDefaults) install(skin)
		return skin
	}

	fun dispose() {
		activeSkin?.dispose()
		activeSkin = null
	}

	fun install(skin: Skin) {
		if (skin.has(INSTALLED_COLOR, Color::class.java)) return

		addPalette(skin)
		addPanelDrawables(skin)
		addControlDrawables(skin)
		addStyles(skin)

		skin.add(INSTALLED_COLOR, Color.WHITE.cpy())
	}

	private fun addPalette(skin: Skin) {
		skin.add("meta.background", MetaColor.BACKGROUND.cpy())
		skin.add("meta.surface", MetaColor.SURFACE.cpy())
		skin.add("meta.surfaceRaised", MetaColor.SURFACE_RAISED.cpy())
		skin.add("meta.border", MetaColor.BORDER.cpy())
		skin.add("meta.borderStrong", MetaColor.BORDER_STRONG.cpy())
		skin.add("meta.text", MetaColor.TEXT.cpy())
		skin.add("meta.textMuted", MetaColor.TEXT_MUTED.cpy())
		skin.add("meta.textDisabled", MetaColor.TEXT_DISABLED.cpy())
		skin.add("meta.accent", MetaColor.ACCENT.cpy())
		skin.add("meta.selection", MetaColor.SELECTION.cpy())
		skin.add("meta.selectionFill", MetaColor.SELECTION_FILL.cpy())
	}

	private fun addPanelDrawables(skin: Skin) {
		solid(skin, COLOR_FILL, Color.WHITE, minWidth = 1f, minHeight = 1f)
		loadingRing(skin)
		solid(skin, "meta.menu.bar", MetaColor.SURFACE, minWidth = 1f, minHeight = 1f)
		rounded(skin, "meta.panel", MetaColor.SURFACE, MetaColor.BORDER, radius = 4, border = 1, padding = 8f)
		rounded(skin, "meta.panel.raised", MetaColor.SURFACE_RAISED, MetaColor.BORDER, radius = 4, border = 1, padding = 8f)
		// Windows need a stronger silhouette than nested panels. The strong cool-gray edge remains neutral while
		// making overlapping windows and adjacent chrome unambiguous against the dark workspace.
		chamfered(skin, WINDOW_BACKGROUND, MetaColor.SURFACE_RAISED, MetaColor.BORDER_STRONG,
			cut = 6, border = 1, padding = 8f)
		// Offset below every solid window so a fully covered sibling still leaves a visible depth cue at its edges.
		rounded(skin, WINDOW_SHADOW, Color.valueOf("05060988"), null, radius = 9, border = 0, padding = 0f)
		// Neutral notifications must not outshout semantic toasts, so they get the neutral strong border.
		rounded(skin, TOAST, Color.valueOf("15181EF5"), MetaColor.BORDER_STRONG, radius = 6, border = 2, padding = 10f)
		rounded(skin, TOAST_PRIMARY, Color.valueOf("172733F7"), MetaColor.ACCENT, radius = 6, border = 2, padding = 10f)
		rounded(skin, TOAST_WARNING, Color.valueOf("302717F7"), MetaColor.WARNING, radius = 6, border = 2, padding = 10f)
		rounded(skin, TOAST_ERROR, Color.valueOf("321C20F7"), MetaColor.NEGATIVE, radius = 6, border = 2, padding = 10f)
		rounded(skin, TOAST_MUTED, Color.valueOf("20242AF2"), MetaColor.BORDER, radius = 6, border = 1, padding = 10f)
		rounded(skin, "meta.tooltip", Color.valueOf("18191DEE"), MetaColor.BORDER, radius = 6, border = 1, padding = 8f)
		rounded(skin, DROPDOWN, Color.valueOf("202126FA"), MetaColor.BORDER_STRONG, radius = 7, border = 1, padding = 7f)
		topRounded(skin, BOTTOM_BAR, Color.valueOf("080A0ECC"), Color.valueOf("2F333BAA"), radius = 12, border = 1, padding = 14f)
	}

	private fun loadingRing(skin: Skin) {
		val pixmap = Pixmap(LOADING_RING_PIXMAP_SIZE, LOADING_RING_PIXMAP_SIZE, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		val center = LOADING_RING_PIXMAP_SIZE / 2
		pixmap.setColor(Color.WHITE)
		pixmap.fillCircle(center, center, 28)
		pixmap.setColor(Color.CLEAR)
		pixmap.fillCircle(center, center, 22)
		pixmap.fillTriangle(center, center, center - 15, LOADING_RING_PIXMAP_SIZE, center + 15, LOADING_RING_PIXMAP_SIZE)
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$LOADING_RING.texture", texture)
		skin.add(LOADING_RING, TextureRegionDrawable(TextureRegion(texture)).apply {
			minWidth = 1f
			minHeight = 1f
		}, Drawable::class.java)
	}

	private fun addControlDrawables(skin: Skin) {
		rounded(skin, "meta.button.primary.up", MetaColor.PRIMARY, MetaColor.ACCENT_DIM, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.primary.over", MetaColor.PRIMARY_HOVER, MetaColor.ACCENT, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.primary.focus", MetaColor.PRIMARY, MetaColor.ACCENT_BRIGHT, radius = 4, border = 2, padding = 9f)
		rounded(skin, "meta.button.primary.down", MetaColor.PRIMARY_PRESSED, MetaColor.ACCENT, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.primary.disabled", MetaColor.FILL_SUNKEN, MetaColor.BORDER_DISABLED, radius = 4, border = 1, padding = 9f)

		rounded(skin, "meta.button.up", MetaColor.SECONDARY, MetaColor.BORDER, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.over", MetaColor.SECONDARY_HOVER, MetaColor.BORDER_STRONG, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.focus", MetaColor.FILL_FOCUS, MetaColor.ACCENT, radius = 4, border = 2, padding = 9f)
		rounded(skin, "meta.button.focusOver", MetaColor.SECONDARY_HOVER, MetaColor.ACCENT_BRIGHT, radius = 4, border = 2, padding = 9f)
		rounded(skin, "meta.button.down", MetaColor.FILL_PRESSED, MetaColor.ACCENT, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.checked", MetaColor.SELECTION_FILL, MetaColor.ACCENT, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.checkedOver", MetaColor.SELECTION, MetaColor.ACCENT, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.checkedFocus", MetaColor.SELECTION_FILL, MetaColor.ACCENT_BRIGHT, radius = 4, border = 2, padding = 9f)
		rounded(skin, "meta.button.disabled", MetaColor.FILL_SUNKEN, MetaColor.BORDER_DISABLED, radius = 4, border = 1, padding = 9f)

		// Tertiary is low-emphasis, not invisible. Ordinary buttons and selectable rows keep a restrained resting
		// frame so their hit area remains legible; only MetaImageButton utilities are borderless until interaction.
		rounded(skin, "meta.button.tertiary.up", MetaColor.TERTIARY, MetaColor.BORDER, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.tertiary.over", MetaColor.TERTIARY_HOVER, MetaColor.BORDER_STRONG, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.tertiary.focus", MetaColor.FILL_FOCUS, MetaColor.ACCENT, radius = 4, border = 2, padding = 9f)
		rounded(skin, "meta.button.tertiary.down", MetaColor.FILL_PRESSED, MetaColor.ACCENT, radius = 4, border = 1, padding = 9f)
		rounded(skin, "meta.button.tertiary.disabled", MetaColor.TERTIARY, MetaColor.BORDER_DISABLED, radius = 4, border = 1, padding = 9f)

	// Image utilities stay visually lighter than tertiary actions, but retain a faint resting edge so their hit
	// target is discoverable without requiring hover (important for keyboard/controller and dense editor toolbars).
		rounded(skin, "meta.imageButton.up", MetaColor.TERTIARY, Color.valueOf("353E48FF"), radius = 3, border = 1, padding = 8f)
		rounded(skin, "meta.imageButton.over", MetaColor.TERTIARY_HOVER, MetaColor.BORDER_STRONG, radius = 3, border = 1, padding = 8f)
		rounded(skin, "meta.imageButton.focus", MetaColor.FILL_FOCUS, MetaColor.ACCENT, radius = 3, border = 2, padding = 8f)
		rounded(skin, "meta.imageButton.down", MetaColor.FILL_PRESSED, MetaColor.ACCENT, radius = 3, border = 1, padding = 8f)
		// Checked is a persistent "on" state: selection fill plus dim-accent edge, clearly distinct from the
		// 2px bright-accent focus ring so keyboard users can tell the cursor from a toggled-on tool.
		rounded(skin, "meta.imageButton.checked", MetaColor.SELECTION_FILL, MetaColor.ACCENT_DIM, radius = 3, border = 1, padding = 8f)
		rounded(skin, "meta.imageButton.checkedOver", MetaColor.SELECTION, MetaColor.ACCENT_DIM, radius = 3, border = 1, padding = 8f)
		rounded(skin, "meta.imageButton.checkedFocus", MetaColor.SELECTION_FILL, MetaColor.ACCENT_BRIGHT, radius = 3, border = 2, padding = 8f)
		rounded(skin, "meta.imageButton.disabled", Color.valueOf("00000000"), null, radius = 3, border = 0, padding = 8f)

		rounded(skin, "meta.field.up", MetaColor.FILL_FIELD, MetaColor.BORDER_STRONG, radius = FIELD_RADIUS, border = 1, padding = 7f)
		rounded(skin, "meta.field.over", Color.valueOf("20262DFF"), MetaColor.BORDER_STRONG, radius = FIELD_RADIUS, border = 1, padding = 7f)
		rounded(skin, "meta.field.focus", MetaColor.FILL_FIELD, MetaColor.ACCENT, radius = FIELD_RADIUS, border = 2, padding = 7f)
		rounded(skin, "meta.field.error", Color.valueOf("241E20FF"), MetaColor.NEGATIVE, radius = FIELD_RADIUS, border = 2, padding = 7f)
		rounded(skin, "meta.field.disabled", MetaColor.FILL_SUNKEN, MetaColor.BORDER_DISABLED, radius = FIELD_RADIUS, border = 1, padding = 7f)
		rounded(skin, "meta.field.focusBorder", TRANSPARENT, MetaColor.ACCENT, radius = FIELD_RADIUS, border = 2, padding = 0f)
		rounded(skin, "meta.field.errorBorder", TRANSPARENT, MetaColor.NEGATIVE, radius = FIELD_RADIUS, border = 2, padding = 0f)
		rounded(skin, "meta.selection", MetaColor.SELECTION, null, radius = 3, border = 0, padding = 4f)
		rounded(skin, "meta.dropdown.over", MetaColor.TERTIARY_HOVER, null, radius = 5, border = 0, padding = 6f)
		rounded(skin, "meta.dropdown.selection", MetaColor.SELECTION_FILL, MetaColor.ACCENT_DIM, radius = 5, border = 1, padding = 6f)
		rounded(skin, "meta.menu.bar.open", TRANSPARENT, null, radius = 5, border = 0, padding = 0f)
		rounded(skin, "meta.menu.bar.over", MetaColor.TERTIARY_HOVER, null, radius = 5, border = 0, padding = 0f)
		rounded(skin, "meta.menu.bar.selected", MetaColor.SELECTION_FILL, MetaColor.ACCENT, radius = 5, border = 1, padding = 0f)
		configureToolbarDrawable(skin.getDrawable("meta.menu.bar.open"))
		configureToolbarDrawable(skin.getDrawable("meta.menu.bar.over"))
		configureToolbarDrawable(skin.getDrawable("meta.menu.bar.selected"))
		rounded(skin, "meta.menu.item", TRANSPARENT, null, radius = 4, border = 0, padding = 0f)
		rounded(skin, "meta.menu.item.over", MetaColor.TERTIARY_HOVER, null, radius = 4, border = 0, padding = 0f)
		rounded(skin, "meta.menu.item.selected", MetaColor.SELECTION_FILL, null, radius = 4, border = 0, padding = 0f)
		configureMenuItemDrawable(skin.getDrawable("meta.menu.item"))
		configureMenuItemDrawable(skin.getDrawable("meta.menu.item.over"))
		configureMenuItemDrawable(skin.getDrawable("meta.menu.item.selected"))
		solid(skin, "meta.cursor", MetaColor.TEXT, minWidth = 2f, minHeight = 20f)

		rounded(skin, "meta.scroll.track", Color.valueOf("20212666"), null, radius = 3, border = 0, padding = 0f, minWidth = 6f, minHeight = 6f)
		rounded(skin, "meta.scroll.knob", Color.valueOf("858F9EFF"), null, radius = 3, border = 0, padding = 0f, minWidth = 6f, minHeight = 6f)
		solid(skin, SEPARATOR, MetaColor.BORDER_STRONG, minWidth = 1f, minHeight = 1f)
		rounded(skin, "meta.slider.track", Color.valueOf("24262BFF"), MetaColor.BORDER, radius = 4, border = 1, padding = 0f, minHeight = 8f)
		rounded(skin, "meta.slider.fill", MetaColor.ACCENT_DIM, null, radius = 4, border = 0, padding = 0f, minHeight = 8f)
		// Knobs are true circles; a nine-patch drawn below its corner budget squashes, so rasterize them directly.
		circle(skin, "meta.slider.knob", MetaColor.ACCENT, MetaColor.ACCENT_BRIGHT, border = 1, diameter = 18)
		circle(skin, "meta.slider.knob.over", MetaColor.ACCENT_BRIGHT, Color.WHITE, border = 2, diameter = 20)
		circle(skin, "meta.slider.knob.down", MetaColor.PRIMARY, MetaColor.ACCENT_BRIGHT, border = 2, diameter = 20)

		segmentedRounded(skin, "meta.spinner.left.up", MetaColor.TERTIARY, MetaColor.BORDER, roundLeft = true)
		segmentedRounded(skin, "meta.spinner.left.over", MetaColor.TERTIARY_HOVER, MetaColor.BORDER_STRONG, roundLeft = true)
		segmentedRounded(skin, "meta.spinner.left.down", MetaColor.FILL_PRESSED, MetaColor.ACCENT, roundLeft = true)
		segmentedRounded(skin, "meta.spinner.right.up", MetaColor.TERTIARY, MetaColor.BORDER, roundRight = true)
		segmentedRounded(skin, "meta.spinner.right.over", MetaColor.TERTIARY_HOVER, MetaColor.BORDER_STRONG, roundRight = true)
		segmentedRounded(skin, "meta.spinner.right.down", MetaColor.FILL_PRESSED, MetaColor.ACCENT, roundRight = true)
		rounded(skin, "meta.spinner.field.up", MetaColor.FILL_SUNKEN, MetaColor.BORDER_STRONG, radius = 0, border = 1, padding = 7f)
		rounded(skin, "meta.spinner.field.focus", MetaColor.FILL_SUNKEN, MetaColor.ACCENT, radius = 0, border = 2, padding = 7f)
		rounded(skin, "meta.spinner.field.disabled", MetaColor.FILL_SUNKEN, MetaColor.BORDER_DISABLED, radius = 0, border = 1, padding = 7f)

		checkbox(skin, "meta.checkbox.off", checked = false, over = false, down = false)
		checkbox(skin, "meta.checkbox.over", checked = false, over = true, down = false)
		checkbox(skin, "meta.checkbox.focus", checked = false, over = true, down = false, focused = true)
		checkbox(skin, "meta.checkbox.down", checked = false, over = true, down = true)
		checkbox(skin, "meta.checkbox.on", checked = true, over = false, down = false)
		checkbox(skin, "meta.checkbox.onOver", checked = true, over = true, down = false)
		checkbox(skin, "meta.checkbox.onFocus", checked = true, over = true, down = false, focused = true)
		checkbox(skin, "meta.checkbox.disabled", checked = false, over = false, down = false, disabled = true)
		checkbox(skin, "meta.checkbox.onDisabled", checked = true, over = false, down = false, disabled = true)
	}

	private fun addStyles(skin: Skin) {
		val defaultFont = if (skin.has("default-font", BitmapFont::class.java)) {
			skin.get("default-font", BitmapFont::class.java)
		} else {
			BitmapFont().also { skin.add("default-font", it, BitmapFont::class.java) }
		}
		skin.add(BUTTON_PRIMARY, Button.ButtonStyle().apply {
			up = skin.getDrawable("meta.button.primary.up")
			over = skin.getDrawable("meta.button.primary.over")
			down = skin.getDrawable("meta.button.primary.down")
			focused = skin.getDrawable("meta.button.primary.focus")
			disabled = skin.getDrawable("meta.button.primary.disabled")
		})
		skin.add(BUTTON_SECONDARY, Button.ButtonStyle().apply {
			up = skin.getDrawable("meta.button.up")
			over = skin.getDrawable("meta.button.over")
			down = skin.getDrawable("meta.button.down")
			focused = skin.getDrawable("meta.button.focus")
			disabled = skin.getDrawable("meta.button.disabled")
		})
		skin.add(BUTTON_TERTIARY, Button.ButtonStyle().apply {
			up = skin.getDrawable("meta.button.tertiary.up")
			over = skin.getDrawable("meta.button.tertiary.over")
			down = skin.getDrawable("meta.button.tertiary.down")
			focused = skin.getDrawable("meta.button.tertiary.focus")
			disabled = skin.getDrawable("meta.button.tertiary.disabled")
		})
		skin.add(SPINNER_DECREMENT, Button.ButtonStyle().apply {
			up = skin.getDrawable("meta.spinner.left.up")
			over = skin.getDrawable("meta.spinner.left.over")
			down = skin.getDrawable("meta.spinner.left.down")
			focused = skin.getDrawable("meta.spinner.left.over")
			disabled = skin.getDrawable("meta.imageButton.disabled")
		})
		skin.add(SPINNER_INCREMENT, Button.ButtonStyle().apply {
			up = skin.getDrawable("meta.spinner.right.up")
			over = skin.getDrawable("meta.spinner.right.over")
			down = skin.getDrawable("meta.spinner.right.down")
			focused = skin.getDrawable("meta.spinner.right.over")
			disabled = skin.getDrawable("meta.imageButton.disabled")
		})
		skin.add(BUTTON_TOGGLE, Button.ButtonStyle(skin.get(BUTTON, Button.ButtonStyle::class.java)).apply {
			up = skin.getDrawable("meta.button.up")
			checked = skin.getDrawable("meta.button.checked")
			checkedOver = skin.getDrawable("meta.button.checkedOver")
			checkedFocused = skin.getDrawable("meta.button.checkedFocus")
		})
		skin.add(ICON_BUTTON, Button.ButtonStyle(skin.get(BUTTON_SECONDARY, Button.ButtonStyle::class.java)))
		skin.add(IMAGE_BUTTON, Button.ButtonStyle().apply {
			up = skin.getDrawable("meta.imageButton.up")
			over = skin.getDrawable("meta.imageButton.over")
			checked = skin.getDrawable("meta.imageButton.checked")
			checkedOver = skin.getDrawable("meta.imageButton.checkedOver")
			checkedFocused = skin.getDrawable("meta.imageButton.checkedFocus")
			down = skin.getDrawable("meta.imageButton.down")
			disabled = skin.getDrawable("meta.imageButton.disabled")
		})
		skin.add(CHECKBOX, Button.ButtonStyle().apply {
			up = skin.getDrawable("meta.checkbox.off")
			over = skin.getDrawable("meta.checkbox.over")
			down = skin.getDrawable("meta.checkbox.down")
			checked = skin.getDrawable("meta.checkbox.on")
			checkedOver = skin.getDrawable("meta.checkbox.onOver")
			focused = skin.getDrawable("meta.checkbox.focus")
			checkedFocused = skin.getDrawable("meta.checkbox.onFocus")
			disabled = skin.getDrawable("meta.checkbox.disabled")
		})

		val baseWindow = if (skin.has("default", com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle::class.java)) {
			skin.get("default", com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle::class.java)
		} else {
			com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(defaultFont, MetaColor.TEXT.cpy(), null).also {
				skin.add("default", it, com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle::class.java)
			}
		}
		skin.add(WINDOW, com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(baseWindow).apply {
			background = skin.getDrawable(WINDOW_BACKGROUND)
		})
		skin.add(WINDOW_RESIZABLE, com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle(baseWindow).apply {
			background = skin.getDrawable(WINDOW_BACKGROUND)
		})

		val baseTextField = if (skin.has("default", TextField.TextFieldStyle::class.java)) {
			skin.get("default", TextField.TextFieldStyle::class.java)
		} else {
			TextField.TextFieldStyle(defaultFont, MetaColor.TEXT.cpy(), skin.getDrawable("meta.cursor"),
				skin.getDrawable("meta.selection"), skin.getDrawable("meta.field.up")).also {
				skin.add("default", it, TextField.TextFieldStyle::class.java)
			}
		}
		skin.add(TEXT_FIELD, TextField.TextFieldStyle(baseTextField).apply {
				background = skin.getDrawable("meta.field.up")
				focusedBackground = skin.getDrawable("meta.field.focus")
				disabledBackground = skin.getDrawable("meta.field.disabled")
				cursor = skin.getDrawable("meta.cursor")
				selection = skin.getDrawable("meta.selection")
				fontColor = MetaColor.TEXT.cpy()
				focusedFontColor = MetaColor.TEXT.cpy()
				disabledFontColor = MetaColor.TEXT_DISABLED.cpy()
				messageFontColor = MetaColor.TEXT_MUTED.cpy()
			})
		skin.add(TEXT_FIELD_ERROR, TextField.TextFieldStyle(baseTextField).apply {
				background = skin.getDrawable("meta.field.error")
				focusedBackground = skin.getDrawable("meta.field.error")
				disabledBackground = skin.getDrawable("meta.field.disabled")
				cursor = skin.getDrawable("meta.cursor")
				selection = skin.getDrawable("meta.selection")
				fontColor = MetaColor.TEXT.cpy()
				focusedFontColor = MetaColor.TEXT.cpy()
				disabledFontColor = MetaColor.TEXT_DISABLED.cpy()
				messageFontColor = MetaColor.TEXT_MUTED.cpy()
			})
		skin.add(TEXT_AREA, TextField.TextFieldStyle(skin.get(TEXT_FIELD, TextField.TextFieldStyle::class.java)))
		skin.add(SPINNER_TEXT_FIELD, TextField.TextFieldStyle(skin.get(TEXT_FIELD, TextField.TextFieldStyle::class.java)).apply {
			background = skin.getDrawable("meta.spinner.field.up")
			focusedBackground = skin.getDrawable("meta.spinner.field.focus")
			disabledBackground = skin.getDrawable("meta.spinner.field.disabled")
		})

		val defaultScroll = if (skin.has("default", ScrollPane.ScrollPaneStyle::class.java)) {
			ScrollPane.ScrollPaneStyle(skin.get(ScrollPane.ScrollPaneStyle::class.java))
		} else ScrollPane.ScrollPaneStyle().also { skin.add("default", it, ScrollPane.ScrollPaneStyle::class.java) }
			skin.add(SCROLL_PANE, ScrollPane.ScrollPaneStyle(defaultScroll).apply {
				background = skin.getDrawable("meta.panel")
				vScroll = skin.getDrawable("meta.scroll.track")
				hScroll = skin.getDrawable("meta.scroll.track")
				vScrollKnob = skin.getDrawable("meta.scroll.knob")
				hScrollKnob = skin.getDrawable("meta.scroll.knob")
			})
			skin.add(SCROLL_PANE_FLAT, ScrollPane.ScrollPaneStyle(defaultScroll).apply {
				background = null
				vScroll = skin.getDrawable("meta.scroll.track")
				hScroll = skin.getDrawable("meta.scroll.track")
				vScrollKnob = skin.getDrawable("meta.scroll.knob")
				hScrollKnob = skin.getDrawable("meta.scroll.knob")
			})

		val baseList = if (skin.has("default", List.ListStyle::class.java)) skin.get(List.ListStyle::class.java) else
			List.ListStyle(defaultFont, MetaColor.TEXT.cpy(), MetaColor.TEXT.cpy(), skin.getDrawable("meta.dropdown.selection")).also {
				skin.add("default", it, List.ListStyle::class.java)
			}
		val baseSelect = if (skin.has("default", SelectBox.SelectBoxStyle::class.java)) skin.get(SelectBox.SelectBoxStyle::class.java) else
			SelectBox.SelectBoxStyle(defaultFont, MetaColor.TEXT.cpy(), skin.getDrawable("meta.field.up"), defaultScroll, baseList).also {
				skin.add("default", it, SelectBox.SelectBoxStyle::class.java)
			}
			val base = baseSelect
			skin.add(SELECT_BOX, SelectBox.SelectBoxStyle(base).apply {
				background = skin.getDrawable("meta.field.up")
				backgroundOver = skin.getDrawable("meta.field.over")
				backgroundOpen = skin.getDrawable("meta.field.focus")
				backgroundDisabled = skin.getDrawable("meta.field.disabled")
				fontColor = MetaColor.TEXT.cpy()
				overFontColor = MetaColor.TEXT.cpy()
				disabledFontColor = MetaColor.TEXT_DISABLED.cpy()
				listStyle = List.ListStyle(base.listStyle).apply {
					background = null
					selection = skin.getDrawable("meta.dropdown.selection")
					over = skin.getDrawable("meta.dropdown.over")
					down = skin.getDrawable("meta.dropdown.selection")
					fontColorSelected = MetaColor.TEXT.cpy()
					fontColorUnselected = MetaColor.TEXT.cpy()
				}
				scrollStyle = if (skin.has(SCROLL_PANE_FLAT, ScrollPane.ScrollPaneStyle::class.java)) {
					ScrollPane.ScrollPaneStyle(skin.get(SCROLL_PANE_FLAT, ScrollPane.ScrollPaneStyle::class.java)).apply {
						background = skin.getDrawable(DROPDOWN)
					}
				} else {
					base.scrollStyle
				}
			})

		val baseSlider = if (skin.has("default-horizontal", Slider.SliderStyle::class.java)) skin.get("default-horizontal", Slider.SliderStyle::class.java)
		else Slider.SliderStyle(skin.getDrawable("meta.slider.track"), skin.getDrawable("meta.slider.knob")).also {
			skin.add("default-horizontal", it, Slider.SliderStyle::class.java)
		}
			skin.add(SLIDER_HORIZONTAL, Slider.SliderStyle(baseSlider).apply {
				background = skin.getDrawable("meta.slider.track")
				knobBefore = skin.getDrawable("meta.slider.fill")
				knob = skin.getDrawable("meta.slider.knob")
				knobOver = skin.getDrawable("meta.slider.knob.over")
				knobDown = skin.getDrawable("meta.slider.knob.down")
			})
		val baseProgress = if (skin.has("default-horizontal", ProgressBar.ProgressBarStyle::class.java)) skin.get("default-horizontal", ProgressBar.ProgressBarStyle::class.java)
		else ProgressBar.ProgressBarStyle(skin.getDrawable("meta.slider.track"), skin.getDrawable("meta.slider.fill")).also {
			skin.add("default-horizontal", it, ProgressBar.ProgressBarStyle::class.java)
		}
			skin.add(PROGRESS_HORIZONTAL, ProgressBar.ProgressBarStyle(baseProgress).apply {
				background = skin.getDrawable("meta.slider.track")
				knob = skin.getDrawable("meta.slider.fill")
				knobBefore = skin.getDrawable("meta.slider.fill")
			})
	}

	private fun rounded(
		skin: Skin,
		name: String,
		fill: Color,
		stroke: Color?,
		radius: Int,
		border: Int,
		padding: Float,
		minWidth: Float = 24f,
		minHeight: Float = 24f,
	) {
		val pixmap = Pixmap(PATCH_SIZE, PATCH_SIZE, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		val innerRadius = (radius - border).coerceAtLeast(0)
		for (y in 0 until PATCH_SIZE) {
			for (x in 0 until PATCH_SIZE) {
				val pixel = sampleShapePixel(
					x,
					y,
					fill,
					stroke,
					outer = { sx, sy -> insideRoundRect(sx, sy, PATCH_SIZE.toFloat(), PATCH_SIZE.toFloat(), radius.toFloat()) },
					inner = { sx, sy ->
						border <= 0 || insideRoundRect(
							sx - border,
							sy - border,
							(PATCH_SIZE - border * 2).toFloat(),
							(PATCH_SIZE - border * 2).toFloat(),
							innerRadius.toFloat(),
						)
					},
				)
				if (pixel.a > 0f) pixmap.drawPixel(x, y, Color.rgba8888(pixel))
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
			this.minWidth = minWidth
			this.minHeight = minHeight
		}
		skin.add(name, drawable, Drawable::class.java)
	}

	private fun segmentedRounded(
		skin: Skin,
		name: String,
		fill: Color,
		stroke: Color,
		roundLeft: Boolean = false,
		roundRight: Boolean = false,
	) {
		val pixmap = Pixmap(PATCH_SIZE, PATCH_SIZE, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		val radius = 6f
		val border = 1f
		for (y in 0 until PATCH_SIZE) {
			for (x in 0 until PATCH_SIZE) {
				val pixel = sampleShapePixel(
					x,
					y,
					fill,
					stroke,
					outer = { sx, sy -> insideSegmentRect(sx, sy, PATCH_SIZE.toFloat(), PATCH_SIZE.toFloat(), radius, roundLeft, roundRight) },
					inner = { sx, sy -> insideSegmentRect(
						sx - border,
						sy - border,
						PATCH_SIZE - border * 2f,
						PATCH_SIZE - border * 2f,
						radius - border,
						roundLeft,
						roundRight,
					) },
				)
				if (pixel.a > 0f) pixmap.drawPixel(x, y, Color.rgba8888(pixel))
			}
		}
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		val split = radius.toInt() + 2
		skin.add(name, NinePatchDrawable(NinePatch(texture, split, split, split, split)).apply {
			leftWidth = 8f
			rightWidth = 8f
			topHeight = 8f
			bottomHeight = 8f
			minWidth = 24f
			minHeight = 24f
		}, Drawable::class.java)
	}

	/** Angular window frame used as Meta's restrained signature silhouette. */
	private fun chamfered(
		skin: Skin,
		name: String,
		fill: Color,
		stroke: Color,
		cut: Int,
		border: Int,
		padding: Float,
	) {
		val pixmap = Pixmap(PATCH_SIZE, PATCH_SIZE, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		for (y in 0 until PATCH_SIZE) {
			for (x in 0 until PATCH_SIZE) {
				val pixel = sampleShapePixel(
					x,
					y,
					fill,
					stroke,
					outer = { sx, sy -> insideChamferedRect(sx, sy, PATCH_SIZE.toFloat(), PATCH_SIZE.toFloat(), cut.toFloat()) },
					inner = { sx, sy ->
						insideChamferedRect(
							sx - border,
							sy - border,
							(PATCH_SIZE - border * 2).toFloat(),
							(PATCH_SIZE - border * 2).toFloat(),
							(cut - border).coerceAtLeast(0).toFloat(),
						)
					},
				)
				if (pixel.a > 0f) pixmap.drawPixel(x, y, Color.rgba8888(pixel))
			}
		}
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		val split = cut + 2
		skin.add(name, NinePatchDrawable(NinePatch(texture, split, split, split, split)).apply {
			leftWidth = padding
			rightWidth = padding
			topHeight = padding
			bottomHeight = padding
			minWidth = 32f
			minHeight = 32f
		}, Drawable::class.java)
	}

	private fun configureToolbarDrawable(drawable: Drawable) {
		drawable.leftWidth = 8f
		drawable.rightWidth = 8f
		drawable.topHeight = 2f
		drawable.bottomHeight = 2f
		drawable.minWidth = 24f
		drawable.minHeight = 24f
	}

	private fun configureMenuItemDrawable(drawable: Drawable) {
		drawable.leftWidth = 8f
		drawable.rightWidth = 8f
		drawable.topHeight = 4f
		drawable.bottomHeight = 4f
		drawable.minWidth = 28f
		drawable.minHeight = 28f
	}

	private fun solid(skin: Skin, name: String, color: Color, minWidth: Float, minHeight: Float) {
		val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		pixmap.drawPixel(0, 0, Color.rgba8888(color))
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		skin.add(name, TextureRegionDrawable(TextureRegion(texture)).apply {
			this.minWidth = minWidth
			this.minHeight = minHeight
		}, Drawable::class.java)
	}

	internal fun focusedButtonStyle(base: Button.ButtonStyle): Button.ButtonStyle {
		return Button.ButtonStyle(base).apply {
			val focus = base.focused ?: skin().getDrawable("meta.button.focus")
			up = focus
			over = focus
			focused = focus
			if (base.checked != null || base.checkedOver != null || base.checkedFocused != null) {
				checked = skin().getDrawable("meta.button.checkedFocus")
				checkedOver = skin().getDrawable("meta.button.checkedFocus")
				checkedFocused = skin().getDrawable("meta.button.checkedFocus")
			}
		}
	}

	internal fun focusedImageButtonStyle(base: Button.ButtonStyle): Button.ButtonStyle {
		val skin = skin()
		return Button.ButtonStyle(base).apply {
			up = skin.getDrawable("meta.imageButton.focus")
			over = skin.getDrawable("meta.imageButton.focus")
			focused = skin.getDrawable("meta.imageButton.focus")
			down = skin.getDrawable("meta.imageButton.down")
			disabled = skin.getDrawable("meta.imageButton.disabled")
			if (base.checked != null || base.checkedOver != null || base.checkedFocused != null) {
				checked = skin.getDrawable("meta.imageButton.checkedFocus")
				checkedOver = skin.getDrawable("meta.imageButton.checkedFocus")
				checkedFocused = skin.getDrawable("meta.imageButton.checkedFocus")
			}
		}
	}

	// Selection is a persistent state, not the keyboard cursor: selected controls wear the checked (selection-fill)
	// look, keeping the bright 2px focus ring unambiguous for keyboard/controller navigation.
	internal fun selectedButtonStyle(base: Button.ButtonStyle): Button.ButtonStyle {
		val skin = skin()
		return Button.ButtonStyle(base).apply {
			val selected = skin.getDrawable("meta.button.checked")
			val selectedOver = skin.getDrawable("meta.button.checkedOver")
			val selectedFocus = skin.getDrawable("meta.button.checkedFocus")
			up = selected
			over = selectedOver
			down = selected
			checked = selected
			checkedOver = selectedOver
			checkedFocused = selectedFocus
			focused = selectedFocus
		}
	}

	internal fun selectedImageButtonStyle(base: Button.ButtonStyle): Button.ButtonStyle {
		val skin = skin()
		return Button.ButtonStyle(base).apply {
			val selected = skin.getDrawable("meta.imageButton.checked")
			val selectedOver = skin.getDrawable("meta.imageButton.checkedOver")
			val selectedFocus = skin.getDrawable("meta.imageButton.checkedFocus")
			up = selected
			over = selectedOver
			down = skin.getDrawable("meta.imageButton.down")
			checked = selected
			checkedOver = selectedOver
			checkedFocused = selectedFocus
			focused = selectedFocus
		}
	}

	internal fun focusedCheckboxStyle(base: Button.ButtonStyle): Button.ButtonStyle {
		val skin = skin()
		return Button.ButtonStyle(base).apply {
			up = skin.getDrawable("meta.checkbox.focus")
			over = skin.getDrawable("meta.checkbox.focus")
			focused = skin.getDrawable("meta.checkbox.focus")
			checked = skin.getDrawable("meta.checkbox.onFocus")
			checkedOver = skin.getDrawable("meta.checkbox.onFocus")
			checkedFocused = skin.getDrawable("meta.checkbox.onFocus")
		}
	}

	internal fun focusedSelectBoxStyle(base: SelectBox.SelectBoxStyle): SelectBox.SelectBoxStyle {
		val skin = skin()
		return SelectBox.SelectBoxStyle(base).apply {
			background = skin.getDrawable("meta.field.focus")
			backgroundOver = skin.getDrawable("meta.field.focus")
			backgroundOpen = skin.getDrawable("meta.field.focus")
		}
	}

	internal fun focusedTextFieldStyle(base: TextField.TextFieldStyle): TextField.TextFieldStyle {
		return TextField.TextFieldStyle(base).apply {
			val focus = base.focusedBackground ?: skin().getDrawable("meta.field.focus")
			background = focus
			focusedBackground = focus
		}
	}

	private fun topRounded(
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
		val innerRadius = (radius - border).coerceAtLeast(0)
		for (y in 0 until PATCH_SIZE) {
			for (x in 0 until PATCH_SIZE) {
				val pixel = sampleShapePixel(
					x,
					y,
					fill,
					stroke,
					outer = { sx, sy -> insideTopRoundRect(sx, sy, PATCH_SIZE.toFloat(), PATCH_SIZE.toFloat(), radius.toFloat()) },
					inner = { sx, sy ->
						border <= 0 || insideTopRoundRect(
							sx - border,
							sy - border,
							(PATCH_SIZE - border * 2).toFloat(),
							(PATCH_SIZE - border * 2).toFloat(),
							innerRadius.toFloat(),
						)
					},
				)
				if (pixel.a > 0f) pixmap.drawPixel(x, y, Color.rgba8888(pixel))
			}
		}
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		val split = radius + 2
		val drawable = NinePatchDrawable(NinePatch(texture, split, split, split, 0)).apply {
			leftWidth = padding
			rightWidth = padding
			topHeight = padding
			bottomHeight = padding * 0.5f
			minWidth = 28f
			minHeight = 30f
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
		focused: Boolean = false,
	) {
		val scale = ICON_PIXMAP_SCALE
		val pixmap = Pixmap(ICON_PIXMAP_SIZE, ICON_PIXMAP_SIZE, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		val fill = when {
			disabled -> Color.valueOf("292A2EFF")
			down -> MetaColor.FILL_PRESSED
			over -> MetaColor.TERTIARY_HOVER
			else -> MetaColor.FILL_SUNKEN
		}
		val stroke = when {
			disabled -> MetaColor.BORDER_DISABLED
			focused -> MetaColor.ACCENT_BRIGHT
			checked -> MetaColor.ACCENT
			over -> MetaColor.BORDER_STRONG
			else -> MetaColor.BORDER_STRONG
		}
		drawRoundedPixels(pixmap, fill, stroke, radius = 5 * scale, border = 2 * scale)
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		skin.add(name, TextureRegionDrawable(TextureRegion(texture)).apply {
			minWidth = ICON_SIZE.toFloat()
			minHeight = ICON_SIZE.toFloat()
		}, Drawable::class.java)
	}

	/** Supersampled solid circle rendered at [ICON_PIXMAP_SCALE], for knobs that must never nine-patch-stretch. */
	private fun circle(skin: Skin, name: String, fill: Color, stroke: Color, border: Int, diameter: Int) {
		val size = diameter * ICON_PIXMAP_SCALE
		val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
		pixmap.setBlending(Pixmap.Blending.None)
		drawRoundedPixels(pixmap, fill, stroke, radius = size / 2, border = border * ICON_PIXMAP_SCALE)
		val texture = Texture(pixmap)
		pixmap.dispose()
		texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		skin.add("$name.texture", texture)
		skin.add(name, TextureRegionDrawable(TextureRegion(texture)).apply {
			minWidth = diameter.toFloat()
			minHeight = diameter.toFloat()
		}, Drawable::class.java)
	}

	private fun drawRoundedPixels(pixmap: Pixmap, fill: Color, stroke: Color, radius: Int, border: Int) {
		for (y in 0 until pixmap.height) {
			for (x in 0 until pixmap.width) {
				val pixel = sampleShapePixel(
					x,
					y,
					fill,
					stroke,
					outer = { sx, sy -> insideRoundRect(sx, sy, pixmap.width.toFloat(), pixmap.height.toFloat(), radius.toFloat()) },
					inner = { sx, sy ->
						insideRoundRect(
							sx - border,
							sy - border,
							(pixmap.width - border * 2).toFloat(),
							(pixmap.height - border * 2).toFloat(),
							(radius - border).coerceAtLeast(0).toFloat(),
						)
					},
				)
				if (pixel.a > 0f) pixmap.drawPixel(x, y, Color.rgba8888(pixel))
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

	private fun insideChamferedRect(px: Float, py: Float, width: Float, height: Float, cut: Float): Boolean {
		if (px < 0f || py < 0f || px > width || py > height) return false
		val c = cut.coerceAtMost(width * 0.5f).coerceAtMost(height * 0.5f)
		return px + py >= c &&
			(width - px) + py >= c &&
			px + (height - py) >= c &&
			(width - px) + (height - py) >= c
	}

	private fun insideSegmentRect(
		px: Float,
		py: Float,
		width: Float,
		height: Float,
		radius: Float,
		roundLeft: Boolean,
		roundRight: Boolean,
	): Boolean {
		if (px < 0f || py < 0f || px > width || py > height) return false
		if (!roundLeft && px <= radius) return true
		if (!roundRight && px >= width - radius) return true
		return insideRoundRect(px, py, width, height, radius)
	}

	private inline fun sampleShapePixel(
		x: Int,
		y: Int,
		fill: Color,
		stroke: Color?,
		outer: (Float, Float) -> Boolean,
		inner: (Float, Float) -> Boolean,
	): Color {
		var fillSamples = 0
		var strokeSamples = 0
		for (sy in 0 until SHAPE_AA_SAMPLES) {
			for (sx in 0 until SHAPE_AA_SAMPLES) {
				val sampleX = x + (sx + 0.5f) / SHAPE_AA_SAMPLES
				val sampleY = y + (sy + 0.5f) / SHAPE_AA_SAMPLES
				if (outer(sampleX, sampleY)) {
					if (inner(sampleX, sampleY) || stroke == null) fillSamples++ else strokeSamples++
				}
			}
		}
		val total = SHAPE_AA_SAMPLES * SHAPE_AA_SAMPLES
		val covered = fillSamples + strokeSamples
		if (covered == 0) return TRANSPARENT
		if (strokeSamples == 0) return TMP_COLOR.set(fill.r, fill.g, fill.b, fill.a * covered / total)
		if (fillSamples == 0) return TMP_COLOR.set(stroke!!.r, stroke.g, stroke.b, stroke.a * covered / total)

		val fillWeight = fillSamples.toFloat() / covered
		val strokeWeight = strokeSamples.toFloat() / covered
		return TMP_COLOR.set(
			fill.r * fillWeight + stroke!!.r * strokeWeight,
			fill.g * fillWeight + stroke.g * strokeWeight,
			fill.b * fillWeight + stroke.b * strokeWeight,
			(fill.a * fillSamples + stroke.a * strokeSamples) / total,
		)
	}

	private fun insideTopRoundRect(px: Float, py: Float, width: Float, height: Float, radius: Float): Boolean {
		if (width <= 0f || height <= 0f) return false
		val r = radius.coerceAtMost(width * 0.5f).coerceAtMost(height)
		if (py >= r) return px >= 0f && px <= width && py <= height
		val cx = px.coerceIn(r, width - r)
		val cy = r
		val dx = px - cx
		val dy = py - cy
		return dx * dx + dy * dy <= r * r
	}

	private val TRANSPARENT = Color(0f, 0f, 0f, 0f)
	private val TMP_COLOR = Color()
}
