package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import de.fatox.meta.api.graphics.FontType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MetaIcon @JvmOverloads constructor(
	icon: String,
	size: Int = 24,
	color: Color? = Color.WHITE,
) : MetaLabel(MetaIcons.glyph(icon), size, color, FontType.ICON) {
	var iconName: String = MetaIcons.normalize(icon)
		private set

	fun setIcon(icon: String) {
		iconName = MetaIcons.normalize(icon)
		setText(MetaIcons.glyph(iconName))
	}

	override fun adjustDrawPosition(position: Vector2) {
		val glyph = activeFont.data.getGlyph(text[0]) ?: return
		// MetaLabel centers a line using the font's cap height. Icon-font glyphs can have asymmetric vertical bearings,
		// which makes a rotating glyph orbit around the actor center. Move the glyph's actual bounds center onto it.
		val glyphCenterFromBaseline = (glyph.yoffset + glyph.height * 0.5f) * activeFont.scaleY
		position.y -= activeFont.capHeight * 0.5f + glyphCenterFromBaseline
	}
}

@Suppress("FunctionName")
@OptIn(ExperimentalContracts::class)
inline fun MetaIcon(
	icon: String,
	size: Int = 24,
	color: Color? = Color.WHITE,
	init: MetaIcon.() -> Unit
): MetaIcon {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MetaIcon(icon, size, color).apply(init)
}
