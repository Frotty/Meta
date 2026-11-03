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
import de.fatox.meta.api.isLoaded
import de.fatox.meta.api.load
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
					val currentKey = selectedMusicPath ?: musicCache.findKey(currentMusic, true)
					discardSelectedTrack()
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
	private val startGain = 0.01f
	private var trackGain = startGain
	private var focusSilenced = false
	private var musicEnabled = true
	private var currentMusic: Music = UninitializedMusic
	private var nextMusic: Music = UninitializedMusic
	private var selectedMusicPath: String? = null
	private var selectedMusicQueued = false
	private val allPool = Array<String>()
	private val activePool = Array<String>()
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
		if (!musicEnabled || focusSilenced) return
		if (currentMusic === UninitializedMusic || !currentMusic.isPlaying) {
			if (nextMusic === UninitializedMusic) nextFromPool()
			if (nextMusic !== UninitializedMusic) startNextMusic()
			return
		}

		if (nextMusic !== UninitializedMusic) {
			trackGain *= 0.4f
			if (trackGain < startGain) {
				startNextMusic()
				return
			}
		} else {
			trackGain = (trackGain * 3f).coerceAtMost(1f)
		}
		applyMusicGain()
	}

	private fun startNextMusic() {
		currentMusic.stop()
		val music = nextMusic
		nextMusic = UninitializedMusic
		startMusic(music)
	}

	private fun applyMusicGain() {
		val settings = MetaAudioVideoState.state.value
		val volume = if (focusSilenced) 0f else trackGain * settings.masterVolume * settings.musicVolume
		if (currentMusic.volume != volume) currentMusic.volume = volume
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
		trackGain = startGain
		applyMusicGain()
		currentMusic.play()
	}

	private fun getMusic(musicPath: String): Music {
		if (!musicCache.containsKey(musicPath)) {
			musicCache.put(musicPath, assetProvider[musicPath])
		}
		return musicCache.get(musicPath)
	}

	fun addMusicToPool(musicName: String) {
		allPool.add(musicName)
	}

	internal fun nextFromPool() {
		if (activePool.size == 0 && allPool.size > 0) {
			activePool.addAll(allPool)
		}
		if (activePool.size <= 0) return
		val musicPath = selectedMusicPath ?: run {
			if (random) activePool.shuffle()
			activePool.peek().also { selectedMusicPath = it }
		}
		if (!assetProvider.isLoaded<Music>(musicPath)) {
			if (!selectedMusicQueued) {
				assetProvider.load<Music>(musicPath)
				selectedMusicQueued = true
			}
			assetProvider.update(MUSIC_LOAD_BUDGET_MS)
			if (!assetProvider.isLoaded<Music>(musicPath)) return
		}
		val music = getMusic(musicPath)
		activePool.removeValue(musicPath, false)
		selectedMusicPath = null
		selectedMusicQueued = false
		if (currentMusic === UninitializedMusic) {
			startMusic(music)
		} else {
			nextMusic = music
		}
	}

	fun clearPools() {
		allPool.clear()
		activePool.clear()
		selectedMusicPath = null
		selectedMusicQueued = false
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

	fun silenceMusic(musicEnabled: Boolean) {
		val silenced = !musicEnabled
		if (focusSilenced == silenced) return
		focusSilenced = silenced
		if (currentMusic !== UninitializedMusic) applyMusicGain()
		if (silenced) task.cancel() else start()
	}

	internal fun discardSelectedTrack() {
		val musicPath = selectedMusicPath ?: return
		activePool.removeValue(musicPath, false)
		allPool.removeValue(musicPath, false)
		selectedMusicPath = null
		selectedMusicQueued = false
	}

	val isMusicPlaying: Boolean get() = currentMusic !== UninitializedMusic && currentMusic.isPlaying

	override fun dispose() {
		task.cancel()
		// Pooled paths only enter musicCache when selected, so never-played tracks allocate no native resources.
		musicCache.forEachEntryReentrant { _, music -> music.dispose() }
		musicCache.clear()
		activePool.clear()
		allPool.clear()
		selectedMusicPath = null
		selectedMusicQueued = false
		currentMusic = UninitializedMusic
		nextMusic = UninitializedMusic
	}
}

private const val MUSIC_LOAD_BUDGET_MS = 1
