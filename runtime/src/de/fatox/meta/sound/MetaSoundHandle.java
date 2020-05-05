package de.fatox.meta.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import de.fatox.meta.Meta;
import de.fatox.meta.api.model.MetaAudioVideoData;
import de.fatox.meta.assets.MetaData;
import de.fatox.meta.injection.Inject;

public class MetaSoundHandle {
    @Inject
    private ShapeRenderer shapeRenderer;
    @Inject
    private MetaData metaData;

    private final MetaAudioVideoData audioVideoData;
    private final MetaSoundDefinition definition;
    private long handleId;
    // For 2d positioning
    private Vector2 soundPos;

    private long startTime;

    private boolean done = false;
    private boolean playing = true;

    public MetaSoundHandle(MetaSoundDefinition definition) {
        this.definition = definition;
        this.startTime = TimeUtils.millis();
        if (!playing) {
            stop();
        }
        Meta.inject(this);
        audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData.class);
    }

    public long getStartTime() {
        return startTime;
    }

    public float calcPan(Vector2 listenerPos) {
		float audibleRange = (Gdx.graphics.getHeight() * 0.4f);
		float xPan = soundPos.x - listenerPos.x;
		return MathUtils.clamp(xPan / (audibleRange), -1, 1) * 0.90f;
	}

    public float calcVolume(Vector2 listenerPos, boolean terminate) {
		float audibleRange = (Gdx.graphics.getHeight() * 0.4f);
        float audibleRangeSquared = audibleRange * audibleRange;
        float distSquared = listenerPos.dst2(soundPos);
        float volumeMod = audioVideoData.getMasterVolume() * audioVideoData.getSoundVolume();
		if (terminate && (distSquared > audibleRangeSquared)) {
			setDone();
		}
		return volumeMod * definition.getVolume() * MathUtils.clamp(1 - (distSquared / audibleRangeSquared), 0, 1);
    }

    public void setSoundPosition(Vector2 listenerPos, Vector2 soundPos) {
        this.soundPos = soundPos;
		calcVolAndPan(listenerPos);
	}

	public void calcVolAndPan(Vector2 listenerPos) {
		definition.getSound().setPan(handleId, calcPan(listenerPos), calcVolume(listenerPos, true));
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

	public void setHandleId(long handleId) {
		this.handleId = handleId;
	}
}
