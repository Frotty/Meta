package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.ui.components.MetaFlexAlign
import de.fatox.meta.ui.components.MetaFlexBox
import de.fatox.meta.ui.components.MetaFlexDirection
import de.fatox.meta.ui.components.MetaFlexJustify
import de.fatox.meta.ui.components.MetaGrid
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaStack
import de.fatox.meta.ui.components.MetaTable
import de.fatox.meta.ui.components.MetaTextButton

/**
 * Meta's UI design tokens - the single source of "Meta feel". Prefer these over magic numbers and ad-hoc colors so
 * every screen/window shares one typographic scale, spacing rhythm and palette.
 *
 * All values are constants or load-time singletons, so referencing them never allocates (no per-frame GC churn).
 * [MetaColor] values are shared, mutable libGDX [Color] objects - treat them as READ-ONLY; call `.cpy()` if you need
 * a variant. Use [metaLabel]/[metaButton] so all text comes from the Meta font provider.
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
 * Shared interaction density for Meta controls. A size owns the complete control rhythm so composed widgets do not
 * drift into unrelated 30/32/34/38px conventions as their fonts and icons evolve.
 */
enum class MetaControlSize(
	val height: Float,
	val iconSize: Int,
	val iconTarget: Float,
	val horizontalPadding: Float,
) {
	COMPACT(32f, 16, 32f, MetaSpacing.SM),
	STANDARD(40f, 18, 40f, MetaSpacing.MD),
	COMFORTABLE(48f, 20, 48f, MetaSpacing.LG),
}

/**
 * The Meta palette. Shared, mutable [Color] instances - do NOT mutate them; use [Color.cpy] for a variant.
 * Dark theme derived from the engine's backdrop/clear colors.
 */
object MetaColor {
	val BACKGROUND: Color = Color.valueOf("191C21FF")
	val SURFACE: Color = Color.valueOf("24282FFF")
	val SURFACE_RAISED: Color = Color.valueOf("2D323AFF")
	val BORDER: Color = Color.valueOf("48515DFF")

	val TEXT: Color = Color.valueOf("FFFFFFFF")
	val TEXT_MUTED: Color = Color.valueOf("B8BBC5FF")
	val TEXT_DISABLED: Color = Color.valueOf("858894FF")

	/** Primary action fill. Primary states are regression-tested against white text at WCAG AA contrast. */
	val PRIMARY: Color = Color.valueOf("276F9FFF")
	val PRIMARY_HOVER: Color = Color.valueOf("3078A5FF")
	val PRIMARY_PRESSED: Color = Color.valueOf("205B82FF")
	val SECONDARY: Color = Color.valueOf("303741FF")
	val SECONDARY_HOVER: Color = Color.valueOf("3A4551FF")
	val TERTIARY: Color = Color.valueOf("00000000")
	val TERTIARY_HOVER: Color = Color.valueOf("303842FF")

	val ACCENT: Color = Color.valueOf("59C2FFFF")
	val POSITIVE: Color = Color.valueOf("6FCF63FF")
	val NEGATIVE: Color = Color.valueOf("E5534BFF")
	val WARNING: Color = Color.valueOf("E0A33AFF")
}

/** Semantic action emphasis shared by text, icon-text, icon, and custom-content buttons. */
enum class MetaButtonTier {
	PRIMARY,
	SECONDARY,
	TERTIARY,
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
	tier: MetaButtonTier = MetaButtonTier.SECONDARY,
): MetaTextButton = MetaTextButton(text, size, type, tier)

/** Sets sensible Meta cell defaults on a table (uniform [MetaSpacing] padding). Returns the table for chaining. */
fun <T : Table> T.metaDefaults(pad: Float = MetaSpacing.SM): T = apply { defaults().pad(pad) }

/** Adds a cell with standard Meta spacing around it. */
fun <T : com.badlogic.gdx.scenes.scene2d.Actor> Table.addSpaced(actor: T, space: Float = MetaSpacing.SM): Cell<T> =
	add(actor).pad(space)

/** A content row with Meta's standard gap between children. Prefer this for ordinary control/action rows. */
fun metaRow(spacing: Float = MetaSpacing.SM, init: MetaTable.() -> Unit = {}): MetaTable = MetaTable().apply {
	left()
	defaults().spaceRight(spacing)
	init()
}

/** A content column with Meta's standard gap between children. Call [Table.row] between added children. */
fun metaColumn(spacing: Float = MetaSpacing.SM, init: MetaTable.() -> Unit = {}): MetaTable = MetaTable().apply {
	top().left()
	defaults().growX().spaceBottom(spacing)
	init()
}

/** Responsive row layout. Prefer this over table cells for ordinary horizontal composition. */
fun metaFlexRow(
	wrap: Boolean = false,
	gap: Float = MetaSpacing.SM,
	crossGap: Float = gap,
	justify: MetaFlexJustify = MetaFlexJustify.START,
	align: MetaFlexAlign = MetaFlexAlign.START,
	init: MetaFlexBox.() -> Unit = {},
): MetaFlexBox = MetaFlexBox(
	direction = MetaFlexDirection.ROW,
	wrap = wrap,
	mainGap = gap,
	crossGap = crossGap,
	justify = justify,
	align = align,
).apply(init)

/** Responsive column layout. Enable [wrap] to flow tall content into additional columns. */
fun metaFlexColumn(
	wrap: Boolean = false,
	gap: Float = MetaSpacing.SM,
	crossGap: Float = gap,
	justify: MetaFlexJustify = MetaFlexJustify.START,
	align: MetaFlexAlign = MetaFlexAlign.START,
	init: MetaFlexBox.() -> Unit = {},
): MetaFlexBox = MetaFlexBox(
	direction = MetaFlexDirection.COLUMN,
	wrap = wrap,
	mainGap = gap,
	crossGap = crossGap,
	justify = justify,
	align = align,
).apply(init)

/** Responsive equal-track grid with an auto-fit column count. */
fun metaGrid(
	minColumnWidth: Float,
	maxColumns: Int = Int.MAX_VALUE,
	columnGap: Float = MetaSpacing.SM,
	rowGap: Float = MetaSpacing.SM,
	horizontalAlign: MetaFlexAlign = MetaFlexAlign.STRETCH,
	verticalAlign: MetaFlexAlign = MetaFlexAlign.START,
	init: MetaGrid.() -> Unit = {},
): MetaGrid = MetaGrid(
	minColumnWidth,
	maxColumns,
	columnGap,
	rowGap,
	horizontalAlign,
	verticalAlign,
).apply(init)

/** Layered layout for backgrounds, overlays and badges sharing the same bounds. */
fun metaStack(
	horizontalAlign: MetaFlexAlign = MetaFlexAlign.STRETCH,
	verticalAlign: MetaFlexAlign = MetaFlexAlign.STRETCH,
	init: MetaStack.() -> Unit = {},
): MetaStack = MetaStack(horizontalAlign, verticalAlign).apply(init)
