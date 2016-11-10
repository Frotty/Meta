package de.fatox.meta.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Timer;

/**
 * Created by Frotty on 09.11.2016.
 */
public class MetaMusicPlayer {

    public float masterVolume = 1f;
    public float startVolume = 0.01f;
    private boolean musicEnabled = true;

    private Music currentMusic = null;
    private Music nextMusic = null;

    private Array<Music> allPool = new Array<>();
    private Array<Music> activePool = new Array<>();
    private ObjectMap<String, Music> musicCache = new ObjectMap<>();

    private boolean enablePool = true;


    public MetaMusicPlayer() {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (!musicEnabled) return;
                if (currentMusic == null) {
                    if (nextMusic == null) {
                        if (enablePool) {
                            nextFromPool();
                        } else {
                            return;
                        }
                    } else {
                        startMusic(nextMusic);
                    }

                }

                if (currentMusic.getVolume() < startVolume) {
                    currentMusic.stop();
                    currentMusic = nextMusic;
                    nextMusic = null;
                    currentMusic.play();
                    currentMusic.setVolume(startVolume);
                } else {
                    if (nextMusic != null && currentMusic.isPlaying()) {
                        currentMusic.setVolume(currentMusic.getVolume() * 0.4f);
                    } else if (currentMusic.getVolume() >= startVolume && currentMusic.getVolume() < masterVolume) {
                        currentMusic.setVolume(currentMusic.getVolume() * 3f);
                        if (currentMusic.getVolume() > masterVolume) {
                            currentMusic.setVolume(masterVolume);
                        }
                    }
                }

            }
        }, 0, 0.1f);
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
            musicCache.put(musicPath, Gdx.audio.newMusic(Gdx.files.internal(musicPath)));
        }
        return musicCache.get(musicPath);
    }

    public void addMusicToPool(String musicName) {
        Music music = getMusic(musicName);
        allPool.add(music);
    }

    public void nextFromPool() {
        if (activePool.size == 0 && allPool.size > 0) {
            activePool.addAll(allPool);
            activePool.shuffle();
        }
        if(activePool.size <= 0) return;
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
}
