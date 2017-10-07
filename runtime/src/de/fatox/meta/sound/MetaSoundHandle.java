package de.fatox.meta.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.dao.MetaAudioVideoData;
import de.fatox.meta.api.dao.MetaData;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

public class MetaSoundHandle {
    private static final String TAG = "SoundHandle";
    private final MetaAudioVideoData audioVideoData;
    @Inject
    @Log
    private Logger log;
    @Inject
    private ShapeRenderer shapeRenderer;
    @Inject
    private MetaData metaData;

    private final MetaSoundDefinition definition;
    private final long handleId;
    // For 2d positioning
    private Vector2 listenerPos;
    private Vector2 soundPos;

    private long startTime;
    private boolean done = false;
    private boolean playing = true;


    public MetaSoundHandle(MetaSoundDefinition definition, long id) {
        this.definition = definition;
        this.handleId = id;
        this.startTime = TimeUtils.millis();
        this.definition.getSound().setVolume(handleId, definition.getVolume());
        if (!playing) {
            stop();
        }
        Meta.inject(this);
        audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData.class);
    }

    public void calculateVolPitchPan() {
        float audibleRange = (Gdx.graphics.getHeight() * 0.4f);
        float audibleRangeSquared = audibleRange * audibleRange;
        float distSquared = listenerPos.dst2(soundPos);
        float volumeMod = audioVideoData.masterVolume * audioVideoData.soundVolume;
        float volumeRemap =  volumeMod * definition.getVolume() * MathUtils.clamp(1 - (distSquared / audibleRangeSquared), 0, 1);
        float xPan = soundPos.x - listenerPos.x;
        float remappedXPan = MathUtils.clamp(xPan / (audibleRange), -1, 1) * 0.90f;
        if (distSquared > audibleRangeSquared) {
            setDone();
        } else {
            definition.getSound().setPan(handleId, remappedXPan, volumeRemap);
        }
    }

    public void setSoundPosition(Vector2 listenerPos, Vector2 soundPos) {
        this.listenerPos = listenerPos;
        this.soundPos = soundPos;
        calculateVolPitchPan();
    }

    public void stop() {
        definition.getSound().stop(handleId);
    }

    public void setDone() {
        done = true;
        stop();
    }

    public boolean isDone() {
        return done || (!definition.isLooping() && TimeUtils.timeSinceMillis(startTime) > definition.getDuration());
    }

    public boolean isPlaying() {
        return playing;
    }

    public void debugRender() {
        shapeRenderer.setColor(Color.CHARTREUSE);
        shapeRenderer.circle(soundPos.x + 16, soundPos.y + 16, 14);
    }
}
