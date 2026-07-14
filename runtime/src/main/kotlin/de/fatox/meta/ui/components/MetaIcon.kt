package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import de.fatox.meta.api.graphics.FontType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MetaIcon @JvmOverloads constructor(
	icon: String,
	size: Int = MetaIconButton.DEFAULT_ICON_SIZE.toInt(),
	color: Color? = Color.WHITE,
) : MetaLabel(MetaIcons.glyph(icon), size, color, FontType.ICON) {
	var iconName: String = MetaIcons.normalize(icon)
		private set

	fun setIcon(icon: String) {
		iconName = MetaIcons.normalize(icon)
		setText(MetaIcons.glyph(iconName))
	}

}

@Suppress("FunctionName")
@OptIn(ExperimentalContracts::class)
inline fun MetaIcon(
	icon: String,
	size: Int = MetaIconButton.DEFAULT_ICON_SIZE.toInt(),
	color: Color? = Color.WHITE,
	init: MetaIcon.() -> Unit
): MetaIcon {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MetaIcon(icon, size, color).apply(init)
}
