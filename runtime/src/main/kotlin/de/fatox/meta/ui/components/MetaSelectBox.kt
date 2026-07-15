package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.api.graphics.physicalPixelsPerUnit
import de.fatox.meta.api.graphics.snapToPhysicalPixel
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.injection.MetaInject.Companion.inject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.FontGenerationTracker
import de.fatox.meta.ui.FontRefreshable
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaType
import de.fatox.meta.ui.UiControlHelper

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaSelectBox<T>(private val fontSize: Int = MetaType.BODY) : SelectBox<T>(MetaSkin.skin()), MetaFocusable, FontRefreshable {

	private val uiControlHelper: UiControlHelper = inject()
	private var wasHelperActive = false
	private val fontProvider: FontProvider = inject()
	private val fontTracker = FontGenerationTracker()
	private val focusStyle: MetaSelectBoxFocusStyle<T>
	val selectedValue: Signal<T?> = signal(selected)
	val dropdownOpenValue: Signal<Boolean> = signal(false)
	private val chevronDown = MetaIconDrawable("ri-arrow-down-s-line", CHEVRON_SIZE, MetaColor.TEXT_MUTED.cpy())
	private val chevronUp = MetaIconDrawable("ri-arrow-up-s-line", CHEVRON_SIZE, MetaColor.TEXT_MUTED.cpy())

	init {
		val skin = MetaSkin.skin()
		// Clone before mutating - never write into the shared skin style (see MetaTextField for the pattern).
		val baseStyle = if (skin.has(MetaSkin.SELECT_BOX, SelectBoxStyle::class.java)) {
			skin.get(MetaSkin.SELECT_BOX, SelectBoxStyle::class.java)
		} else {
			style
		}
		style = SelectBoxStyle(baseStyle)
		list.style = List.ListStyle(style.listStyle)
		val font = fontProvider.getFont(fontSize, FontType.REGULAR)
		style.font = font
		// Set the font on style.listStyle (not just the list's current clone): focusStyle re-clones list.style from
		// style.listStyle on every apply(), so the dropdown would otherwise fall back to the skin's baked font.
		style.listStyle.font = font
		list.style.font = font
		focusStyle = MetaSelectBoxFocusStyle(this, style, MetaSkin::focusedSelectBoxStyle)
		focusStyle.install(style)
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				selectedValue.value = selected
			}
		})
		cursorPointer()
	}

	override fun drawItem(batch: Batch, font: BitmapFont, item: T?, x: Float, y: Float, width: Float): GlyphLayout {
		// Vanilla SelectBox positions the selected-item text straight from x/y with no pixel-grid awareness.
		val pixelsPerUnit = font.physicalPixelsPerUnit()
		return super.drawItem(
			batch, font, item,
			snapToPhysicalPixel(x, pixelsPerUnit),
			snapToPhysicalPixel(y, pixelsPerUnit),
			(width - CHEVRON_RESERVED_WIDTH).coerceAtLeast(0f),
		)
	}

	/**
	 * The dropdown's own [List] draws its rows via vanilla `List.drawItem`, with the same unsnapped-position gap
	 * [drawItem] above fixes for the closed box - apply the identical snap via the two hooks gdx ships for exactly
	 * this (customizing the popup's scroll pane / list) instead of reimplementing the popup.
	 */
	override fun newScrollPane(): SelectBoxScrollPane<T> {
		return object : SelectBoxScrollPane<T>(this@MetaSelectBox) {
			override fun newList(): List<T> {
				return object : List<T>(this@MetaSelectBox.style.listStyle) {
					init {
						cursorPointer()
					}

					override fun toString(obj: T): String = this@MetaSelectBox.toString(obj)

					override fun drawItem(batch: Batch, font: BitmapFont, index: Int, item: T, x: Float, y: Float, width: Float): GlyphLayout {
						val pixelsPerUnit = font.physicalPixelsPerUnit()
						return super.drawItem(
							batch, font, index, item,
							snapToPhysicalPixel(x, pixelsPerUnit),
							snapToPhysicalPixel(y, pixelsPerUnit),
							width,
						)
					}
				}
			}
		}
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		super.draw(batch, parentAlpha)
		val drawable = if (dropdownOpenValue.peek()) chevronUp else chevronDown
		drawable.draw(
			batch,
			x + width - CHEVRON_RIGHT_PAD - CHEVRON_SIZE,
			y + (height - CHEVRON_SIZE) * 0.5f,
			CHEVRON_SIZE.toFloat(),
			CHEVRON_SIZE.toFloat(),
		)
	}

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	/** Re-fetches the font into the (cloned) box + dropdown-list styles after a UI-scale change. */
	override fun refreshFont() {
		fontTracker.markFresh()
		focusStyle.refreshFont(fontProvider.getFont(fontSize, FontType.REGULAR))
		invalidateHierarchy()
	}

	/** Self-heal on (re)attach: a box that was detached during a UI-scale change holds a disposed font. */
	override fun setStage(stage: Stage?) {
		super.setStage(stage)
		if (stage != null) fontTracker.refreshIfStale(this)
	}

	override fun showScrollPane() {
		super.showScrollPane()
		dropdownOpenValue.value = true

		if (uiControlHelper.activated) {
			wasHelperActive = true
			uiControlHelper.activated = false
		}
	}

	override fun onHide(scrollPane: Actor?) {
		super.onHide(scrollPane)
		dropdownOpenValue.value = false

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

	private companion object {
		const val CHEVRON_SIZE = 18
		const val CHEVRON_RIGHT_PAD = 8f
		const val CHEVRON_RESERVED_WIDTH = 30f
	}
}
