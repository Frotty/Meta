package de.fatox.meta.ui.components

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle
import de.fatox.meta.util.GoldenRatio
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaTextButton @JvmOverloads constructor(text: String = "", size: Int = 12) :
	Button(VisUI.getSkin().get(VisTextButtonStyle::class.java)) {

	private val label: MetaLabel = MetaLabel(text, size) { setAlignment(Align.center) }

	fun setText(text: String = "") {
		label.setText(text)
	}

	val text: CharSequence = label.text

	init {
		pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20)
		add(label).center().grow()
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
inline fun MetaTextButton(text: String = "", size: Int = 12, init: MetaTextButton.() -> Unit): MetaTextButton {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MetaTextButton(text, size).apply(init)
}