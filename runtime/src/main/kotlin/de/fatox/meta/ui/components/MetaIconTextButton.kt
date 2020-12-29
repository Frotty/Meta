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
class MetaIconTextButton(text: String?, size: Int, drawable: Drawable?) : Button(
    VisUI.getSkin().get(
        VisTextButtonStyle::class.java
    )
) {
    private val image: VisImage
    private val label: MetaLabel

    constructor(text: String?, drawable: Drawable?, maxWidth: Int) : this(text, 12, drawable) {
        label.setMaxWidth(maxWidth)
    }

    constructor(text: String?, drawable: Drawable?) : this(text, 12, drawable) {}

    fun setText(text: String?) {
        label.setText(text)
    }

    val text: CharSequence?
        get() = label.text

    init {
        pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20)
        image = VisImage(drawable)
        label = MetaLabel(text, size, Color.WHITE)
        label.setAlignment(Align.center)
        add(image).center().grow()
        row()
        add(label).center().grow().pad(2f)
    }
}