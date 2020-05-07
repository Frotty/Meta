package de.fatox.meta.sound;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Timer;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.model.MetaAudioVideoData;
import de.fatox.meta.assets.MetaData;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 09.11.2016.
 */
public class MetaMusicPlayer {
    @Inject
    private MetaData metaData;
    @Inject
    private AssetProvider assetProvider;

    private Timer.Task task;
    private float startVolume = 0.01f;
    private boolean musicEnabled = true;

    private Music currentMusic = null;
    private Music nextMusic = null;

    private Array<Music> allPool = new Array<>();
    private Array<Music> activePool = new Array<>();
    private ObjectMap<String, Music> musicCache = new ObjectMap<>();

    public MetaMusicPlayer() {
        Meta.inject(this);
    }

    public void start() {
        // Start Timer to update music
        task = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                updateMusic();
            }
        }, 0, 0.1f);
    }

    private void updateMusic() {
        MetaAudioVideoData audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData.class);
        float volume = audioVideoData.getMasterVolume() * audioVideoData.getMusicVolume();
        if (!musicEnabled || volume <= startVolume) {
            if (currentMusic != null) {
                currentMusic.setVolume(0);
            }
            return;
        }
        if (currentMusic == null || !currentMusic.isPlaying()) {
            if (nextMusic == null) {
                nextFromPool();
            } else {
                startMusic(nextMusic);
            }
        }
        if (currentMusic != null) {
            if (currentMusic.getVolume() < startVolume) {
                finishMusic();
            } else {
                fadeInOut(volume);
                if (currentMusic.getVolume() > volume) {
                    currentMusic.setVolume(volume);
                }
            }
        }
    }

    private void finishMusic() {
        currentMusic.stop();
        // Check if there is a track queued
        if (nextMusic != null) {
            currentMusic = nextMusic;
            nextMusic = null;
            currentMusic.play();
            currentMusic.setVolume(startVolume);
        }
    }

    private void fadeInOut(float volume) {
        if (nextMusic != null && currentMusic.isPlaying()) {
            currentMusic.setVolume(currentMusic.getVolume() * 0.4f);
        } else if (currentMusic.getVolume() >= startVolume && currentMusic.getVolume() < volume) {
            currentMusic.setVolume(currentMusic.getVolume() * 3f);
        }
    }

    public void playMusic(String musicPath) {
        Music music = getMusic(musicPath);
        if (currentMusic == null || !currentMusic.isPlaying()) {
            startMusic(music);
        } else {
            nextMusic = music;
        }
    }

    private void startMusic(Music music) {
        currentMusic = music;
        currentMusic.play();
        currentMusic.setVolume(startVolume);
    }

    private Music getMusic(String musicPath) {
        if (!musicCache.containsKey(musicPath)) {
            Music music = assetProvider.get(musicPath, Music.class);
            musicCache.put(musicPath, music);
        }
        return musicCache.get(musicPath);
    }

    public void addMusicToPool(String musicName) {
        Music music = getMusic(musicName);
        allPool.add(music);
        allPool.shuffle();
    }

    public void nextFromPool() {
        if (activePool.size == 0 && allPool.size > 0) {
            activePool.addAll(allPool);
            activePool.shuffle();
        }
        if (activePool.size <= 0) return;
        if (currentMusic == null) {
            startMusic(activePool.pop());
        } else {
            nextMusic = activePool.pop();
        }
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setMusicEnabled(boolean musicEnabled) {
        this.musicEnabled = musicEnabled;
        if (!musicEnabled) {
            nextMusic = null;
            currentMusic.stop();
            currentMusic = null;
        }
    }

    private float vol = 1f;

    public void silenceMusic(boolean musicEnabled) {
        if (currentMusic != null) {
            if (musicEnabled) {
                currentMusic.setVolume(vol);
                if (!task.isScheduled()) {
					Timer.schedule(task, 0, 0.1f);
				}
            } else {
                vol = currentMusic.getVolume();
                currentMusic.setVolume(0);
                task.cancel();
            }
        }
    }

	public boolean isMusicPlaying() {
		return currentMusic != null;
	}
}
