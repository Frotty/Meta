package de.fatox.meta.api

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion

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