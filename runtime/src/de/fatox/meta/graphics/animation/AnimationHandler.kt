package de.fatox.meta.graphics.animation

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array

/**
 * Wraps a libGDX Animation, tracking state time and a queue of animations.
 * Queued animations will play once the current animation has finished.
 */
class AnimationHandler {
	var animQueue: Array<Animation<TextureRegion>>? = null
	private var currentAnimation: Animation<TextureRegion>? = null
	var isPlaying = false
		private set
	var stateTime = 0f
	/**
	 * Plays the given animation instantly
	 */
	fun playAnimation(animation: Animation<TextureRegion>?) {
		stateTime = 0f
		currentAnimation = animation
		isPlaying = true
	}

	/**
	 * Queues the given animation to be played after the current one finishes,
	 * if it is not queued already.
	 */
	fun queueAnimation(animation: Animation<TextureRegion>) {
		if (animQueue == null) {
			animQueue = Array(2)
		}
		if (animQueue!!.size == 0 || animQueue!!.peek() !== animation) {
			animQueue!!.add(animation)
		}
	}

	/**
	 * @return The to be rendered frame of the animation
	 */
	val currentFrame: TextureRegion
		get() = currentAnimation!!.getKeyFrame(stateTime)

	/**
	 * Internally updates the animation
	 */
	fun update(delta: Float) {
		if (!isPlaying) {
			return
		}
		stateTime += delta
		if (currentAnimation!!.playMode == PlayMode.NORMAL && currentAnimation!!.isAnimationFinished(stateTime)) {
			if (animQueue != null && animQueue!!.size > 0) {
				currentAnimation = animQueue!!.pop()
				stateTime = 0f
			} else {
				stateTime = currentAnimation!!.animationDuration
				isPlaying = false
			}
		}
	}

	/**
	 * Randomizes the timeState
	 */
	fun randomizeState() {
		stateTime = MathUtils.random(0f, currentAnimation!!.animationDuration)
	}

	/**
	 * Stops the current animation from playing.
	 * It will still stay as the last animation and return the same frame.
	 */
	fun stopAnimation() {
		isPlaying = false
	}

	val isFinished: Boolean
		get() = currentAnimation!!.isAnimationFinished(stateTime)

	fun getCurrentAnimation(): Animation<TextureRegion>? {
		return currentAnimation
	}

	fun hasNoQueue(): Boolean {
		return animQueue == null || animQueue!!.size == 0
	}

	fun hasQueue(): Boolean {
		return animQueue != null && animQueue!!.size > 0
	}
}