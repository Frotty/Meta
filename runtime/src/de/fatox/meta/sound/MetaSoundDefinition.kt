package de.fatox.meta.sound;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MetaSoundDefinition {
    public static final long DEFAULT_SOUND_DURATION = 2000;
    public static final float DEFAULT_SOUND_VOLUME = 0.4f;
    public static final float DEFAULT_SOUND_RANGE2 = 600f * 600f;

    public final String soundName;
    public final int maxInstances;
    private Sound sound;
    private boolean looping = false;
    private float soundRange2 = DEFAULT_SOUND_RANGE2;
    private float volume = DEFAULT_SOUND_VOLUME;
    private long duration = DEFAULT_SOUND_DURATION;

    @Inject
    private MetaSoundPlayer soundPlayer;

    public MetaSoundDefinition(String soundName) {
        this(soundName, 4);
    }

    public MetaSoundDefinition(String soundName, int maxInstances) {
        this.soundName = soundName.replaceAll("/", "\\\\");
        this.maxInstances = maxInstances;
        Meta.inject(this);
    }

    public MetaSoundHandle play(Vector2 listenerPosition, Vector2 soundPosition) {
        return soundPlayer.playSound(this, listenerPosition, soundPosition);
    }

    public MetaSoundHandle play() {
        return soundPlayer.playSound(this);
    }

    public Sound getSound() {
        return sound;
    }

    public void setSound(Sound sound) {
        this.sound = sound;
        try {
            Method duration = sound.getClass().getMethod("duration");
            if (duration != null) {
                this.duration = (long) (((float) duration.invoke(sound)) * 1000L);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public float getVolume() {
        return volume;
    }

    public long getDuration() {
        return duration;
    }

    public float getSoundRange2() {
        return soundRange2;
    }

    public void setSoundRange(float soundRange) {
        this.soundRange2 = soundRange * soundRange;
    }
}
