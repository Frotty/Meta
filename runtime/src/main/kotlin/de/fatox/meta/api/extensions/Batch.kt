package de.fatox.meta.api.extensions

import com.badlogic.gdx.graphics.g2d.Batch

inline fun <T : Batch> T.use(action: () -> Unit) {
	begin()
	action()
	end()
}