package de.fatox.meta.test

import com.badlogic.gdx.Game

class UndoRedoTest : Game() {
    override fun create() {}
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        println("w$width")
    }
}