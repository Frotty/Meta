package de.fatox.meta.api.entity

import com.badlogic.gdx.math.Vector

interface Entity<out DIMENSION : Vector<*>> {

    val id: Int

    val position: DIMENSION

    fun update()

    fun draw()
}
