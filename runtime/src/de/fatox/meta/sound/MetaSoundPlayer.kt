package de.fatox.meta.sound

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.TimeUtils
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.assets.MetaData
import de.fatox.meta.assets.get
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

class MetaSoundPlayer {
	private val soundDefinitions = ObjectMap<String, MetaSoundDefinition>()
	private val playingHandles = ObjectMap<MetaSoundDefinition, Array<MetaSoundHandle>>()
	private val dynamicHandles = Array<MetaSoundHandle>()

	private val metaAssetProvider: AssetProvider by lazyInject()
	private val shapeRenderer: ShapeRenderer by lazyInject()
	private val spriteBatch: SpriteBatch by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()
	private val metaData: MetaData by lazyInject()

	fun playSound(soundDefinition: MetaSoundDefinition?, listenerPos: Vector2? = null, soundPos: Vector2 = Vector2.Zero): MetaSoundHandle? {
		if (soundDefinition == null) return null
		val audioVideoData: MetaAudioVideoData = metaData["audioVideoData"]
		val volume = audioVideoData.masterVolume * audioVideoData.soundVolume
		if (volume <= 0) {
			return null
		}
		if (listenerPos != null && !isInAudibleRange(soundDefinition, listenerPos, soundPos)) {
			return null
		}
		if (!playingHandles.containsKey(soundDefinition)) {
			// Create handlelist if sound is played for the first time
			playingHandles.put(soundDefinition, Array(soundDefinition.maxInstances))
		}
		val handleList = playingHandles.get(soundDefinition)
		cleanupHandles(handleList)
		if (handleList.size >= soundDefinition.maxInstances || handleList.size > 0 && handleList.first().startTime + 200 >= TimeUtils.millis()) {
			return null
		}
		if (soundDefinition.sound === UninitializedSound) {
			// Load sound if it is played for the first time
			val sound = Gdx.audio.newSound(metaAssetProvider.getResource(soundDefinition.soundName, FileHandle::class.java))
			soundDefinition.sound = sound
		}
		// Play or loop sound
		val soundHandle = MetaSoundHandle(soundDefinition)
		soundHandle.soundPos = soundPos
		if (listenerPos != null) {
			val mappedVolume = soundHandle.calcVolume(listenerPos, false)
			val mappedPan = soundHandle.calcPan(listenerPos)
			val id = if (soundDefinition.isLooping) soundDefinition.sound.loop(mappedVolume, 1f, mappedPan) else soundDefinition.sound.play(mappedVolume, 1f, mappedPan)
			soundHandle.setHandleId(id)
			dynamicHandles.add(soundHandle)
		} else {
			val id = if (soundDefinition.isLooping) soundDefinition.sound.loop(volume, 1f, 0f) else soundDefinition.sound.play(volume, 1f, 0f)
			soundHandle.setHandleId(id)
		}
		handleList.add(soundHandle)
		return soundHandle
	}

	private fun cleanupHandles(handleList: Array<MetaSoundHandle>) {
		val iterator: MutableIterator<MetaSoundHandle> = handleList.iterator()
		while (iterator.hasNext()) {
			val next = iterator.next()
			if (next.isDone || !next.isPlaying) {
				stopSound(next)
				iterator.remove()
			}
		}
	}

	private fun isInAudibleRange(sound: MetaSoundDefinition, listenerPosition: Vector2, soundPosition: Vector2): Boolean {
		return listenerPosition.dst2(soundPosition) <= sound.soundRange2 || soundInScreen(soundPosition)
	}

	private fun soundInScreen(soundPosition: Vector2): Boolean {
		helper.set(soundPosition.x, soundPosition.y, 0f)
		val project = uiRenderer.getCamera().project(helper)
		return project.x > 0 && project.x < Gdx.graphics.width && project.y > 0 && project.y < Gdx.graphics.height
	}

	fun playSound(path: String, listenerPosition: Vector2? = null, soundPosition: Vector2 = Vector2.Zero): MetaSoundHandle? {
		if (!soundDefinitions.containsKey(path)) {
			soundDefinitions.put(path, MetaSoundDefinition(path))
		}
		return playSound(soundDefinitions.get(path), listenerPosition, soundPosition)
	}

	fun updateDynamicSounds(listenerPos: Vector2) {
		val iterator: Iterator<MetaSoundHandle> = dynamicHandles.iterator()
		while (iterator.hasNext()) {
			val soundHandle = iterator.next()
			if (soundHandle.isDone || !soundHandle.isPlaying) {
				stopSound(soundHandle)
			} else {
				soundHandle.calcVolAndPan(listenerPos)
			}
		}
	}

	/**
	 * Debug-renders all dynamic sound instances
	 */
	fun debugRender() {
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
		shapeRenderer.projectionMatrix = spriteBatch.projectionMatrix
		shapeRenderer.transformMatrix = spriteBatch.transformMatrix
		for (soundHandle in dynamicHandles) {
			soundHandle.debugRender()
		}
		shapeRenderer.end()
	}

	fun stopSound(soundHandle: MetaSoundHandle?) {
		if (soundHandle != null) {
			soundHandle.stop()
			soundHandle.setDone()
			dynamicHandles.removeValue(soundHandle, true)
		}
	}

	fun stopAllSounds() {
		for (soundHandles in playingHandles.values()) {
			for (soundHandle in soundHandles) {
				soundHandle.stop()
				soundHandle.setDone()
			}
			soundHandles.clear()
		}
		dynamicHandles.clear()
	}

	companion object {
		private val helper = Vector3()
	}
}