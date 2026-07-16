package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType

/** Scene2d-native menu coordinator. Exactly one menu owns the active state. */
class MetaMenuBar {
	val table: Table = MetaTable().apply {
		background = MetaSkin.skin().getDrawable("meta.menu.bar")
		pad(MetaSpacing.XS, MetaSpacing.SM, MetaSpacing.XS, MetaSpacing.SM)
		defaults().padRight(MetaSpacing.XS)
		left()
	}
	val activeMenuValue: Signal<MetaMenu?> = signal(null)
	private val menus = Array<MetaMenu>()

	fun addMenu(menu: MetaMenu) {
		if (menus.contains(menu, true)) return
		menus.add(menu)
		menu.attach(this)
		table.add(menu.openButton)
	}

	fun removeMenu(menu: MetaMenu): Boolean {
		if (!menus.removeValue(menu, true)) return false
		if (activeMenuValue.peek() === menu) closeMenu()
		table.removeActor(menu.openButton)
		menu.attach(null)
		return true
	}

	fun closeMenu() {
		setActiveMenu(null)
	}

	internal fun toggle(menu: MetaMenu) {
		setActiveMenu(if (activeMenuValue.peek() === menu) null else menu)
	}

	internal fun switchOnHover(menu: MetaMenu) {
		if (activeMenuValue.peek() != null && activeMenuValue.peek() !== menu) setActiveMenu(menu)
	}

	private fun setActiveMenu(menu: MetaMenu?) {
		val previous = activeMenuValue.peek()
		if (previous === menu) return
		previous?.hidePopup()
		activeMenuValue.value = menu
		menu?.showBelowButton()
	}
}

/** Popup menu usable both from a [MetaMenuBar] and as a context menu through [showMenu]. */
class MetaMenu @JvmOverloads constructor(
	val title: String,
	fontSize: Int = MetaType.BODY,
) : MetaTable() {
	internal val openButton = MenuHeaderButton(title, fontSize).apply {
		addListener(object : InputListener() {
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
				if (pointer != 0 || button != 0) return false
				menuBar?.toggle(this@MetaMenu)
				event.stop()
				return true
			}

			override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
				if (pointer == -1) menuBar?.switchOnHover(this@MetaMenu)
			}
		})
	}
	val openValue: Signal<Boolean> = signal(false)
	private var menuBar: MetaMenuBar? = null
	private var shownStage: Stage? = null
	private val anchorPosition = Vector2()
	private val outsideListener = object : InputListener() {
		override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
			if (!event.target.isDescendantOf(this@MetaMenu) && !event.target.isDescendantOf(openButton)) {
				menuBar?.closeMenu() ?: hidePopup()
			}
			return false
		}
	}

	init {
		background = MetaSkin.skin().getDrawable(MetaSkin.DROPDOWN)
		pad(MetaSpacing.XS, 0f, MetaSpacing.XS, 0f)
		defaults().growX()
		touchable = Touchable.enabled
	}

	fun addItem(item: MetaMenuItem) {
		add(item).growX().row()
		item.owner = this
		pack()
	}

	fun addSeparator() {
		add(MetaSeparator()).height(1f).fillX()
			.padLeft(SEPARATOR_INSET).padRight(SEPARATOR_INSET)
			.padTop(MetaSpacing.XXS).padBottom(MetaSpacing.XXS).row()
		pack()
	}

	fun showMenu(stage: Stage, x: Float, y: Float) {
		showPopup(stage, x, y)
	}

	internal fun attach(bar: MetaMenuBar?) {
		menuBar = bar
		if (bar == null) hidePopup()
	}

	internal fun showBelowButton() {
		val stage = openButton.stage ?: return
		openButton.localToStageCoordinates(anchorPosition.set(0f, 0f))
		showPopup(stage, anchorPosition.x, anchorPosition.y)
	}

	private fun showPopup(stage: Stage, anchorX: Float, anchorY: Float) {
		// Repositioning this menu must not clear the bar's active-menu ownership. Losing that ownership here meant
		// every subsequent header click opened another popup without closing the previous one.
		clearPopupPresentation()
		super.remove()
		pack()
		val popupX = anchorX.coerceIn(0f, (stage.width - width).coerceAtLeast(0f))
		val popupY = (anchorY - height).coerceIn(0f, (stage.height - height).coerceAtLeast(0f))
		setPosition(popupX, popupY)
		shownStage = stage
		stage.addActor(this)
		stage.root.addCaptureListener(outsideListener)
		openValue.value = true
		openButton.active = true
		toFront()
	}

	internal fun hidePopup() {
		clearPopupState()
		super.remove()
	}

	override fun remove(): Boolean {
		clearPopupState()
		return super.remove()
	}

	private fun clearPopupState() {
		clearPopupPresentation()
		val bar = menuBar
		if (bar?.activeMenuValue?.peek() === this) bar.activeMenuValue.value = null
	}

	private fun clearPopupPresentation() {
		shownStage?.root?.removeCaptureListener(outsideListener)
		shownStage = null
		openValue.value = false
		openButton.active = false
	}

	internal fun closeFromItem() {
		menuBar?.closeMenu() ?: hidePopup()
	}

	private fun Actor?.isDescendantOf(ancestor: Actor): Boolean {
		var current = this
		while (current != null) {
			if (current === ancestor) return true
			current = current.parent
		}
		return false
	}

	private companion object {
		const val SEPARATOR_INSET = 3f
	}
}

/** TTF menu entry with optional Remix icon and reactive disabled state. */
class MetaMenuItem private constructor(
	text: String,
	icon: Drawable?,
	fontSize: Int,
) : Button(menuItemStyle()) {
	val disabledValue: Signal<Boolean> = signal(false)
	@Suppress("unused")
	private val disabledBinding = disabledValue.subscribe { setDisabled(disabledValue.peek()) }
	internal var owner: MetaMenu? = null
	private val label = MetaLabel(text, fontSize, MetaColor.TEXT).apply { setAlignment(Align.left) }

	@JvmOverloads
	constructor(text: String, fontSize: Int = MetaType.BODY) : this(text, null, fontSize)

	@JvmOverloads
	constructor(text: String, icon: String, size: Int = 18, fontSize: Int = MetaType.BODY) :
		this(text, MetaIconDrawable(icon, size), fontSize)

	init {
		left()
		if (icon != null) add(Image(icon)).size(icon.minWidth, icon.minHeight).padLeft(MetaSpacing.SM).padRight(MetaSpacing.XS)
		else add().width(MetaSpacing.SM)
		add(label).left().growX().padRight(MetaSpacing.SM)
		cursorPointer()
		addListener(object : ClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				if (!isDisabled) owner?.closeFromItem()
			}
		})
	}

	override fun setDisabled(disabled: Boolean) {
		super.setDisabled(disabled)
		touchable = if (disabled) Touchable.disabled else Touchable.enabled
		disabledValue.value = disabled
		label.color.set(if (disabled) MetaColor.TEXT_DISABLED else MetaColor.TEXT)
	}

	private companion object {
		fun menuItemStyle(): ButtonStyle {
			val skin = MetaSkin.skin()
			return ButtonStyle().apply {
				up = skin.getDrawable("meta.menu.item")
				over = skin.getDrawable("meta.menu.item.over")
				down = skin.getDrawable("meta.menu.item.selected")
				disabled = skin.getDrawable("meta.menu.item")
			}
		}
	}
}

internal class MenuHeaderButton(text: String, fontSize: Int) : MetaTextButton(text, fontSize) {
	private val normalStyle = headerStyle(false)
	private val activeStyle = headerStyle(true)

	var active: Boolean = false
		set(value) {
			if (field == value) return
			field = value
			installMetaStyle(if (value) activeStyle else normalStyle)
		}

	init {
		pad(0f, MetaSpacing.SM, 0f, MetaSpacing.SM)
		installMetaStyle(normalStyle)
	}

	private companion object {
		fun headerStyle(active: Boolean): Button.ButtonStyle {
			val skin = MetaSkin.skin()
			return Button.ButtonStyle().apply {
				up = skin.getDrawable(if (active) "meta.menu.bar.selected" else "meta.menu.bar.open")
				over = skin.getDrawable(if (active) "meta.menu.bar.selected" else "meta.menu.bar.over")
				down = skin.getDrawable("meta.menu.bar.selected")
			}
		}
	}
}
