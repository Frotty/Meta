package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.UiControlHelper

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaSelectBox<T>(fontSize: Int = 22) : SelectBox<T>(MetaSkin.skin()), MetaFocusable {

	private val uiControlHelper: UiControlHelper = inject()
	private var wasHelperActive = false
	private val fontProvider: FontProvider = inject()
	private val focusStyle: MetaSelectBoxFocusStyle<T>
	val selectedValue: Signal<T?> = signal(selected)

	init {
		val skin = MetaSkin.skin()
		if (skin.has(MetaSkin.SELECT_BOX, SelectBoxStyle::class.java)) {
			style = SelectBoxStyle(skin.get(MetaSkin.SELECT_BOX, SelectBoxStyle::class.java))
			list.style = List.ListStyle(style.listStyle)
		}
		val font = fontProvider.getFont(fontSize, FontType.REGULAR)
		style.font = font
		list.style.font = font
		focusStyle = MetaSelectBoxFocusStyle(this, style, MetaSkin::focusedSelectBoxStyle)
		focusStyle.install(style)
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				selectedValue.value = selected
			}
		})
	}

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	override fun showScrollPane() {
		super.showScrollPane()

		if (uiControlHelper.activated) {
			wasHelperActive = true
			uiControlHelper.activated = false
		}
	}

	override fun onHide(scrollPane: Actor?) {
		super.onHide(scrollPane)

		if (wasHelperActive) {
			uiControlHelper.activated = true
			wasHelperActive = false
		}
	}

	override fun setSelected(item: T?) {
		super.setSelected(item)
		selectedValue.value = selected
	}

	override fun setSelectedIndex(index: Int) {
		super.setSelectedIndex(index)
		selectedValue.value = selected
	}
}
