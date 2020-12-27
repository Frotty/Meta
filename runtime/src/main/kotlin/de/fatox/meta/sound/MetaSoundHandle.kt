package de.fatox.meta.sound

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

class MetaSoundHandle(private val definition: MetaSoundDefinition) {
	private val shapeRenderer: ShapeRenderer by lazyInject()
	private val metaData: MetaData by lazyInject()

	private val audioVideoData: MetaAudioVideoData
	private var handleId: Long = 0

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
		val audibleRange = Gdx.graphics.height * 0.4f
		val xPan = soundPos.x - listenerPos.x
		return MathUtils.clamp(xPan / audibleRange, -1f, 1f) * 0.90f
	}

	fun calcVolume(listenerPos: Vector2, terminate: Boolean): Float {
		val audibleRange = Gdx.graphics.height * 0.4f
		val audibleRangeSquared = audibleRange * audibleRange
		val distSquared = listenerPos.dst2(soundPos)
		val volumeMod = audioVideoData.masterVolume * audioVideoData.soundVolume
		if (terminate && distSquared > audibleRangeSquared) {
			setDone()
		}
		return volumeMod * definition.volume * MathUtils.clamp(1 - distSquared / audibleRangeSquared, 0f, 1f)
	}

	fun calcVolAndPan(listenerPos: Vector2) {
		definition.sound.setPan(handleId, calcPan(listenerPos), calcVolume(listenerPos, true))
	}

	fun stop() {
		definition.sound.stop(handleId)
	}

	fun debugRender() {
		shapeRenderer.color = Color.CHARTREUSE
		shapeRenderer.circle(soundPos.x + 16, soundPos.y + 16, 14f)
	}

	fun setHandleId(handleId: Long) {
		this.handleId = handleId
	}

	init {
		if (!isPlaying) {
			stop()
		}
		audioVideoData = metaData["audioVideoData"]
	}
}