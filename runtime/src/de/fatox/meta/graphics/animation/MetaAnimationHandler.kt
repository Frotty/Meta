package de.fatox.meta.graphics.animation

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.*
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import de.fatox.meta.api.AnimationHandler

/**
 * Default [AnimationHandler] implementation.
 *
 * @constructor Creates a new instance of [AnimationHandler].
 */
class MetaAnimationHandler(
	private val defaultAnimation: Animation<TextureRegion>,
	private val startingQueueSize: Int = 2,
	private val startingStateTime: Float = 0f,
	override var stateTime: Float = startingStateTime,
) : AnimationHandler {
	/** The animation queue filled by [queueAnimation]. */
	private val animationQueue: Array<Animation<TextureRegion>> = Array(startingQueueSize)

	override var currentAnimation: Animation<TextureRegion> = defaultAnimation
		private set

	override var isPlaying: Boolean = false
		private set

	override fun playDefaultAnimation() {
		stateTime = 0f
		currentAnimation = defaultAnimation
		isPlaying = true
	}

	override fun playAnimation(animation: Animation<TextureRegion>) {
		stateTime = 0f
		currentAnimation = animation
		isPlaying = true
	}

	override fun queueAnimation(animation: Animation<TextureRegion>) {
		if (animationQueue.isEmpty || animationQueue.peek() !== animation) {
			animationQueue.add(animation)
		}
	}

	override fun update(delta: Float) {
		if (!isPlaying) return

		stateTime += delta
		if (currentAnimation.playMode == PlayMode.NORMAL && currentAnimation.isAnimationFinished(stateTime)) {
			if (animationQueue.size > 0) {
				currentAnimation = animationQueue.pop()
				stateTime = 0f
			} else {
				stateTime = currentAnimation.animationDuration
				isPlaying = false
			}
		}
	}

	override fun randomizeStateTime() {
		stateTime = MathUtils.random(0f, currentAnimation.animationDuration) // TODO replace with own random
	}

	override fun freezeAnimation() {
		isPlaying = false
	}

	override fun resetAnimation() {
		isPlaying = false
		stateTime = startingStateTime
		animationQueue.clear()
		animationQueue.setSize(startingQueueSize)
		currentAnimation = defaultAnimation
	}

	override fun isQueueEmpty(): Boolean = animationQueue.isEmpty

	override fun isQueueNotEmpty(): Boolean = animationQueue.notEmpty()
}