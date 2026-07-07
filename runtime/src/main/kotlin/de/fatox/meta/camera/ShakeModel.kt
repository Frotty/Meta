package de.fatox.meta.camera

import com.badlogic.gdx.math.Vector3
import kotlin.math.floor

/**
 * Trauma-based screen shake model for 2D/orthographic games.
 *
 * Callers add shake energy via [addShake] or [addShakeAtDistance]. The visible amplitude is trauma squared for a
 * punchy onset and smooth tail, sampled through deterministic value noise so the motion reads as crisp vibration
 * rather than random jitter. This class is pure visual state: it has no camera, DI, GL, or simulation dependency.
 */
class ShakeModel {
	private var additive = 0f
	private var floor = 0f

	val trauma: Float get() = maxOf(additive, floor)

	private var time = 0f
	private val shake = Vector3()
	var rotation = 0f
		private set

	fun addShake(amount: Float) {
		additive = (additive + amount * TRAUMA_PER_SHAKE).coerceAtMost(1f)
	}

	fun addShakeAtDistance(nearAmount: Float, dist: Float, minFloor: Float = 0f) {
		val d = dist / SHAKE_FALLOFF_DISTANCE
		val falloff = 1f / (1f + d * d)
		additive = (additive + nearAmount * falloff * TRAUMA_PER_SHAKE).coerceAtMost(1f)
		if (minFloor > 0f) floor = maxOf(floor, minFloor)
	}

	val peakTranslation: Float get() = MAX_TRANSLATION * trauma * trauma

	fun update(delta: Float) {
		time += delta
		if (trauma > 0f) {
			additive = (additive - TRAUMA_DECAY * delta).coerceAtLeast(0f)
			floor = (floor - TRAUMA_DECAY * delta).coerceAtLeast(0f)
			val amount = trauma * trauma
			val t = time * SHAKE_FREQUENCY
			shake.set(
				MAX_TRANSLATION * amount * valueNoise(t + SEED_X),
				MAX_TRANSLATION * amount * valueNoise(t + SEED_Y),
				-MAX_ZOOM_PUNCH * amount + MAX_ZOOM_PUNCH * 0.35f * amount * valueNoise(t + SEED_Z),
			)
			rotation = MAX_ROTATION * amount * valueNoise(t + SEED_ROT)
		} else {
			shake.set(Vector3.Zero)
			rotation = 0f
		}
	}

	fun reset() {
		additive = 0f
		floor = 0f
		rotation = 0f
		shake.set(Vector3.Zero)
	}

	val shakeX: Float get() = shake.x
	val shakeY: Float get() = shake.y
	val shakeZ: Float get() = shake.z

	companion object {
		const val TRAUMA_PER_SHAKE = 0.30f
		const val TRAUMA_DECAY = 1.4f
		const val MAX_TRANSLATION = 22f
		const val MAX_ROTATION = 2.6f
		const val MAX_ZOOM_PUNCH = 0.05f
		const val SHAKE_FREQUENCY = 28f
		const val SHAKE_FALLOFF_DISTANCE = 384f
		const val EXPLOSION_NEAR_AMOUNT = 0.30f
		const val DISCHARGER_NEAR_AMOUNT = 0.15f
		const val AUDIBLE_FLOOR = 0.32f

		private const val SEED_X = 0f
		private const val SEED_Y = 37.3f
		private const val SEED_Z = 71.9f
		private const val SEED_ROT = 113.1f
	}
}

/**
 * Effective audible range of a world event whose sound has [baseRange], in a window [screenWidthPx] pixels wide.
 */
fun audibleShakeRange(baseRange: Float, screenWidthPx: Int): Float = maxOf(baseRange, screenWidthPx * 0.5f)

private fun valueNoise(x: Float): Float {
	val xi = floor(x)
	val f = x - xi
	val u = f * f * (3f - 2f * f)
	val a = hashNoise(xi.toInt())
	val b = hashNoise(xi.toInt() + 1)
	return a + (b - a) * u
}

private fun hashNoise(n: Int): Float {
	var h = n
	h = h xor (h ushr 16)
	h *= -2048144789
	h = h xor (h ushr 13)
	h *= -1028477387
	h = h xor (h ushr 16)
	return h.toFloat() / Int.MAX_VALUE.toFloat()
}
