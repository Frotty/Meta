package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle
import de.fatox.meta.util.GoldenRatio

/**
 * Created by Frotty on 04.06.2016.
 */
class MetaIconTextButton(
	text: String,
	drawable: Drawable?,
	size: Int = 12,
	maxWidth: Int? = null,
) : Button(VisUI.getSkin().get(VisTextButtonStyle::class.java)) {
	private val image: VisImage = VisImage(drawable)
	private val label: MetaLabel = MetaLabel(text, size, Color.WHITE).apply {
		setAlignment(Align.center)
		if (maxWidth != null) setMaxWidth(maxWidth)
	}
	var text: CharSequence
		get() = label.text
		set(value) = label.setText(value)

	init {
		pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20)
		add(image).center().grow()
		row()
		add(label).center().grow().pad(2f)
	}
}