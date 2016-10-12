package de.fatox.meta.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

public class MetaSoundHandle {
    private static final String TAG = "SoundHandle";
    @Inject
    @Log
    private Logger log;
    private final MetaSoundDefinition definition;
    private final long handleId;
    private Vector2 listenerPos;
    private Vector2 soundPos;

    @Inject
    private UIRenderer uiRenderer;


    public MetaSoundHandle(MetaSoundDefinition definition, long id) {
        this.definition = definition;
        this.handleId = id;
        definition.getSound().setVolume(handleId, 0.25f);
        Meta.inject(this);
    }

    private void calculateVolPitchPan() {
        log.debug(TAG, "updating dynamic sound");
        float audibleRange = (Gdx.graphics.getHeight() * 0.6f);
        float audibleRangeSquared = audibleRange * audibleRange;
        log.debug(TAG, "audible range2: " + audibleRangeSquared);
        float distSquared = listenerPos.dst2(soundPos);
        log.debug(TAG, "dist to listener2: " + distSquared);
        float volumeRemap = 0.15f * MathUtils.clamp(1 - (distSquared / audibleRangeSquared), 0, 1);
        log.debug(TAG, "volume: " + volumeRemap);
        float xPan = soundPos.x - listenerPos.x;
        log.debug(TAG, "xPan" + xPan);
        float remappedXPan = MathUtils.clamp(xPan / audibleRange, -1, 1);
        log.debug(TAG, "remappedXPan" + remappedXPan);
        definition.getSound().setPan(handleId, remappedXPan, volumeRemap);

    }

    public void setSoundPosition(Vector2 listenerPos, Vector2 soundPos) {
        this.listenerPos = listenerPos;
        this.soundPos = soundPos;
        calculateVolPitchPan();
    }
}
