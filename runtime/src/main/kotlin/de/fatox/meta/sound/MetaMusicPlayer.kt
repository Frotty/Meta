package de.fatox.meta.sound

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Timer
import com.badlogic.gdx.utils.Timer.Task
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.MetaLoggerFactory
import de.fatox.meta.api.extensions.error
import de.fatox.meta.api.extensions.forEachEntryReentrant
import de.fatox.meta.api.get
import de.fatox.meta.api.model.MetaAudioVideoState
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import org.slf4j.Logger

object UninitializedMusic : Music {
	override fun dispose(): Unit = Unit

	override fun play(): Unit = Unit

	override fun pause(): Unit = Unit

	override fun stop(): Unit = Unit

	override fun isPlaying(): Boolean = false

	override fun setLooping(isLooping: Boolean): Unit = Unit

	override fun isLooping(): Boolean = false

	override fun setVolume(volume: Float): Unit = Unit

	override fun getVolume(): Float = 0f

	override fun setPan(pan: Float, volume: Float): Unit = Unit

	override fun setPosition(position: Float): Unit = Unit

	override fun getPosition(): Float = 0f

	override fun setOnCompletionListener(listener: Music.OnCompletionListener?): Unit = Unit
}

private val log: Logger = MetaLoggerFactory.logger {}
private const val MAX_CONSECUTIVE_FAILURES = 10

class MetaMusicPlayer : Disposable {
	private val assetProvider: AssetProvider by lazyInject()
	private var consecutiveFailureCount = 0

	private val task: Task = object : Task() {
		override fun run() {
			Gdx.app.postRunnable {
				try {
					updateMusic()
					consecutiveFailureCount = 0
				} catch (e: GdxRuntimeException) {
					consecutiveFailureCount++
					val currentKey = musicCache.findKey(currentMusic, true)
					log.error(e) {
						"Failed to update music '$currentKey' " +
							"($consecutiveFailureCount/$MAX_CONSECUTIVE_FAILURES)"
					}
					if (consecutiveFailureCount >= MAX_CONSECUTIVE_FAILURES) {
						cancel()
					}
				}
			}
		}
	}
	private val startVolume = 0.01f
	private var musicEnabled = true
	private var currentMusic: Music = UninitializedMusic
	private var nextMusic: Music = UninitializedMusic
	private val allPool = Array<Music>()
	private val activePool = Array<Music>()
	private val musicCache = ObjectMap<String, Music>()
	private val timer = Timer()
	var random = false

	fun start() {
		if (!task.isScheduled) {
			consecutiveFailureCount = 0
			timer.scheduleTask(task, 0f, 0.1f)
		}
	}

	private fun updateMusic() {
		val audioVideoData = MetaAudioVideoState.state.value
		val volume = audioVideoData.masterVolume * audioVideoData.musicVolume
		if (!musicEnabled || volume <= startVolume) {
			if (currentMusic !== UninitializedMusic)
				currentMusic.volume = 0f
			return
		}
		if (currentMusic === UninitializedMusic || !currentMusic.isPlaying) {
			if (nextMusic === UninitializedMusic) {
				nextFromPool()
			} else {
				startMusic(nextMusic)
			}
		}
		if (currentMusic !== UninitializedMusic) {
			if (currentMusic.volume < startVolume) {
				finishMusic()
			} else {
				fadeInOut(volume)
				if (currentMusic.volume > volume) {
					currentMusic.volume = volume
				}
			}
		}
	}

	private fun finishMusic() {
		currentMusic.stop()
		if (nextMusic !== UninitializedMusic) {
			currentMusic = nextMusic
			nextMusic = UninitializedMusic
			currentMusic.play()
			currentMusic.volume = startVolume
		}
	}

	private fun fadeInOut(volume: Float) {
		if (nextMusic !== UninitializedMusic && currentMusic.isPlaying) {
			currentMusic.volume *= 0.4f
		} else if (currentMusic.volume >= startVolume && currentMusic.volume < volume) {
			currentMusic.volume *= 3f
		}
	}

	fun playMusic(musicPath: String, now: Boolean = false) {
		val music = getMusic(musicPath)
		if (now) {
			currentMusic.stop()
			startMusic(music)
		} else if (currentMusic === UninitializedMusic || !currentMusic.isPlaying) {
			startMusic(music)
		} else {
			nextMusic = music
		}
	}

	private fun startMusic(music: Music) {
		currentMusic = music
		currentMusic.play()
		currentMusic.volume = startVolume
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

	private fun nextFromPool() {
		if (activePool.size == 0 && allPool.size > 0) {
			activePool.addAll(allPool)
		}
		if (random) {
			activePool.shuffle()
		}
		if (activePool.size <= 0) return
		if (currentMusic === UninitializedMusic) {
			startMusic(activePool.pop())
		} else {
			nextMusic = activePool.pop()
		}
	}

	fun clearPools() {
		allPool.clear()
		activePool.clear()
	}

	fun isMusicEnabled(): Boolean {
		return musicEnabled
	}

	fun setMusicEnabled(musicEnabled: Boolean) {
		this.musicEnabled = musicEnabled
		if (!musicEnabled) {
			nextMusic = UninitializedMusic
			currentMusic.stop()
			currentMusic = UninitializedMusic
		}
	}

	private var vol = 1f
	private var focusSilenced = false
	fun silenceMusic(musicEnabled: Boolean) {
		if (currentMusic !== UninitializedMusic) {
			if (musicEnabled) {
				focusSilenced = false
				currentMusic.volume = vol.coerceAtLeast(startVolume)
				start()
			} else {
				if (!focusSilenced && currentMusic.volume > startVolume) vol = currentMusic.volume
				focusSilenced = true
				currentMusic.volume = 0f
				task.cancel()
			}
		} else if (!musicEnabled) {
			focusSilenced = true
			task.cancel()
		}
	}

	val isMusicPlaying: Boolean get() = currentMusic !== UninitializedMusic && currentMusic.isPlaying

	override fun dispose() {
		task.cancel()
		// allPool only ever holds Music instances already present in musicCache, so disposing
		// via musicCache covers both pooled and directly-played (playMusic/getMusic-only) tracks.
		musicCache.forEachEntryReentrant { _, music -> music.dispose() }
		musicCache.clear()
		activePool.clear()
		allPool.clear()
		currentMusic = UninitializedMusic
		nextMusic = UninitializedMusic
	}
}
