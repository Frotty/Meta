package de.fatox.meta.sound;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.injection.Inject;

public class MetaSoundPlayer {
    private ObjectMap<String, MetaSoundDefinition> soundDefinitions = new ObjectMap<>();
    private Array<MetaSoundHandle> dynamicSoundHandles = new Array<>();

    @Inject
    private AssetProvider assetProvider;

    public MetaSoundPlayer() {
        Meta.inject(this);
    }

    public MetaSoundHandle playSound(MetaSoundDefinition soundDefinition) {
        if (soundDefinition.getSound() == null) {
            assetProvider.load(soundDefinition.soundName, Sound.class);
            assetProvider.finish();
            soundDefinition.setSound(assetProvider.get(soundDefinition.soundName, Sound.class));
        }
        long id;
        if (soundDefinition.isLooping()) {
            id = soundDefinition.getSound().loop(1, 1, 0);
        } else {
            id = soundDefinition.getSound().play(1, 1, 0);
        }
        MetaSoundHandle soundHandle = new MetaSoundHandle(soundDefinition, id);
        soundDefinition.handles.add(soundHandle);
        return soundHandle;
    }

    public MetaSoundHandle playSound(MetaSoundDefinition sound, Vector2 listenerPosition, Vector2 soundPosition) {
        MetaSoundHandle metaSoundHandle = playSound(sound);
        metaSoundHandle.setSoundPosition(listenerPosition, soundPosition);
        return metaSoundHandle;
    }

    public MetaSoundHandle playSound(String path) {
        if (!soundDefinitions.containsKey(path)) {
            soundDefinitions.put(path, new MetaSoundDefinition(path));
        }

        return playSound(soundDefinitions.get(path));
    }

    public MetaSoundHandle playSound(String path, Vector2 listenerPosition, Vector2 soundPosition) {
        MetaSoundHandle metaSoundHandle = playSound(path);
        metaSoundHandle.setSoundPosition(listenerPosition, soundPosition);
        return metaSoundHandle;
    }
}
