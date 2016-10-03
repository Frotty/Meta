package de.fatox.meta.sound;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;

public class MetaSoundDefinition {
    public final String soundName;
    public final long minimumDelay;
    public final int maxInstances;
    public final Array<MetaSoundHandle> handles = new Array<>();
    private Sound sound;
    private boolean looping = false;

    public MetaSoundDefinition(String soundName) {
        this(soundName, 0, 12);
    }

    public MetaSoundDefinition(String soundName, long minimumDelay, int maxInstances) {
        this.soundName = soundName;
        this.minimumDelay = minimumDelay;
        this.maxInstances = maxInstances;
    }

    public Sound getSound() {
        return sound;
    }

    public void setSound(Sound sound) {
        this.sound = sound;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }
}
