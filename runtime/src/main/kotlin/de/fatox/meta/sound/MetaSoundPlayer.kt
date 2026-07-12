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
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.api.extensions.getOrPut
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import kotlin.math.max

/**
 * Improved MetaSoundPlayer with fade-out support on sound stops.
 */
class MetaSoundPlayer {
	private val soundDefinitions = ObjectMap<String, MetaSoundDefinition>()
	private val playingHandles = ObjectMap<MetaSoundDefinition, Array<MetaSoundHandle>>()
	private val dynamicHandles = Array<MetaSoundHandle>()

	private val metaAssetProvider: AssetProvider by lazyInject()
	private val shapeRenderer: ShapeRenderer by lazyInject()
	private val spriteBatch: SpriteBatch by lazyInject()
	private val uiRenderer: UIRenderer by lazyInject()

	var soundsSilenced = false

	/**
	 * Plays a sound, optionally positional (with [listenerPos] and [soundPos]).
	 * [minimumPause] is in milliseconds.
	 */
	fun playSound(
		soundDefinition: MetaSoundDefinition?,
		listenerPos: Vector2? = null,
		minimumPause: Float = 200f,
		soundPos: Vector2 = Vector2.Zero
	): MetaSoundHandle? {
		if (soundDefinition == null || soundsSilenced) return null

		val audioVideoData = MetaAudioVideoState.state.value
		val volume = audioVideoData.masterVolume * audioVideoData.soundVolume
		if (volume <= 0f) return null

		// If out of audible range, skip.
		if (listenerPos != null && !isInAudibleRange(soundDefinition, listenerPos, soundPos)) {
			return null
		}

		val handleList = playingHandles.getOrPut(soundDefinition) { Array(soundDefinition.maxInstances) }
		cleanupHandles(handleList)

		// Ensure we don't spam the same sound within minimumPause time
		// (this is a simple approach; you can refine logic as needed).
		if (handleList.size > 0) {
			// Handles are appended in play order, so the last entry is the most recent play.
			// (first() would be the OLDEST live handle, disabling the check while older instances exist.)
			val lastPlayTime = handleList.peek().startTime
			if (TimeUtils.timeSinceMillis(lastPlayTime) < minimumPause) {
				return null
			}
		}

		// If maximum concurrent instances is reached, fade out the furthest one.
		if (handleList.size >= soundDefinition.maxInstances) {
			var furthest: MetaSoundHandle? = null
			listenerPos?.let { lp ->
				for (i in 0 until handleList.size) {
					val handle = handleList[i]
					if (furthest == null ||
						handle.soundPos.dst2(lp) > furthest.soundPos.dst2(lp)
					) {
						furthest = handle
					}
				}
			}
			if (furthest != null) {
				// Fade out the furthest instance to free a slot.
				stopSound(furthest, fadeOut = true)
				handleList.removeValue(furthest, true)
			} else {
				return null
			}
		}

		// Load sound if uninitialized.
		if (soundDefinition.sound === UninitializedSound) {
			soundDefinition.sound = Gdx.audio.newSound(
				metaAssetProvider.getResource(
					soundDefinition.soundName,
					FileHandle::class.java
				)
			)
		}

		// Create and populate handle.
		val soundHandle = MetaSoundHandle(soundDefinition)
		soundHandle.soundPos = soundPos

		Gdx.app.postRunnable {
			// stopSound/stopAllSounds may have run between playSound and this deferred start;
			// never start playback on a handle that was already stopped (would leak an untracked loop).
			if (soundHandle.isDone) return@postRunnable
			// If positional, compute initial volume/pan. Otherwise play at global volume.
			if (listenerPos != null) {
				val mappedVolume = max(0.001f, soundHandle.calcVolume(listenerPos))
				val mappedPan = soundHandle.calcPan(listenerPos)
				val id = if (soundDefinition.isLooping)
					soundDefinition.sound.loop(mappedVolume, 1f, mappedPan)
				else
					soundDefinition.sound.play(mappedVolume, 1f, mappedPan)
				soundHandle.setHandleId(id)
				dynamicHandles.add(soundHandle)
			} else {
				val id = if (soundDefinition.isLooping)
					soundDefinition.sound.loop(volume, 1f, 0f)
				else
					soundDefinition.sound.play(volume, 1f, 0f)
				soundHandle.setHandleId(id)
			}
		}
		handleList.add(soundHandle)
		return soundHandle
	}

	/**
	 * Removes finished or invalid handles from a handle list.
	 */
	private fun cleanupHandles(handleList: Array<MetaSoundHandle>) {
		for (i in handleList.size - 1 downTo 0) {
			val soundHandle = handleList[i]
			// Looping handles never expire by time; they are only evicted once explicitly stopped/done.
			// Time-evicting them would orphan an infinite OpenAL loop that stopAllSounds could no longer reach.
			if (soundHandle.isDone ||
				(!soundHandle.definition.isLooping &&
					TimeUtils.timeSinceMillis(soundHandle.startTime) > soundHandle.definition.durationMs)
			) {
				// remove if finished
				handleList.removeIndex(i)
			}
		}
	}

	private fun isInAudibleRange(
		sound: MetaSoundDefinition,
		listenerPosition: Vector2,
		soundPosition: Vector2
	): Boolean {
		return listenerPosition.dst2(soundPosition) <= sound.audibleRange2
			|| soundInScreen(soundPosition)
	}

	private fun soundInScreen(soundPosition: Vector2): Boolean {
		helper.set(soundPosition.x, soundPosition.y, 0f)
		val project = uiRenderer.getCamera().project(helper)
		return project.x > 0 && project.x < Gdx.graphics.width
			&& project.y > 0 && project.y < Gdx.graphics.height
	}

	/**
	 * Convenience method to play a sound by file path.
	 */
	fun playSound(
		path: String,
		listenerPosition: Vector2? = null,
		soundPosition: Vector2 = Vector2.Zero
	): MetaSoundHandle? {
		if (!soundDefinitions.containsKey(path)) {
			soundDefinitions.put(path, MetaSoundDefinition(path))
		}
		return playSound(soundDefinitions.get(path), listenerPosition, soundPos = soundPosition)
	}

	/**
	 * Update dynamic (positional) sounds each frame.
	 */
	fun updateDynamicSounds(listenerPos: Vector2, delta: Float) {
		for (i in dynamicHandles.size - 1 downTo 0) {
			val soundHandle = dynamicHandles[i]
			if (soundHandle.isDone) {
				dynamicHandles.removeIndex(i)
			} else {
				soundHandle.calcVolAndPan(listenerPos, delta)
				// If handle just finished fade-out during update, remove it.
				if (soundHandle.isDone) {
					dynamicHandles.removeIndex(i)
				}
			}
		}
	}

	/**
	 * Debug-renders all dynamic sound instances.
	 */
	@Suppress("GDXKotlinUnsafeIterator")
	fun debugRender() {
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
		shapeRenderer.projectionMatrix = spriteBatch.projectionMatrix
		shapeRenderer.transformMatrix = spriteBatch.transformMatrix
		for (soundHandle in dynamicHandles) {
			soundHandle.debugRender()
		}
		shapeRenderer.end()
	}

	/**
	 * Stop a sound handle. Optionally allow it to fade out instead of stopping immediately.
	 */
	fun stopSound(soundHandle: MetaSoundHandle?, fadeOut: Boolean = true) {
		if (soundHandle != null && !soundHandle.isDone) {
			if (fadeOut) {
				// Start fading out, not done yet.
				soundHandle.beginFadeOut()
			} else {
				// Immediately stop
				soundHandle.stopImmediately()
				soundHandle.setDone()
			}
		}
	}

	/**
	 * Stops all currently active sounds, optionally fading them out.
	 */
	@Suppress("GDXKotlinUnsafeIterator")
	fun stopAllSounds() {
		stopAllSoundSources()
		for (soundHandles in playingHandles.values()) {
			for (i in soundHandles.size - 1 downTo 0) {
				stopSound(soundHandles[i], false)
			}
			soundHandles.clear()
		}
		// Dynamic handles may have been evicted from playingHandles but still be playing; stop them too.
		// stopSound is idempotent, so handles present in both collections are stopped only once.
		for (i in dynamicHandles.size - 1 downTo 0) {
			stopSound(dynamicHandles[i], false)
		}
		dynamicHandles.clear()
	}

	companion object {
		private val helper = Vector3()
	}
}
