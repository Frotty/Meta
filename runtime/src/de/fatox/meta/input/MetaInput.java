package de.fatox.meta.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import de.fatox.meta.ui.components.MetaClickListener;

public class MetaInput extends InputAdapter {
    private final IntMap<Array<KeyListener>> keyListenerMap = new IntMap<>();
    private final Array<InputProcessor> screenProcessors = new Array<>();

    public MetaInput() {
        Gdx.input.setInputProcessor(this);
    }

    public IntMap<Array<KeyListener>> getKeyListenerMap() {
        return keyListenerMap;
    }

    public void changeScreen() {
        keyListenerMap.clear();
        screenProcessors.clear();
    }

    public void addAdapterForScreen(InputProcessor adapter) {
        screenProcessors.add(adapter);
    }

    public void registerKeyListener(int keycode, KeyListener keyListener) {
        if (!keyListenerMap.containsKey(keycode)) {
            keyListenerMap.put(keycode, new Array<>());
        }
        keyListenerMap.get(keycode).add(keyListener);
    }

    @Override
    public boolean keyTyped(char character) {
        for(InputProcessor processor : screenProcessors) {
            processor.keyTyped(character);
        }
        return false;
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

    @Override
    public boolean scrolled(int amount) {
        for(InputProcessor processor : screenProcessors) {
            processor.scrolled(amount);
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        for(InputProcessor processor : screenProcessors) {
            processor.touchDown(screenX, screenY, pointer, button);
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        for(InputProcessor processor : screenProcessors) {
            processor.touchDragged(screenX, screenY, pointer);
        }
        return true;
    }

    public void registerClick(String simpleName, MetaClickListener metaClickListener) {
    }
}
