package de.fatox.meta.ui.components

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.extensions.onClick
import de.fatox.meta.api.extensions.tooltip
import de.fatox.meta.reactive.Disposable
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.MetaButtonTier
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaSpacing
import de.fatox.meta.ui.MetaType

/** Density presets for tool-style action lists. */
enum class MetaActionRowDensity(val height: Float, val horizontalPad: Float, val verticalPad: Float) {
	COMPACT(32f, MetaSpacing.SM, MetaSpacing.XXS),
	COMFORTABLE(44f, MetaSpacing.MD, MetaSpacing.XS),
}

/**
 * A compact, selectable list row with an optional leading actor, subtitle, trailing metadata and overflow menu.
 * Consumers provide content and actions; Meta owns the interaction, spacing, selection and popup behavior.
 */
class MetaActionRow @JvmOverloads constructor(
	title: String,
	leading: Actor? = null,
	interactiveLeading: Boolean = false,
	subtitle: String = "",
	trailing: Actor? = null,
	interactiveTrailing: Boolean = false,
	density: MetaActionRowDensity = MetaActionRowDensity.COMPACT,
	tier: MetaButtonTier = MetaButtonTier.TERTIARY,
	private val actions: ((MetaMenu) -> Unit)? = null,
) : MetaTable() {
	val titleValue: Signal<String> = signal(title)
	val subtitleValue: Signal<String> = signal(subtitle)
	val selectedValue: Signal<Boolean> = signal(false)
	val disabledValue: Signal<Boolean> = signal(false)

	private val titleLabel = MetaLabel(title, MetaType.BODY).apply {
		setAlignment(Align.left)
		setEllipsis(true)
		touchable = Touchable.disabled
	}
	private val subtitleLabel = MetaLabel(subtitle, MetaType.CAPTION, MetaColor.TEXT_MUTED).apply {
		setAlignment(Align.left)
		setEllipsis(true)
		touchable = Touchable.disabled
	}
	private val primary = MetaButtonContainer(tier)
	private val subtitleCell: Cell<MetaLabel>
	private val menuPosition = Vector2()
	private var popup: MetaMenu? = null
	private var activation: (() -> Unit)? = null
	private var scope = ReactiveScope()

	val actionsButton: MetaIconButton? = actions?.let {
		MetaImageButton("ri-menu-line", size = 18).apply {
			tooltip("More actions")
			onClick { showActions() }
		}
	}

	init {
		left()
		primary.apply {
			left()
			pad(density.verticalPad, density.horizontalPad, density.verticalPad, density.horizontalPad)
			if (leading != null) {
				if (!interactiveLeading) leading.touchable = Touchable.disabled
				add(leading).padRight(MetaSpacing.SM)
			}
			add(MetaTable().apply {
				left()
				add(titleLabel).minWidth(0f).prefWidth(0f).growX().left()
				row()
				subtitleCell = add(subtitleLabel).minWidth(0f).prefWidth(0f).growX().left()
			}).minWidth(0f).growX()
			if (trailing != null) {
				if (!interactiveTrailing) trailing.touchable = Touchable.disabled
				add(trailing).padLeft(MetaSpacing.SM)
			}
			onClick primaryClick@ { event ->
				if (event.targetsNestedButton(primary)) return@primaryClick
				if (!isDisabled) activation?.invoke()
			}
		}
		add(primary).minWidth(0f).growX().height(density.height)
		actionsButton?.let {
			val actionSize = density.height - density.verticalPad * 2f
			add(it).size(actionSize)
				.padTop(density.verticalPad).padBottom(density.verticalPad)
				.padLeft(MetaSpacing.XS).padRight(density.verticalPad)
		}
		installBindings()
	}

	fun onActivate(action: () -> Unit): MetaActionRow = apply { activation = action }

	fun dismissActions() {
		popup?.remove()
		popup = null
	}

	override fun setStage(stage: Stage?) {
		val wasOnStage = this.stage != null
		super.setStage(stage)
		if (stage != null && scope.isDisposed) {
			scope = ReactiveScope()
			installBindings()
		} else if (wasOnStage && stage == null) {
			dismissActions()
			scope.dispose()
		}
	}

	private fun installBindings() {
		scope.effect("MetaActionRow.title") { titleLabel.setText(titleValue.value) }
		scope.effect("MetaActionRow.subtitle") {
			val text = subtitleValue.value
			subtitleLabel.setText(text)
			val visible = text.isNotEmpty()
			subtitleLabel.isVisible = visible
			subtitleCell.height(if (visible) Value.prefHeight else Value.Fixed(0f))
			subtitleCell.padTop(if (visible) MetaSpacing.XXS else 0f)
			invalidateHierarchy()
		}
		scope.effect("MetaActionRow.selected") { primary.isChecked = selectedValue.value }
		scope.effect("MetaActionRow.disabled") {
			val disabled = disabledValue.value
			primary.isDisabled = disabled
			actionsButton?.isDisabled = disabled
		}
	}

	private fun showActions() {
		val button = actionsButton ?: return
		val stage = button.stage ?: return
		dismissActions()
		val menu = MetaMenu("")
		actions?.invoke(menu)
		popup = menu
		button.localToStageCoordinates(menuPosition.set(0f, 0f))
		menu.showMenu(stage, menuPosition.x, menuPosition.y)
	}

}

/**
 * Scrollable action-row collection with reactive selection and scope-owned reactive item binding.
 * Rebuilds happen only when the submitted/bound item collection changes, never per frame.
 */
class MetaActionList<T>(
	horizontalScroll: Boolean = false,
	private val createRow: (item: T, select: () -> Unit) -> MetaActionRow,
) : MetaTable() {
	val selectedItemValue: Signal<T?> = signal(null)
	val rowsTable = MetaTable().apply {
		top().left()
		defaults().growX().spaceBottom(MetaSpacing.XXS)
	}
	val scrollPane = MetaScrollPane(rowsTable).apply {
		fadeScrollBars = false
		setFlickScroll(false)
		setScrollingDisabled(!horizontalScroll, false)
	}

	private val items = Array<T>()
	private val rows = Array<MetaActionRow>()

	init {
		add(scrollPane).grow()
	}

	fun submit(newItems: Array<out T>) {
		items.clear()
		for (i in 0 until newItems.size) items.add(newItems[i])
		rebuild()
	}

	fun submit(newItems: List<T>) {
		items.clear()
		for (i in newItems.indices) items.add(newItems[i])
		rebuild()
	}

	fun bindItems(scope: ReactiveScope, source: ReactiveValue<List<T>>): Disposable =
		scope.effect("MetaActionList.items") { submit(source.value) }

	fun selectItem(item: T?) {
		selectedItemValue.value = item
		applySelection()
	}

	private fun rebuild() {
		for (i in 0 until rows.size) rows[i].dismissActions()
		rows.clear()
		rowsTable.clearChildren()
		for (i in 0 until items.size) {
			val item = items[i]
			val row = createRow(item) { selectItem(item) }
			rows.add(row)
			rowsTable.add(row).growX().row()
		}
		val selected = selectedItemValue.peek()
		if (selected != null && !items.contains(selected, false)) selectedItemValue.value = null
		applySelection()
	}

	private fun applySelection() {
		val selected = selectedItemValue.peek()
		for (i in 0 until rows.size) rows[i].selectedValue.value = items[i] == selected
	}
}
