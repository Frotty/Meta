package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.cursorPointer
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.reactive.subscribe
import de.fatox.meta.ui.MetaColor
import de.fatox.meta.ui.MetaFocusable
import de.fatox.meta.ui.MetaSkin
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A simple checkbox using Meta's generated checked/unchecked/hover states. The assetProvider parameter is retained
 * for source compatibility with older call sites; the default visuals no longer require texture assets.
 */
class MetaCheckBox @JvmOverloads constructor(
	@Suppress("UNUSED_PARAMETER")
	assetProvider: AssetProvider? = null,
	initialChecked: Boolean = false,
) : Button(Button.ButtonStyle(MetaSkin.skin().get(MetaSkin.CHECKBOX, Button.ButtonStyle::class.java))), MetaFocusable {
	private val focusStyle = MetaButtonFocusStyle(this, style, MetaSkin::focusedCheckboxStyle)
	private val checkIcon = MetaIcon("ri-check-line", CHECK_ICON_SIZE, MetaColor.TEXT.cpy()).apply {
		touchable = Touchable.disabled
	}
	private var checkIconAttached = false
	val checkedValue: Signal<Boolean> = signal(initialChecked)
	val disabledValue: Signal<Boolean> = signal(isDisabled)
	private val checkedBinding = checkedValue.subscribe { applySignalToButton() }
	private val disabledBinding = disabledValue.subscribe { syncCheckIcon() }

	init {
		// Signal writes sync back into the widget in [applySignalToButton]; disable programmatic ChangeEvents so
		// that sync (and programmatic setChecked calls) can't fire consumer ChangeListeners or recurse. User clicks
		// still fire their ChangeEvent unconditionally.
		setProgrammaticChangeEvents(false)
		isChecked = initialChecked
		addListener(object : ChangeListener() {
			override fun changed(event: ChangeEvent, actor: Actor) {
				syncSignalsFromButton()
			}
		})
		syncSignalsFromButton()
		syncCheckIcon()
		cursorPointer()
	}

	override fun setMetaFocused(focused: Boolean) {
		focusStyle.setFocused(focused)
	}

	override fun setChecked(isChecked: Boolean) {
		super.setChecked(isChecked)
		syncSignalsFromButton()
	}

	override fun setDisabled(isDisabled: Boolean) {
		super.setDisabled(isDisabled)
		touchable = if (isDisabled) Touchable.disabled else Touchable.enabled
		disabledValue.value = isDisabled
	}

	private fun syncSignalsFromButton() {
		if (checkedValue.peek() != isChecked) checkedValue.value = isChecked
	}

	/**
	 * Signal -> widget direction: an external [checkedValue] write updates [isChecked] and the check glyph. No
	 * recursion: setChecked -> [syncSignalsFromButton] sees the states already equal, and programmaticChangeEvents
	 * is off so no consumer ChangeEvent fires.
	 */
	private fun applySignalToButton() {
		val desired = checkedValue.peek()
		if (isChecked != desired) isChecked = desired
		syncCheckIcon()
	}

	private fun syncCheckIcon() {
		if (isChecked != checkIconAttached) {
			clearChildren()
			if (isChecked) add(checkIcon).size(CHECK_ICON_SIZE.toFloat()).center()
			checkIconAttached = isChecked
			invalidateHierarchy()
		}
		if (checkIconAttached) checkIcon.setColor(if (isDisabled) MetaColor.TEXT_DISABLED else MetaColor.TEXT)
	}

	private companion object {
		const val CHECK_ICON_SIZE = 18
	}
}

@Suppress("FunctionName")
@OptIn(ExperimentalContracts::class)
inline fun MetaCheckBox(
	initialChecked: Boolean = false,
	assetProvider: AssetProvider? = null,
	init: MetaCheckBox.() -> Unit,
): MetaCheckBox {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MetaCheckBox(assetProvider, initialChecked).apply(init)
}
