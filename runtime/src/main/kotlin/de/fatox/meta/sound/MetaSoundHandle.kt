package de.fatox.meta.sound

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.audioVideoDataKey
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.max
import kotlin.math.pow

/**
 * Improved MetaSoundHandle supporting fade-out.
 */
class MetaSoundHandle(val definition: MetaSoundDefinition) {
	private val shapeRenderer: ShapeRenderer by lazyInject()
	private val metaData: MetaData by lazyInject()
	private val audioVideoData: MetaAudioVideoData = metaData[audioVideoDataKey]
	private val log = MetaLoggerFactory.logger {}

	// Internal state
	private var handleId: Long = -1L

	// Current volume/pan (will update gradually).
	private var currentVolume = 0f
	private var currentPan = 0f

	// Fading out
	private var isFadingOut = false

	// For 2D positioning
	var soundPos: Vector2 = Vector2.Zero.cpy()
		set(value) {
			field.set(value)
		}

	val startTime: Long = TimeUtils.millis()

	/**
	 * Whether the sound is considered done:
	 *   - For non-looping sounds, after the duration
	 *   - Or after fade-out completes
	 *   - Or forcibly flagged done
	 */
	var isDone: Boolean = false
		private set

	/**
	 * Begin fade-out sequence; the handle will remain "active" until fade-out finishes.
	 */
	fun beginFadeOut() {
		if (!isFadingOut) {
			isFadingOut = true
		}
	}

	/**
	 * Stops the sound immediately.
	 * Typically called once the fade-out has completed or if we want an instant stop.
	 */
	fun stopImmediately() {
		if (handleId != -1L) {
			definition.sound.stop(handleId)
			handleId = -1
		}
	}

	/**
	 * Flags the handle as done, so the player can remove it.
	 */
	fun setDone() {
		isDone = true
	}

	/**
	 * Convenience property to check if handle is active in the audio engine.
	 */
	val isPlaying: Boolean
		get() = !isDone && handleId != -1L

	fun calcPan(listenerPos: Vector2): Float {
		val audibleRange = max(definition.audibleRange, Gdx.graphics.width * 0.5f)
		val xPan = soundPos.x - listenerPos.x
		return MathUtils.clamp(xPan / audibleRange, -0.9f, 0.9f)
	}

	/**
	 * Calculates the volume based on distance falloff from [listenerPos].
	 * If [terminate] is true, handle will setDone if out of audible range.
	 */
	fun calcVolume(listenerPos: Vector2, terminate: Boolean): Float {
		val audibleRange2 = max(
			definition.audibleRange2,
			Gdx.graphics.width * 1f * Gdx.graphics.width
		)
		val distSquared = listenerPos.dst2(soundPos)
		val globalVolume = audioVideoData.masterVolume * audioVideoData.soundVolume
		val volume = definition.volume * MathUtils.clamp(1 - distSquared / audibleRange2, 0f, 1f) * globalVolume

		if (terminate && distSquared > audibleRange2) {
			setDone()
		}
		return volume
	}

	/**
	 * Called each frame for positional sounds.
	 * [delta] is time since last update in seconds.
	 */
	fun calcVolAndPan(listenerPos: Vector2, delta: Float) {
		if (isDone || handleId == -1L) return

		if (!isFadingOut) {
			// Normal volume/pan update
			val targetVolume = calcVolume(listenerPos, terminate = false)
			val targetPan = calcPan(listenerPos)
			currentVolume = currentVolume.dlerp(targetVolume, 0.5f, delta)
			currentPan = currentPan.dlerp(targetPan, 0.5f, delta)

			// If the sound is non-looping, check if it should be done by time.
			if (!definition.isLooping &&
				TimeUtils.timeSinceMillis(startTime) >= definition.duration
			) {
				beginFadeOut() // or setDone() if you prefer an immediate stop
			}
		} else {
			// We're fading out
			currentVolume = currentVolume.dlerp(0f, 0.5f, delta)

			if (currentVolume <= 0.001f) {
				// Fade-out finished
				stopImmediately()
				setDone()
			}
		}

		// Update the pan and volume to the actual sound instance.
		definition.sound.setPan(handleId, currentPan, currentVolume)
	}

	/**
	 * Debug visualization for the handle's position.
	 */
	fun debugRender() {
		shapeRenderer.color = Color.CHARTREUSE
		shapeRenderer.circle(soundPos.x + 16, soundPos.y + 16, 14f)
	}

	/**
	 * Sets the LibGDX sound handle ID.
	 */
	fun setHandleId(handleId: Long) {
		if (handleId == -1L) {
			log.error("HandleId is -1 – sound failed to play or invalid handle!")
		}
		this.handleId = handleId
		// When first set, we assume the initial volume is the full volume, so let's do a quick calc:
		currentVolume = definition.volume * audioVideoData.masterVolume * audioVideoData.soundVolume
	}
}
