package de.fatox.meta.graphics.animation

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array

interface IAnimationHandler {
	val stateTime: Float
	val currentAnimation: Animation<TextureRegion>
	val isPlaying: Boolean

	/**
	 * @return The to be rendered frame of the animation
	 */
	val currentFrame: TextureRegion
	val isFinished: Boolean

	/**
	 * Plays the given animation instantly
	 */
	fun playAnimation(animation: Animation<TextureRegion>)

	/**
	 * Queues the given animation to be played after the current one finishes,
	 * if it is not queued already.
	 */
	fun queueAnimation(animation: Animation<TextureRegion>)

	/**
	 * Internally updates the animation
	 */
	fun update(delta: Float)

	/**
	 * Randomizes the timeState
	 */
	fun randomizeState()

	/**
	 * Stops the current animation from playing.
	 * It will still stay as the last animation and return the same frame.
	 */
	fun stopAnimation()
	fun hasNoQueue(): Boolean
	fun hasQueue(): Boolean
}

/**
 * Wraps a libGDX Animation, tracking state time and a queue of animations.
 * Queued animations will play once the current animation has finished.
 */
class AnimationHandler(
	currentAnimation: Animation<TextureRegion> = EmptyAnimation,
	var animQueue: Array<Animation<TextureRegion>> = Array(2),
	override var stateTime: Float = 0f
) : IAnimationHandler {
	override var currentAnimation: Animation<TextureRegion> = currentAnimation
		private set

	override var isPlaying = false
		private set

	override val currentFrame: TextureRegion get() = currentAnimation.getKeyFrame(stateTime)

	override val isFinished: Boolean get() = currentAnimation.isAnimationFinished(stateTime)

	override fun playAnimation(animation: Animation<TextureRegion>) {
		stateTime = 0f
		currentAnimation = animation
		isPlaying = true
	}

	override fun queueAnimation(animation: Animation<TextureRegion>) {
		if (animQueue.size == 0 || animQueue.peek() !== animation) {
			animQueue.add(animation)
		}
	}

	override fun update(delta: Float) {
		if (!isPlaying) return

		stateTime += delta
		if (currentAnimation.playMode == PlayMode.NORMAL && currentAnimation.isAnimationFinished(stateTime)) {
			if (animQueue.size > 0) {
				currentAnimation = animQueue.pop()
				stateTime = 0f
			} else {
				stateTime = currentAnimation.animationDuration
				isPlaying = false
			}
		}
	}

	override fun randomizeState() {
		stateTime = MathUtils.random(0f, currentAnimation.animationDuration)
	}

	override fun stopAnimation() {
		isPlaying = false
	}

	override fun hasNoQueue(): Boolean = animQueue.size == 0

	override fun hasQueue(): Boolean = animQueue.size > 0
}