package de.fatox.meta.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import de.fatox.meta.Meta;
import de.fatox.meta.api.model.MetaAudioVideoData;
import de.fatox.meta.api.ui.UIRenderer;
import de.fatox.meta.assets.MetaAssetProvider;
import de.fatox.meta.assets.MetaData;
import de.fatox.meta.injection.Inject;

import java.util.Iterator;

public class MetaSoundPlayer {
    private static final Vector3 helper = new Vector3();
    private ObjectMap<String, MetaSoundDefinition> soundDefinitions = new ObjectMap<>();
    private ObjectMap<MetaSoundDefinition, Array<MetaSoundHandle>> playingHandles = new ObjectMap<>();
    private Array<MetaSoundHandle> dynamicHandles = new Array<>();

    @Inject
    private MetaAssetProvider metaAssetProvider;
    @Inject
    private ShapeRenderer shapeRenderer;
    @Inject
    private SpriteBatch spriteBatch;
    @Inject
    private UIRenderer uiRenderer;
    @Inject
    private MetaData metaData;

    public MetaSoundPlayer() {
        Meta.inject(this);
    }

    public MetaSoundHandle playSound(MetaSoundDefinition soundDefinition, Vector2 listenerPos) {
        if (soundDefinition == null) return null;
        MetaAudioVideoData audioVideoData = metaData.get("audioVideoData", MetaAudioVideoData.class);
        float volume = audioVideoData.getMasterVolume() * audioVideoData.getSoundVolume();
        if (volume <= 0) {
            return null;
        }

        if (!playingHandles.containsKey(soundDefinition)) {
            // Create handlelist if sound is played for the first time
            playingHandles.put(soundDefinition, new Array<>(soundDefinition.maxInstances));
        }

        Array<MetaSoundHandle> handleList = playingHandles.get(soundDefinition);
        cleanupHandles(handleList);
        if (handleList.size >= soundDefinition.maxInstances || (handleList.size > 0 && handleList.first().getStartTime() + 200 >= TimeUtils.millis())) {
            return null;
        }

        if (soundDefinition.getSound() == null) {
            // Load sound if it is played for the first time
            Sound sound = Gdx.audio.newSound(metaAssetProvider.get(soundDefinition.soundName, FileHandle.class));
            soundDefinition.setSound(sound);
        }
        // Play or loop sound
		MetaSoundHandle soundHandle = new MetaSoundHandle(soundDefinition);

        if (listenerPos != null) {
			float mappedVolume = soundHandle.calcVolume(listenerPos, false);
			float mappedPan = soundHandle.calcPan(listenerPos);
			long id = soundDefinition.isLooping()
				? soundDefinition.getSound().loop(mappedVolume, 1, mappedPan)
				: soundDefinition.getSound().play(mappedVolume, 1, mappedPan);
			soundHandle.setHandleId(id);
		} else {
			long id = soundDefinition.isLooping() ? soundDefinition.getSound().loop(volume, 1, 0) : soundDefinition.getSound().play(volume, 1, 0);
			soundHandle.setHandleId(id);
        }

        handleList.add(soundHandle);
        return soundHandle;
    }

    private void cleanupHandles(Array<MetaSoundHandle> handleList) {
        Iterator<MetaSoundHandle> iterator = handleList.iterator();
        while (iterator.hasNext()) {
            MetaSoundHandle next = iterator.next();
            if (next.isDone() || !next.isPlaying()) {
                stopSound(next);
                iterator.remove();
            }
        }
    }

    public MetaSoundHandle playSound(MetaSoundDefinition sound, Vector2 listenerPosition, Vector2 soundPosition) {
        if (isInAudibleRange(sound, listenerPosition, soundPosition)) {
            // Sound is in audible range
            MetaSoundHandle metaSoundHandle = playSound(sound, listenerPosition);
            if (metaSoundHandle != null) {
                // The sound started playing successfully, set position and add to managed handles
                metaSoundHandle.setSoundPosition(listenerPosition, soundPosition);
                dynamicHandles.add(metaSoundHandle);
            }
            return metaSoundHandle;
        }
        return null;
    }

    private boolean isInAudibleRange(MetaSoundDefinition sound, Vector2 listenerPosition, Vector2 soundPosition) {
        return listenerPosition.dst2(soundPosition) <= sound.getSoundRange2() || soundInScreen(soundPosition);
    }

    private boolean soundInScreen(Vector2 soundPosition) {
        helper.set(soundPosition.x, soundPosition.y, 0);
        Vector3 project = uiRenderer.getCamera().project(helper);
        return project.x > 0 && project.x < Gdx.graphics.getWidth() && project.y > 0 && project.y < Gdx.graphics.getHeight();
    }

    public MetaSoundHandle playSound(String path) {
        if (!soundDefinitions.containsKey(path)) {
            soundDefinitions.put(path, new MetaSoundDefinition(path));
        }
        return playSound(soundDefinitions.get(path), null);
    }

    public MetaSoundHandle playSound(String path, Vector2 listenerPosition, Vector2 soundPosition) {
        MetaSoundHandle metaSoundHandle = playSound(path);
        if (metaSoundHandle != null) {
            metaSoundHandle.setSoundPosition(listenerPosition, soundPosition);
        }
        return metaSoundHandle;
    }

    public void updateDynamicSounds(Vector2 listenerPos) {
        Iterator<MetaSoundHandle> iterator = dynamicHandles.iterator();
        while (iterator.hasNext()) {
            MetaSoundHandle soundHandle = iterator.next();
            if (soundHandle.isDone() || !soundHandle.isPlaying()) {
                stopSound(soundHandle);
            } else {
                soundHandle.calcVolAndPan(listenerPos);
            }
        }
    }


    /**
     * Debug-renders all dynamic sound instances
     */
    public void debugRender() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setProjectionMatrix(spriteBatch.getProjectionMatrix());
        shapeRenderer.setTransformMatrix(spriteBatch.getTransformMatrix());
        for (MetaSoundHandle soundHandle : dynamicHandles) {
            soundHandle.debugRender();
        }
        shapeRenderer.end();
    }

    public void stopSound(MetaSoundHandle soundHandle) {
        if (soundHandle != null) {
            soundHandle.stop();
            soundHandle.setDone();
            dynamicHandles.removeValue(soundHandle, true);
        }
    }

    public void stopAllSounds() {
        for (Array<MetaSoundHandle> soundHandles : playingHandles.values()) {
            for (MetaSoundHandle soundHandle : soundHandles) {
                soundHandle.stop();
                soundHandle.setDone();
            }
            soundHandles.clear();
        }
        dynamicHandles.clear();
    }
}
