package de.fatox.meta.sound

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.Timer.Task
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.get
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject

//object UninitializedMusic : MockMusic()

/**
 * Created by Frotty on 09.11.2016.
 */
class MetaMusicPlayer {
	private val metaData: MetaData by lazyInject()
	private val assetProvider: AssetProvider by lazyInject()

	private var task: Task = object : Task() {
		override fun run() {
			updateMusic()
		}
	}
	private val startVolume = 0.01f
	private var musicEnabled = true
	private var currentMusic: Music? = null
	private var nextMusic: Music? = null
	private val allPool = Array<Music>()
	private val activePool = Array<Music>()
	private val musicCache = ObjectMap<String, Music>()
	private val timer = Timer()

	fun start() {
		// Start Timer to update music
		task = timer.scheduleTask(task, 0f, 0.1f)
	}

	private fun updateMusic() {
		val audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData::class.java)
		val volume = audioVideoData.masterVolume * audioVideoData.musicVolume
		if (!musicEnabled || volume <= startVolume) {
			if (currentMusic !== null)
				currentMusic!!.volume = 0f
			return
		}
		if (currentMusic === null || !currentMusic!!.isPlaying) {
			if (nextMusic === null) {
				nextFromPool()
			} else {
				startMusic(nextMusic!!)
			}
		}
		if (currentMusic !== null) {
			if (currentMusic!!.volume < startVolume) {
				finishMusic()
			} else {
				fadeInOut(volume)
				if (currentMusic!!.volume > volume) {
					currentMusic!!.volume = volume
				}
			}
		}
	}

	private fun finishMusic() {
		currentMusic!!.stop()
		// Check if there is a track queued
		if (nextMusic !== null) {
			currentMusic = nextMusic
			nextMusic = null
			currentMusic!!.play()
			currentMusic!!.volume = startVolume
		}
	}

	private fun fadeInOut(volume: Float) {
		if (nextMusic !== null && currentMusic!!.isPlaying) {
			currentMusic!!.volume *= 0.4f
		} else if (currentMusic!!.volume >= startVolume && currentMusic!!.volume < volume) {
			currentMusic!!.volume *= 3f
		}
	}

	fun playMusic(musicPath: String) {
		val music = getMusic(musicPath)
		if (currentMusic === null || !currentMusic!!.isPlaying) {
			startMusic(music)
		} else {
			nextMusic = music
		}
	}

	private fun startMusic(music: Music) {
		currentMusic = music
		currentMusic!!.play()
		currentMusic!!.volume = startVolume
	}

	private fun getMusic(musicPath: String): Music {
		if (!musicCache.containsKey(musicPath)) {
			musicCache.put(musicPath, assetProvider[musicPath])
		}
		return musicCache.get(musicPath)
	}

	fun addMusicToPool(musicName: String) {
		val music = getMusic(musicName)
		allPool.add(music)
	}

	fun nextFromPool() {
		if (activePool.size == 0 && allPool.size > 0) {
			activePool.addAll(allPool)
			activePool.shuffle()
		}
		if (activePool.size <= 0) return
		if (currentMusic === null) {
			startMusic(activePool.pop())
		} else {
			nextMusic = activePool.pop()
		}
	}

	fun isMusicEnabled(): Boolean {
		return musicEnabled
	}

	fun setMusicEnabled(musicEnabled: Boolean) {
		this.musicEnabled = musicEnabled
		if (!musicEnabled) {
			nextMusic = null
			currentMusic!!.stop()
			currentMusic = null
		}
	}

	private var vol = 1f
	fun silenceMusic(musicEnabled: Boolean) {
		if (currentMusic !== null) {
			if (musicEnabled) {
				currentMusic!!.volume = vol
				if (!task.isScheduled) {
					timer.scheduleTask(task, 0f, 0.1f)
				}
			} else {
				vol = currentMusic!!.volume
				currentMusic!!.volume = 0f
				task.cancel()
			}
		}
	}

	val isMusicPlaying: Boolean get() = currentMusic !== null && currentMusic!!.isPlaying
}