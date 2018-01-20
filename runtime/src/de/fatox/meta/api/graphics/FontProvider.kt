package de.fatox.meta.api.graphics

import com.badlogic.gdx.graphics.g2d.BitmapFont

interface FontProvider {
    fun getFont(size: Int): BitmapFont

    fun write(x: Float, y: Float, text: String, size: Int)
}
