package de.fatox.meta.graphics.buffer

import com.badlogic.gdx.utils.Disposable

interface MetaFrameBuffer : Disposable {
	val height: Int
		get() = 0
	val width: Int
		get() = 0

	fun begin() {}
	fun end() {}
	fun getFBO(): Int = -1

	override fun dispose() {}
}

object NoMetaFrameBuffer : MetaFrameBuffer