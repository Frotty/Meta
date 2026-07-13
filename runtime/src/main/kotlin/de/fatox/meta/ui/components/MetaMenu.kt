package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.kotcrab.vis.ui.widget.Menu
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.MenuItem
import com.kotcrab.vis.ui.widget.VisTextButton
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.FontGenerationTracker
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType

/** Meta chrome around VisUI's menu coordination logic. All visible text uses the runtime TTF provider. */
class MetaMenuBar : MenuBar() {
	init {
		table.background = MetaSkin.skin().getDrawable("meta.menu.bar")
		table.pad(MetaSpacing.XS, MetaSpacing.SM, MetaSpacing.XS, MetaSpacing.SM)
		table.defaults().padRight(MetaSpacing.XS)
	}
}

class MetaMenu private constructor(
	title: String,
	private val fontSize: Int,
	private val metaStyle: MenuStyle,
) : Menu(title, metaStyle), FontRefreshable {
	private val fontProvider: FontProvider = inject()
	private val fontTracker = FontGenerationTracker()
	private var lastItemCell: Cell<MenuItem>? = null

	@JvmOverloads
	constructor(title: String, fontSize: Int = MetaType.BODY) : this(title, fontSize, createMenuStyle(fontSize))

	override fun addItem(item: MenuItem) {
		lastItemCell?.padBottom(0f)
		super.addItem(item)
		// PopupMenu draws rounded corners but does not clip its rectangular final row. Keep normal rows full-width and
		// lift only the current last row out of the bottom-corner curve. Adding another item removes this old inset.
		lastItemCell = getCell(item)?.padBottom(POPUP_BOTTOM_INSET)
		pack()
	}

	override fun addSeparator() {
		lastItemCell?.padBottom(0f)
		lastItemCell = null
		add(MetaSeparator()).height(1f).fillX().expandX()
			.padLeft(SEPARATOR_HORIZONTAL_INSET).padRight(SEPARATOR_HORIZONTAL_INSET)
			.padTop(MetaSpacing.XXS).padBottom(MetaSpacing.XXS).row()
		pack()
	}

	override fun refreshFont() {
		fontTracker.markFresh()
		metaStyle.openButtonStyle = VisTextButton.VisTextButtonStyle(metaStyle.openButtonStyle).apply {
			font = fontProvider.getFont(fontSize, FontType.REGULAR)
		}
		openButton.style = metaStyle.openButtonStyle
		invalidateHierarchy()
	}

	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) fontTracker.refreshIfStale(this)
	}

	private companion object {
		const val POPUP_BOTTOM_INSET = 4f
		const val SEPARATOR_HORIZONTAL_INSET = 3f
	}
}

class MetaMenuItem private constructor(
	text: String,
	icon: Drawable?,
	private val fontSize: Int,
	private var metaStyle: MenuItemStyle,
) : MenuItem(text, icon, metaStyle), FontRefreshable {
	private val fontProvider: FontProvider = inject()
	private val fontTracker = FontGenerationTracker()
	val disabledValue: Signal<Boolean> = signal(isDisabled)
	private val disabledBinding = disabledValue.subscribe { applyDisabledValue() }

	@JvmOverloads
	constructor(text: String, fontSize: Int = MetaType.BODY) : this(text, null, fontSize, createMenuItemStyle(fontSize))

	@JvmOverloads
	constructor(text: String, icon: String, size: Int = 18, fontSize: Int = MetaType.BODY) : this(
		text,
		MetaIconDrawable(icon, size),
		fontSize,
		createMenuItemStyle(fontSize),
	)

	override fun setDisabled(isDisabled: Boolean) {
		super.setDisabled(isDisabled)
		disabledValue.value = isDisabled
	}

	private fun applyDisabledValue() {
		val next = disabledValue.peek()
		if (isDisabled != next) super.setDisabled(next)
	}

	override fun refreshFont() {
		fontTracker.markFresh()
		metaStyle = MenuItemStyle(metaStyle).apply {
			font = fontProvider.getFont(fontSize, FontType.REGULAR)
		}
		setStyle(metaStyle)
		invalidateHierarchy()
	}

	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) fontTracker.refreshIfStale(this)
	}
}

private fun createMenuStyle(fontSize: Int): Menu.MenuStyle {
	val skin = MetaSkin.skin()
	val base = skin.get(Menu.MenuStyle::class.java)
	return Menu.MenuStyle(base).apply {
		background = skin.getDrawable(MetaSkin.DROPDOWN)
		border = null
		openButtonStyle = VisTextButton.VisTextButtonStyle(base.openButtonStyle).apply {
			font = inject<FontProvider>().getFont(fontSize, FontType.REGULAR)
			fontColor = MetaColor.TEXT.cpy()
			overFontColor = MetaColor.TEXT.cpy()
			downFontColor = MetaColor.TEXT.cpy()
			up = skin.getDrawable("meta.menu.bar.open")
			over = skin.getDrawable("meta.menu.bar.over")
			down = skin.getDrawable("meta.menu.bar.selected")
			checked = skin.getDrawable("meta.menu.bar.selected")
		}
	}
}

private fun createMenuItemStyle(fontSize: Int): MenuItem.MenuItemStyle {
	val skin = MetaSkin.skin()
	val base = skin.get(MenuItem.MenuItemStyle::class.java)
	return MenuItem.MenuItemStyle(base).apply {
		font = inject<FontProvider>().getFont(fontSize, FontType.REGULAR)
		fontColor = MetaColor.TEXT.cpy()
		overFontColor = MetaColor.TEXT.cpy()
		downFontColor = MetaColor.TEXT.cpy()
		disabledFontColor = MetaColor.TEXT_DISABLED.cpy()
		up = skin.getDrawable("meta.menu.item")
		over = skin.getDrawable("meta.menu.item.over")
		down = skin.getDrawable("meta.menu.item.selected")
		checked = skin.getDrawable("meta.menu.item.selected")
		disabled = skin.getDrawable("meta.menu.item")
	}
}
