package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.ui.Button
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.injection.MetaInject.Companion.inject
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
	assetProvider: AssetProvider = inject(),
	initialChecked: Boolean = false,
) : Button(Button.ButtonStyle(MetaSkin.skin().get(MetaSkin.CHECKBOX, Button.ButtonStyle::class.java))) {

	init {
		isChecked = initialChecked
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
