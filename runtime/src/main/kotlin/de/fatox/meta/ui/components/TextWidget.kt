package de.fatox.meta.ui.components

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Align
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.injection.Inject

/**
 * Created by Frotty on 05.06.2016.
 */
class TextWidget(text: String) : Widget() {
    @Inject
    private val fontProvider: FontProvider? = null
    private val text: String
    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
        fontProvider!!.getFont(80, false).draw(batch, text, x, y + height / 2, width, Align.center, false)
    }

    init {
        inject(this)
        this.text = text
    }
}