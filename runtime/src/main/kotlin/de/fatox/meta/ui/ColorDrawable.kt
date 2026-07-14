package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable

class ColorDrawable(private val drawable: Drawable, private val color: Color) : BaseDrawable() {
    private val savedBatchColor = Color()
    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        // Save the batch colour as we are about to change it
        savedBatchColor.set(batch.color)
        // Multiply (not overwrite) so an actor's own color/parentAlpha (e.g. a fade action) is preserved
        val c = batch.color
        batch.setColor(c.r * color.r, c.g * color.g, c.b * color.b, c.a * color.a)
        drawable.draw(batch, x, y, width, height)
        batch.color = savedBatchColor
    }
}
