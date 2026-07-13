package de.fatox.meta.sound

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Timer
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.getOrPut
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.assets.MetaData
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.min
import kotlin.math.sqrt

private val log = MetaLoggerFactory.logger {}
private val soundSourceMap = ObjectMap<MetaPositionalSoundDefinition, MetaSoundClustering>()
private var focusVolume = 1f

fun addSoundSource(soundDefinition: MetaPositionalSoundDefinition, position: Vector2): MetaSoundSource {
	val source = MetaSoundSource(soundDefinition, position)
	val clustering = soundSourceMap.getOrPut(source.definition) { MetaSoundClustering(source.definition) }
	clustering.sources.add(source)
	return source
}

fun removeSoundSource(source: MetaSoundSource) {
	val soundClustering = soundSourceMap[source.definition] ?: return
	soundClustering.sources.removeValue(source, true)
	if (soundClustering.sources.size == 0) {
		for (i in soundClustering.clusters.size - 1 downTo 0) {
			soundClustering.clusters[i].stopNow()
		}
		soundClustering.clusters.clear()
		soundClustering.sourcesNear.clear()
		soundSourceMap.remove(source.definition)
	}
}

fun silencePositionalSounds(flag: Boolean) {
	focusVolume = if (flag) 0f else 1f
}

fun updateSoundSources(listenerPos: Vector2, delta: Float) {
	for (value in soundSourceMap.values()) {
		value.update(listenerPos, delta)
	}
}

fun stopAllSoundSources() {
	for (value in soundSourceMap.values()) {
		for (i in value.clusters.size - 1 downTo 0) {
			value.clusters[i].stopNow()
			value.clusters[i].sources.clear()
		}
		value.clusters.clear()
		value.sources.clear()
		value.sourcesNear.clear()
	}
	soundSourceMap.clear()
}

/** Backwards-compatible alias for callers that only manage positional loop sources through this API. */
fun stopAllSounds() {
	stopAllSoundSources()
}

/** Backwards-compatible no-op. Positional sources now read Meta's live master*sound volume every update. */
@Suppress("UNUSED_PARAMETER")
fun loadSoundVolume(metaData: MetaData) = Unit

class MetaPositionalSoundDefinition(
	soundName: String,
	val maxSources: Int = 3,
	val audibleRange: Float = 600f,
) {
	private val assetProvider: AssetProvider by lazyInject()
	val soundName: String = soundName.replace("/".toRegex(), "\\\\")
	val audibleRange2 = audibleRange * audibleRange
	var volume = 1f
	var randomPitchRange = 0.06f
	var attenuation = MetaSoundAttenuation.SMOOTH
	/** Fraction of base volume retained at [audibleRange]; use a small tail only for globally relevant events. */
	var distantVolume = 0f

	val sound: Sound by lazy {
		runCatching {
			Gdx.audio.newSound(assetProvider.getResource(this.soundName, FileHandle::class.java))
		}.onFailure {
			log.error("Unable to load positional sound '$soundName'; continuing with silent fallback", it)
		}.getOrDefault(UninitializedSound)
	}
}

enum class MetaSoundAttenuation {
	LINEAR,
	SMOOTH,
	INVERSE,
}

/** Shared allocation-free distance curve for positional one-shots and clustered loop sources. */
internal object MetaSoundFalloff {
	fun gain(normalizedDistance: Float, attenuation: MetaSoundAttenuation, distantVolume: Float): Float {
		val t = MathUtils.clamp(normalizedDistance, 0f, 1f)
		val nearGain = when (attenuation) {
			MetaSoundAttenuation.LINEAR -> 1f - t
			MetaSoundAttenuation.SMOOTH -> {
				val smooth = 1f - t * t * (3f - 2f * t)
				// A second smooth pass removes the overly-present middle/far field without crushing nearby detail.
				smooth * smooth
			}
			MetaSoundAttenuation.INVERSE -> {
				val rolloff = 3f
				(1f / (1f + rolloff * t * t) - 1f / (1f + rolloff)) / (1f - 1f / (1f + rolloff))
			}
		}
		val tail = MathUtils.clamp(distantVolume, 0f, 1f)
		return tail + nearGain * (1f - tail)
	}
}

class MetaSoundSource(
	val definition: MetaPositionalSoundDefinition,
	val soundPosReference: Vector2 = Vector2.Zero.cpy(),
	var silenced: Boolean = false,
)

class MetaSoundCluster(
	val definition: MetaPositionalSoundDefinition,
	val centroid: Vector2 = Vector2.Zero.cpy(),
) {
	private var handleId: Long = -1L
	val sources = Array<MetaSoundSource>()
	private var currentPan = 0f
	private var currentVolume = 0f
	private var starting = false
	private var started = false
	val centroidLerp = centroid.cpy()

	fun update(listenerPos: Vector2, delta: Float) {
		centroidLerp.dlerp(centroid, 0.25f, delta)
		if (effectiveBaseVolume() <= 0f) {
			stop()
			return
		}
		if (!starting && !started && calcVolume(listenerPos) > 0f) start()
		if (started) calcVolAndPan(listenerPos, delta)
	}

	private fun calcPan(listenerPos: Vector2): Float {
		// Use the smoothed centroid so pan doesn't jump when re-clustering moves the raw centroid.
		val xPan = centroidLerp.x - listenerPos.x
		if (definition.audibleRange <= 0f) return 0f
		return MathUtils.clamp(xPan / definition.audibleRange, -1f, 1f) * 0.90f
	}

	private fun effectiveBaseVolume(): Float {
		if (definition.volume <= 0f || focusVolume <= 0f) return 0f
		val audioVideoData = MetaAudioVideoState.state.value
		return definition.volume * audioVideoData.masterVolume * audioVideoData.soundVolume * focusVolume
	}

	private fun calcVolume(listenerPos: Vector2): Float {
		// Use the smoothed centroid so volume doesn't jump when re-clustering moves the raw centroid.
		val distance = sqrt(listenerPos.dst2(centroidLerp))
		val t = if (definition.audibleRange <= 0f) {
			if (distance <= 0f) 0f else 1f
		} else {
			MathUtils.clamp(distance / definition.audibleRange, 0f, 1f)
		}
		val attenuated = MetaSoundFalloff.gain(t, definition.attenuation, definition.distantVolume)
		return attenuated * effectiveBaseVolume()
	}

	private fun calcVolAndPan(listenerPos: Vector2, delta: Float) {
		currentPan = currentPan.dlerp(calcPan(listenerPos), 0.25f, delta)
		val targetVolume = calcVolume(listenerPos)
		currentVolume = currentVolume.dlerp(targetVolume, 0.25f, delta)
		if (targetVolume <= 0f && currentVolume <= FADE_STOP_VOLUME) {
			stop()
			return
		}
		definition.sound.setPan(handleId, MathUtils.clamp(currentPan, -1f, 1f), MathUtils.clamp(currentVolume, 0f, 1f))
	}

	fun updateCentroid() {
		var sumX = 0f
		var sumY = 0f
		for (i in 0 until sources.size) {
			sumX += sources[i].soundPosReference.x
			sumY += sources[i].soundPosReference.y
		}
		centroid.x = sumX / sources.size
		centroid.y = sumY / sources.size
	}

	private fun start() {
		starting = true
		Timer.schedule(object : Timer.Task() {
			override fun run() {
				if (!starting) return
				Gdx.app.postRunnable {
					if (!starting || effectiveBaseVolume() <= 0f) {
						starting = false
						return@postRunnable
					}
					val pitch = 1f + MathUtils.random(-definition.randomPitchRange, definition.randomPitchRange)
					handleId = definition.sound.loop(0f, pitch, 0f)
					starting = false
					started = true
				}
			}

			override fun cancel() {
				super.cancel()
				starting = false
			}
		}, MathUtils.random(0.01f, 0.5f))
	}

	fun stop() {
		starting = false
		if (!started) return
		definition.sound.stop(handleId)
		started = false
		handleId = -1L
		currentVolume = 0f
	}

	fun stopNow() {
		stop()
	}

	private companion object {
		// Only used to finish a loop that is already fading toward exact silence; positive target volumes are never cut.
		const val FADE_STOP_VOLUME = 0.0025f
	}
}

class MetaSoundClustering(val definition: MetaPositionalSoundDefinition) {
	val sources = Array<MetaSoundSource>()
	val sourcesNear = Array<MetaSoundSource>()
	val clusters = Array<MetaSoundCluster>()

	private fun moveOutwards(vector: Vector2, radius: Float): Vector2 {
		val randomAngle = MathUtils.random(0f, 2f * MathUtils.PI)
		vector.x += MathUtils.cos(randomAngle) * radius
		vector.y += MathUtils.sin(randomAngle) * radius
		return vector
	}

	fun update(listenerPos: Vector2, delta: Float) {
		sourcesNear.clear()
		for (i in 0 until sources.size) {
			val source = sources[i]
			if (!source.silenced && listenerPos.dst2(source.soundPosReference) < definition.audibleRange2) {
				sourcesNear.add(source)
			}
		}

		val clusterCount = min(sourcesNear.size, definition.maxSources)
		if (clusterCount == 0 && clusters.size == 0) return

		for (i in clusters.size - 1 downTo 0) {
			val cluster = clusters[i]
			if (cluster.sources.size == 0 || cluster.centroid.dst2(listenerPos) > definition.audibleRange2) {
				cluster.stop()
				clusters.removeIndex(i)
			}
		}

		while (clusters.size > clusterCount) {
			clusters.removeIndex(clusters.size - 1).stop()
		}

		val clustersToAdd = clusterCount - clusters.size
		for (i in 0 until clustersToAdd) {
			clusters.add(MetaSoundCluster(definition, moveOutwards(listenerPos.cpy(), definition.audibleRange * 0.5f)))
		}

		for (i in 0 until 2) {
			doClustering()
		}

		for (i in clusters.size - 1 downTo 0) {
			val cluster = clusters[i]
			if (cluster.sources.size == 0 || cluster.centroid.dst2(listenerPos) > definition.audibleRange2) {
				cluster.stop()
				clusters.removeIndex(i)
			} else {
				cluster.update(listenerPos, delta)
			}
		}
	}

	private fun doClustering() {
		for (i in 0 until clusters.size) {
			clusters[i].sources.clear()
		}
		if (sourcesNear.size <= clusters.size) {
			for (i in 0 until sourcesNear.size) {
				clusters[i].sources.add(sourcesNear[i])
				clusters[i].centroid.set(sourcesNear[i].soundPosReference)
				clusters[i].centroidLerp.set(sourcesNear[i].soundPosReference)
			}
			return
		}

		for (i in 0 until sourcesNear.size) {
			var closestClusterIndex = 0
			var closestDistance = Float.MAX_VALUE
			for (j in 0 until clusters.size) {
				val distance = sourcesNear[i].soundPosReference.dst2(clusters[j].centroid)
				if (distance < closestDistance) {
					closestDistance = distance
					closestClusterIndex = j
				}
			}
			clusters[closestClusterIndex].sources.add(sourcesNear[i])
		}

		for (i in 0 until clusters.size) {
			clusters[i].updateCentroid()
		}
	}
}
