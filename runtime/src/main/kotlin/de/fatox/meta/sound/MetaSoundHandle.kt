package de.fatox.meta.sound

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

/**
 * Improved MetaSoundHandle supporting fade-out.
 */
class MetaSoundHandle(val definition: MetaSoundDefinition) {
	private val shapeRenderer: ShapeRenderer by lazyInject()
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
		if (handleId == -1L) {
			// Playback has not started yet (deferred start pending) or already stopped —
			// there is nothing to fade; flag done so a pending deferred start never plays.
			setDone()
			return
		}
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
		// Pan is based purely on the definition's audible range (world units) —
		// never on Gdx.graphics pixels, which would make audio falloff DPI/window-size dependent.
		val xPan = soundPos.x - listenerPos.x
		if (definition.audibleRange <= 0f) return 0f
		return MathUtils.clamp(xPan / definition.audibleRange, -0.9f, 0.9f)
	}

	/**
	 * Calculates the volume based on distance falloff from [listenerPos].
	 * If [terminate] is true, handle will setDone if out of audible range.
	 */
	fun calcVolume(listenerPos: Vector2): Float {
		// Falloff uses the definition's audible range (world units) only; see calcPan.
		val audibleRange2 = definition.audibleRange2
		val distSquared = listenerPos.dst2(soundPos)
		val audioVideoData = MetaAudioVideoState.state.value
		val globalVolume = audioVideoData.masterVolume * audioVideoData.soundVolume
		val normalizedDistance = if (audibleRange2 <= 0f) {
			if (distSquared <= 0f) 0f else 1f
		} else {
			MathUtils.clamp(kotlin.math.sqrt(distSquared / audibleRange2), 0f, 1f)
		}
		val gain = MetaSoundFalloff.gain(normalizedDistance, definition.attenuation, definition.distantVolume)
		val volume = definition.volume * gain * globalVolume

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
			val targetVolume = calcVolume(listenerPos)
			val targetPan = calcPan(listenerPos)
			currentVolume = currentVolume.dlerp(targetVolume, 0.5f, delta)
			currentPan = currentPan.dlerp(targetPan, 0.5f, delta)

			// If the sound is non-looping, check if it should be done by time.
			if (!definition.isLooping &&
				TimeUtils.timeSinceMillis(startTime) >= definition.durationMs
			) {
				beginFadeOut() // or setDone() if you prefer an immediate stop
			}
		} else {
			// We're fading out
			currentVolume = currentVolume.dlerp(0f, 0.5f, delta)

			if (currentVolume <= 0.001f) {
				// Fade-out finished; handleId is now invalid, so don't fall through to setPan below.
				stopImmediately()
				setDone()
				return
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
	fun setHandleId(handleId: Long, initialVolume: Float, initialPan: Float) {
		if (isDone) {
			// Handle was stopped before its deferred start delivered an id; kill the instance
			// immediately instead of adopting it, so no untracked playback survives a stop.
			if (handleId != -1L) {
				definition.sound.stop(handleId)
			}
			return
		}
		if (handleId == -1L) {
			log.debug("HandleId is -1 – sound failed to play or invalid handle!")
		}
		this.handleId = handleId
		// Preserve the volume used to start playback. Resetting to base volume here made distant sounds surge toward
		// full loudness on their first dynamic update.
		currentVolume = initialVolume
		currentPan = initialPan
	}
}
