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

class MetaSoundHandle(private val definition: MetaSoundDefinition) {
	private val shapeRenderer: ShapeRenderer by lazyInject()
	private val metaData: MetaData by lazyInject()

	private val audioVideoData: MetaAudioVideoData
	private var handleId: Long = -1L
	private var volumeTarget = 0f
	private var volumeCurrent = 0f
	private var panTarget = 0f
	private var panCurrent = 0f

	private var log = MetaLoggerFactory.logger {}

	// For 2d positioning
	var soundPos: Vector2 = Vector2.Zero.cpy()
		set(value) {
			this.soundPos.set(value)
		}

	val startTime: Long = TimeUtils.millis()

	var isDone: Boolean = false
		get() = field || !definition.isLooping && TimeUtils.timeSinceMillis(startTime) > definition.duration
		private set

	fun setDone() {
		isDone = true
		stop()
	}

	val isPlaying: Boolean = true

	fun calcPan(listenerPos: Vector2): Float {
		val audibleRange = max(definition.audibleRange, Gdx.graphics.width * 0.5f)
		val xPan = soundPos.x - listenerPos.x
		return MathUtils.clamp(xPan / audibleRange, -0.9f, 0.9f)
	}

	fun calcVolume(listenerPos: Vector2, terminate: Boolean): Float {
		val audibleRange2 = max(definition.audibleRange2, Gdx.graphics.width * 1f * Gdx.graphics.width)
		val distSquared = listenerPos.dst2(soundPos)
		val volumeMod = audioVideoData.masterVolume * audioVideoData.soundVolume
		if (terminate && distSquared > audibleRange2) {
			setDone()
		}
		return volumeMod * definition.volume * MathUtils.clamp(1 - distSquared / audibleRange2, 0f, 1f)
	}

	fun calcVolAndPan(listenerPos: Vector2, delta: Float) {
		if (handleId != -1L) {
			if (delta == -1f) {
				volumeCurrent = calcVolume(listenerPos, false)
				volumeTarget = volumeCurrent
				panCurrent = calcPan(listenerPos)
				panTarget = panCurrent
			} else {
				volumeTarget = calcVolume(listenerPos, false)
				volumeCurrent = volumeCurrent.dlerp(volumeTarget, 0.5f, delta)
				panTarget = calcPan(listenerPos)
				panCurrent = panCurrent.dlerp(panTarget, 0.5f, delta)
			}
			definition.sound.setPan(handleId, panCurrent, volumeCurrent)
		}
	}

	fun stop() {
		if (handleId != -1L) {
			definition.sound.stop(handleId)
			handleId = -1
		}
	}

	fun debugRender() {
		shapeRenderer.color = Color.CHARTREUSE
		shapeRenderer.circle(soundPos.x + 16, soundPos.y + 16, 14f)
	}

	fun setHandleId(handleId: Long) {
		if (handleId == -1L) {
			log.error("HandleId is -1")
		}
		this.handleId = handleId
	}

	init {
		if (!isPlaying) {
			stop()
		}
		audioVideoData = metaData[audioVideoDataKey]
	}
}