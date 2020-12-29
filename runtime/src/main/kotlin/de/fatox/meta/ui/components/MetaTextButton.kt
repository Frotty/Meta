package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle
import de.fatox.meta.util.GoldenRatio

/**
 * Created by Frotty on 04.06.2016.
 */
open class MetaTextButton @JvmOverloads constructor(text: String?, size: Int = 12) : Button(
    VisUI.getSkin().get(
        VisTextButtonStyle::class.java
    )
) {
    private val label: MetaLabel
    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
    }

    fun setText(text: String?) {
        label.setText(text)
    }

    val text: CharSequence?
        get() = label.text

    init {
        pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20)
        label = MetaLabel(text, size, Color.WHITE)
        label.setAlignment(Align.center)
        add(label).center().grow()
    }
}