package de.fatox.meta.ui

import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.computed
import de.fatox.meta.reactive.signal

/**
 * Reactive state for a full-screen fade whose destination is a value rather than a callback.
 *
 * Call [advance] once per rendered frame and use [alpha] for a black overlay. A screen owns the navigation decision
 * in one lifecycle-scoped effect by observing [finished]; the transition never captures the screen or changes it.
 *
 * ```kotlin
 * val transition = MetaScreenTransition<ScreenId>()
 * reactiveScope.effect("screen transition") {
 *     transition.finished()?.let { game.screen = screens[it] }
 * }
 *
 * transition.fadeOutTo(ScreenId.GAME)
 * // render: transition.advance(delta); overlay.color.a = transition.alpha()
 * ```
 */
class MetaScreenTransition<T : Any> @JvmOverloads constructor(
	val durationSeconds: Float = DEFAULT_DURATION_SECONDS,
) {
	init {
		require(durationSeconds.isFinite() && durationSeconds > 0f) {
			"A screen transition duration must be finite and greater than zero"
		}
	}

	enum class Phase {
		FADING_IN,
		IDLE,
		FADING_OUT,
		COVERED,
	}

	private val phaseSignal = signal(Phase.IDLE)
	private val pendingSignal = signal<T?>(null)
	// Advanced every frame and observed only by alpha(), so a primitive field is the correct hot-loop representation.
	// Phase and destination changes are the coarse state that presentation effects need to observe reactively.
	private var elapsed = 0f

	val phase: ReactiveValue<Phase> = phaseSignal

	/** The destination waiting for the fade to cover the screen, or null. */
	val pending: ReactiveValue<T?> = pendingSignal

	/** True only while opacity is moving; a covered screen is a stable state. */
	val busy: ReactiveValue<Boolean> = computed {
		val phase = phaseSignal()
		phase == Phase.FADING_IN || phase == Phase.FADING_OUT
	}

	/** The destination once the screen is fully covered, otherwise null. */
	val finished: ReactiveValue<T?> = computed {
		if (phaseSignal() == Phase.COVERED) pendingSignal() else null
	}

	/** Current overlay opacity: zero is transparent and one is fully covered. */
	fun alpha(): Float {
		val progress = (elapsed / durationSeconds).coerceIn(0f, 1f)
		return when (phaseSignal.peek()) {
			Phase.FADING_IN -> 1f - progress
			Phase.IDLE -> 0f
			Phase.FADING_OUT -> progress
			Phase.COVERED -> 1f
		}
	}

	/** Starts fully covered and reveals the current screen. Clears any prior destination. */
	fun fadeIn() = batch {
		elapsed = 0f
		pendingSignal.value = null
		phaseSignal.value = Phase.FADING_IN
	}

	/** Holds a black cover immediately, without publishing a completed destination. */
	fun holdCovered() = batch {
		elapsed = 0f
		pendingSignal.value = null
		phaseSignal.value = Phase.COVERED
	}

	/**
	 * Fades to [destination]. Returns false unless the transition is idle, so an entry fade cannot jump abruptly to
	 * transparent or replace a destination that has already landed.
	 */
	fun fadeOutTo(destination: T): Boolean {
		val phase = phaseSignal.peek()
		if (phase != Phase.IDLE) return false
		batch {
			elapsed = 0f
			pendingSignal.value = destination
			phaseSignal.value = Phase.FADING_OUT
		}
		return true
	}

	/** Cancels any transition and returns immediately to a transparent idle state. */
	fun cancel() = batch {
		elapsed = 0f
		pendingSignal.value = null
		phaseSignal.value = Phase.IDLE
	}

	/** Advances a running fade. Non-positive or non-finite frame deltas are ignored. */
	fun advance(deltaSeconds: Float) {
		if (!deltaSeconds.isFinite() || deltaSeconds <= 0f) return
		val phase = phaseSignal.peek()
		if (phase == Phase.IDLE || phase == Phase.COVERED) return
		val next = elapsed + deltaSeconds
		if (next < durationSeconds) {
			elapsed = next
			return
		}
		elapsed = durationSeconds
		phaseSignal.value = if (phase == Phase.FADING_OUT) Phase.COVERED else Phase.IDLE
	}

	companion object {
		const val DEFAULT_DURATION_SECONDS = 0.55f
	}
}
