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
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.audioVideoDataKey
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.min
import kotlin.math.sqrt

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

	val sound: Sound by lazy {
		Gdx.audio.newSound(assetProvider.getResource(soundName, FileHandle::class.java))
	}
}

enum class MetaSoundAttenuation {
	LINEAR,
	SMOOTH,
	INVERSE,
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
	private val metaData: MetaData by lazyInject()
	private var handleId: Long = -1L
	val sources = Array<MetaSoundSource>()
	private var currentPan = 0f
	private var currentVolume = 0f
	private var starting = false
	private var started = false
	val centroidLerp = centroid.cpy()

	fun update(listenerPos: Vector2, delta: Float) {
		centroidLerp.dlerp(centroid, 0.25f, delta)
		if (!starting && !started) start()
		if (started) calcVolAndPan(listenerPos, delta)
	}

	private fun calcPan(listenerPos: Vector2): Float {
		val xPan = centroid.x - listenerPos.x
		return MathUtils.clamp(xPan / definition.audibleRange, -1f, 1f) * 0.90f
	}

	private fun calcVolume(listenerPos: Vector2): Float {
		val distance = sqrt(listenerPos.dst2(centroid))
		val t = MathUtils.clamp(distance / definition.audibleRange, 0f, 1f)
		val attenuated = when (definition.attenuation) {
			MetaSoundAttenuation.LINEAR -> 1f - t
			MetaSoundAttenuation.SMOOTH -> {
				val smooth = t * t * (3f - 2f * t)
				1f - smooth
			}
			MetaSoundAttenuation.INVERSE -> {
				val rolloff = 3f
				(1f / (1f + rolloff * t * t) - 1f / (1f + rolloff)) / (1f - 1f / (1f + rolloff))
			}
		}
		val audioVideoData = metaData[audioVideoDataKey]
		return attenuated * definition.volume * audioVideoData.masterVolume * audioVideoData.soundVolume * focusVolume
	}

	private fun calcVolAndPan(listenerPos: Vector2, delta: Float) {
		currentPan = currentPan.dlerp(calcPan(listenerPos), 0.25f, delta)
		currentVolume = currentVolume.dlerp(calcVolume(listenerPos), 0.25f, delta)
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
	}

	fun stopNow() {
		stop()
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
