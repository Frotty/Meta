package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.Button
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.injection.MetaInject.Companion.inject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A simple checkbox: a [Button] whose checked/unchecked/hover states are drawn from the standard Meta UI
 * checkbox drawables. The drawables are resolved through the [AssetProvider] under the conventional
 * `textures/ui/checkbox_*` paths, so a consuming game only needs to ship those regions to get the Meta look.
 */
class MetaCheckBox @JvmOverloads constructor(
	assetProvider: AssetProvider = inject(),
	initialChecked: Boolean = false,
) : Button(createStyle(assetProvider)) {

	init {
		isChecked = initialChecked
	}

	companion object {
		private fun createStyle(assetProvider: AssetProvider): ButtonStyle = ButtonStyle().apply {
			up = assetProvider.getDrawable("textures/ui/checkbox_off")
			checked = assetProvider.getDrawable("textures/ui/checkbox_on")
			down = assetProvider.getDrawable("textures/ui/checkbox_down")
			over = assetProvider.getDrawable("textures/ui/checkbox_hover")
			checkedOver = assetProvider.getDrawable("textures/ui/checkbox_on_over")
		}
	}
}

@Suppress("FunctionName")
@OptIn(ExperimentalContracts::class)
inline fun MetaCheckBox(
	initialChecked: Boolean = false,
	assetProvider: AssetProvider = inject(),
	init: MetaCheckBox.() -> Unit,
): MetaCheckBox {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MetaCheckBox(assetProvider, initialChecked).apply(init)
}
