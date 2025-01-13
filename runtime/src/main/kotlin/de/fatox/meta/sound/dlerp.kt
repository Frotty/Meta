package de.fatox.meta.sound

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import kotlin.math.exp

/**
 * Performs a damped linear interpolation between this float and the target float `b`.
 *
 * @param b The target float value.
 * @param decay The decay factor, which controls the rate of interpolation.
 *              Reasonable values are typically between 0.1 and 10.0.
 *              Higher values result in faster interpolation (less smoothing),
 *              while lower values result in slower interpolation (more smoothing).
 *              For example, with a decay of 0.1, a value will move from 0 to 1 in approximately 2.5 seconds.
 * @param delta The time step for the interpolation.
 * @return The interpolated float value.
 */
fun Float.dlerp(b: Float, decay: Float, delta: Float): Float {
	return b+(this-b)* exp(-(decay * 25)*delta)
}

// Same extension but for Vector2
fun Vector2.dlerp(b: Vector2, decay: Float, delta: Float) {
	this.y = this.y.dlerp(b.y, decay, delta)
	this.x = this.x.dlerp(b.x, decay, delta)
}

// Same extension but for Vector3
fun Vector3.dlerp(b: Vector3, decay: Float, delta: Float) {
	this.x = this.x.dlerp(b.x, decay, delta)
	this.y = this.y.dlerp(b.y, decay, delta)
	this.z = this.z.dlerp(b.z, decay, delta)
}
