package de.fatox.meta.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

public class MetaInput extends InputAdapter {
    private final IntMap<Array<KeyListener>> keyListenerMap = new IntMap<>();

    public void registerKeyListener(int keycode, KeyListener keyListener) {
        if (!keyListenerMap.containsKey(keycode)) {
            keyListenerMap.put(keycode, new Array<KeyListener>());
        }
        keyListenerMap.get(keycode).add(keyListener);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keyListenerMap.containsKey(keycode)) {
            for (KeyListener listener : keyListenerMap.get(keycode)) {
                listener.onDown();
            }
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keyListenerMap.containsKey(keycode)) {
            for (KeyListener listener : keyListenerMap.get(keycode)) {
                listener.onUp();
            }
        }
        return false;
    }
}
