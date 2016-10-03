package de.fatox.meta.sound;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.injection.Inject;

public class MetaSoundPlayer {
    private Array<MetaSoundHandle> dynamicSoundHandles = new Array<>();

    @Inject
    private AssetProvider assetProvider;

    public MetaSoundPlayer() {
        Meta.inject(this);
    }

    public MetaSoundHandle playSound(MetaSoundDefinition soundDefinition) {
        if(soundDefinition.getSound() == null) {
            assetProvider.load(soundDefinition.soundName, Sound.class);
            assetProvider.finish();
            soundDefinition.setSound(assetProvider.get(soundDefinition.soundName, Sound.class));
        }
        long id;
        if(soundDefinition.isLooping()) {
            id = soundDefinition.getSound().loop(1, 1, 0);
        } else {
            id = soundDefinition.getSound().play(1, 1, 0);
        }
        MetaSoundHandle soundHandle = new MetaSoundHandle(soundDefinition, id);
        soundDefinition.handles.add(soundHandle);
        return soundHandle;
    }

}
