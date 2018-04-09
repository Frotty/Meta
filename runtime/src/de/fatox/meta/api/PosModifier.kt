package de.fatox.meta.api

interface PosModifier {
    fun modify(x: Int, y: Int)

    fun getX(): Int
    fun getY(): Int
}

class DummyPosModifier : PosModifier {
    override fun getX(): Int {
        return -1
    }

    override fun getY(): Int {
        return -1
    }

    override fun modify(x: Int, y: Int) {

    }

}