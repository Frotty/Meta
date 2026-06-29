package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTextButton

/**
 * Meta's UI design tokens - the single source of "Meta feel". Prefer these over magic numbers and ad-hoc colors so
 * every screen/window shares one typographic scale, spacing rhythm and palette.
 *
 * All values are constants or load-time singletons, so referencing them never allocates (no per-frame GC churn).
 * [MetaColor] values are shared, mutable libGDX [Color] objects - treat them as READ-ONLY; call `.cpy()` if you need
 * a variant. Use [metaLabel]/[metaButton] (TTF, via the Meta font provider) instead of VisUI's baked-glyph widgets.
 */

/** Typographic scale in pixels (the font provider rasterizes TTF at these sizes). Semantic, not t-shirt, names. */
object MetaType {
	const val CAPTION = 12
	const val BODY = 16
	const val LABEL = 18
	const val SUBTITLE = 21
	const val TITLE = 24
	const val HEADING = 32
	const val DISPLAY = 48
}

/** Spacing/padding rhythm in pixels. Use for cell padding and gaps so layouts breathe consistently. */
object MetaSpacing {
	const val NONE = 0f
	const val XXS = 2f
	const val XS = 4f
	const val SM = 8f
	const val MD = 12f
	const val LG = 16f
	const val XL = 24f
	const val XXL = 32f
}

/**
 * The Meta palette. Shared, mutable [Color] instances - do NOT mutate them; use [Color.cpy] for a variant.
 * Dark theme derived from the engine's backdrop/clear colors.
 */
object MetaColor {
	val BACKGROUND: Color = Color.valueOf("1F2025FF")
	val SURFACE: Color = Color.valueOf("2B2B2EFF")
	val SURFACE_RAISED: Color = Color.valueOf("35353AFF")
	val BORDER: Color = Color.valueOf("3A3A40FF")

	val TEXT: Color = Color.valueOf("FFFFFFFF")
	val TEXT_MUTED: Color = Color.valueOf("A0A0A8FF")
	val TEXT_DISABLED: Color = Color.valueOf("6A6A72FF")

	val ACCENT: Color = Color.valueOf("4F9DDEFF")
	val POSITIVE: Color = Color.valueOf("6FCF63FF")
	val NEGATIVE: Color = Color.valueOf("E5534BFF")
	val WARNING: Color = Color.valueOf("E0A33AFF")
}

/** Creates a TTF [MetaLabel] using the Meta palette/scale defaults. */
fun metaLabel(
	text: CharSequence,
	size: Int = MetaType.BODY,
	color: Color = MetaColor.TEXT,
	type: FontType = FontType.REGULAR,
): MetaLabel = MetaLabel(text, size, color, type)

/** Creates a TTF [MetaTextButton] using the Meta scale defaults. */
fun metaButton(
	text: String,
	size: Int = MetaType.LABEL,
	type: FontType = FontType.REGULAR,
): MetaTextButton = MetaTextButton(text, size, type)

/** Sets sensible Meta cell defaults on a table (uniform [MetaSpacing] padding). Returns the table for chaining. */
fun <T : Table> T.metaDefaults(pad: Float = MetaSpacing.SM): T = apply { defaults().pad(pad) }

/** Adds a cell with standard Meta spacing around it. */
fun <T : com.badlogic.gdx.scenes.scene2d.Actor> Table.addSpaced(actor: T, space: Float = MetaSpacing.SM): Cell<T> =
	add(actor).pad(space)
