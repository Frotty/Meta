package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle
import de.fatox.meta.api.graphics.FontType
import de.fatox.meta.util.GoldenRatio
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaTextButton @JvmOverloads constructor(
	text: String = "",
	size: Int = 12,
	type: FontType = FontType.REGULAR
) :
	Button(VisUI.getSkin().get(VisTextButtonStyle::class.java)) {

	private var labelCell: Cell<MetaLabel>
	private val label: MetaLabel = MetaLabel(text, size, Color.WHITE, type) { setAlignment(Align.center) }

	fun setText(text: String = "") {
		label.setText(text)
	}

	val text: CharSequence = label.text


	init {
		pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20)
		labelCell = add(label)
		centerText()
	}

	fun centerText() {
		labelCell.center().grow()
	}

	fun leftText() {
		labelCell.left().grow()
	}

	final override fun pad(top: Float, left: Float, bottom: Float, right: Float): Table {
		return super.pad(top, left, bottom, right)
	}

	final override fun <T : Actor?> add(actor: T): Cell<T> {
		return super.add(actor)
	}
}

@Suppress("FunctionName")
@OptIn(ExperimentalContracts::class)
inline fun MetaTextButton(
	text: String = "",
	size: Int = 12,
	type: FontType = FontType.REGULAR,
	init: MetaTextButton.() -> Unit
): MetaTextButton {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MetaTextButton(text, size, type).apply(init)
}