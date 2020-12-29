package de.fatox.meta.api

import com.badlogic.gdx.audio.Sound

interface SoundHandler {
	fun duration(sound: Sound): Float
}

object NoSoundHandler : SoundHandler {
	override fun duration(sound: Sound): Float = 0f
}