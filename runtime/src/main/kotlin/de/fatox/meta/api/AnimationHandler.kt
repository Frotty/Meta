package de.fatox.meta.api

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Wraps a libGDX Animation, tracking state time and a queue of animations.
 * Queued animations will play once the current animation has finished.
 */
interface AnimationHandler {
	/** The current time state of the handlers [animation][currentAnimation]. */
	var stateTime: Float

	/** The current [animation][Animation]. */
	val currentAnimation: Animation<TextureRegion>

	/** The current playing status of the handlers [animation][currentAnimation]. */
	val isPlaying: Boolean

	/** The current frame of the handlers [animation][currentAnimation]. */
	val currentFrame: TextureRegion get() = currentAnimation.getKeyFrame(stateTime)

	/**
	 * @return `true` if the animation is finished, `false` otherwise.
	 */
	fun isFinished(): Boolean = currentAnimation.isAnimationFinished(stateTime)

	/**
	 * Instantly plays the animation the handler was initialized with.
	 */
	fun playDefault()

	/**
	 * Instantly plays the given animation from its start.
	 *
	 * @param animation The animation to be played instantly.
	 */
	fun play(animation: Animation<TextureRegion>)

	/**
	 * Queues the given animation to be played after the current one finishes, if it is not queued already.
	 *
	 * @param animation Animation<TextureRegion>
	 */
	fun queue(animation: Animation<TextureRegion>)

	/**
	 * Updates the animation state by the given time.
	 *
	 * @param delta The time to progress the animation.
	 */
	fun update(delta: Float)

	/**
	 * Randomizes the current [stateTime].
	 */
	fun randomizeStateTime()

	/**
	 * Pauses the animation on the current frame.
	 *
	 * It will still stay as the last animation and return the same frame.
	 */
	fun pause()

	/**
	 * Resets everything managed by this handler to its initial state.
	 */
	fun reset()

	/**
	 * @return `true` if the animation queue is empty, `false` otherwise.
	 */
	fun isQueueEmpty(): Boolean

	/**
	 * @return `true` if the animation queue is not empty, `false` otherwise.
	 */
	fun isQueueNotEmpty(): Boolean
}

