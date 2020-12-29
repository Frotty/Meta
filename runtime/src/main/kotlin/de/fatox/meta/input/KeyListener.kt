package de.fatox.meta.input

import com.badlogic.gdx.utils.Timer

abstract class KeyListener {
    private var requiredLengthMillis: Long = 0
    private var task: Timer.Task? = null
    abstract fun onEvent()
    fun onDown() {
        if (requiredLengthMillis > 0) {
            task = object : Timer.Task() {
                override fun run() {
                    Timer.schedule(task, requiredLengthMillis / 1000f)
                    onEvent()
                }
            }
            Timer.schedule(task, requiredLengthMillis / 1000f)
        }
    }

    fun onUp() {
        if (task != null) {
            task!!.cancel()
        }
        if (requiredLengthMillis <= 0) {
            onEvent()
        }
    }

    fun resetDelay() {
        task!!.cancel()
        Timer.schedule(task, requiredLengthMillis / 1000f)
    }

    fun setRequiredLengthMillis(requiredLengthMillis: Long) {
        this.requiredLengthMillis = requiredLengthMillis
    }
}