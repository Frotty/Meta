package de.fatox.meta.graphics.animation

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class MetaAnimationHandlerTest {
	@Test
	fun `immediate play and reset discard queued presentation state`() {
		val default = animation()
		val transition = animation()
		val handler = MetaAnimationHandler(default)

		handler.queue(transition)
		handler.play(default)
		assertFalse(handler.isQueueNotEmpty())

		handler.queue(transition)
		handler.reset()
		assertFalse(handler.isQueueNotEmpty())
	}

	private fun animation(): Animation<TextureRegion> =
		Animation(0.1f, Array.with(TextureRegion()))
}
