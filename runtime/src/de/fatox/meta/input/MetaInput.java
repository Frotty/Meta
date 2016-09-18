package de.fatox.meta.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import de.fatox.meta.ui.components.MetaClickListener;

public class MetaInput extends InputAdapter {
    private final IntMap<Array<KeyListener>> globalKeyListeners = new IntMap<>();
    private final IntMap<Array<KeyListener>> screenKeyListeners = new IntMap<>();
    private final Array<InputProcessor> screenProcessors = new Array<>();

    public MetaInput() {
        Gdx.input.setInputProcessor(this);
    }

    public void changeScreen() {
        screenKeyListeners.clear();
        screenProcessors.clear();
    }

    public void addAdapterForScreen(InputProcessor adapter) {
        screenProcessors.add(adapter);
    }

    public void registerGlobalKeyListener(int keycode, KeyListener keyListener) {
        registerGlobalKeyListener(keycode, 0, keyListener);
    }

    public void registerGlobalKeyListener(int keycode, long milisRequired, KeyListener keyListener) {
        if (!globalKeyListeners.containsKey(keycode)) {
            globalKeyListeners.put(keycode, new Array<>());
        }
        keyListener.setRequiredLengthMilis(milisRequired);
        globalKeyListeners.get(keycode).add(keyListener);
    }

    public void registerScreenKeyListener(int keycode, KeyListener keyListener) {
        registerScreenKeyListener(keycode, 0, keyListener);
    }

    public void registerScreenKeyListener(int keycode, long milisRequired, KeyListener keyListener) {
        if (!screenKeyListeners.containsKey(keycode)) {
            screenKeyListeners.put(keycode, new Array<>());
        }
        keyListener.setRequiredLengthMilis(milisRequired);
        screenKeyListeners.get(keycode).add(keyListener);
    }

    @Override
    public boolean keyTyped(char character) {
        for (InputProcessor processor : screenProcessors) {
            processor.keyTyped(character);
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (screenKeyListeners.containsKey(keycode)) {
            for (KeyListener listener : screenKeyListeners.get(keycode)) {
                listener.onDown();
            }
        }
        if (globalKeyListeners.containsKey(keycode)) {
            for (KeyListener listener : globalKeyListeners.get(keycode)) {
                listener.onDown();
            }
        }
        for (InputProcessor processor : screenProcessors) {
            processor.keyDown(keycode);
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (screenKeyListeners.containsKey(keycode)) {
            for (KeyListener listener : screenKeyListeners.get(keycode)) {
                listener.onUp();
            }
        }
        if (globalKeyListeners.containsKey(keycode)) {
            for (KeyListener listener : globalKeyListeners.get(keycode)) {
                listener.onUp();
            }
        }
        for (InputProcessor processor : screenProcessors) {
            processor.keyUp(keycode);
        }
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        for (InputProcessor processor : screenProcessors) {
            processor.scrolled(amount);
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        for (InputProcessor processor : screenProcessors) {
            processor.touchDown(screenX, screenY, pointer, button);
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        for (InputProcessor processor : screenProcessors) {
            processor.touchDragged(screenX, screenY, pointer);
        }
        return true;
    }

    public void registerClick(String simpleName, MetaClickListener metaClickListener) {
    }
}
