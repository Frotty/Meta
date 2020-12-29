package de.fatox.meta.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import de.fatox.meta.camera.ArcCamControl;
import de.fatox.meta.ui.components.MetaClickListener;

public class MetaInput implements InputProcessor {
    private final IntMap<Array<KeyListener>> globalKeyListeners = new IntMap<>();
    private final IntMap<Array<KeyListener>> screenKeyListeners = new IntMap<>();
    private InputProcessor exclusiveProcessor;
    private final Array<InputProcessor> globalProcessors = new Array<>();
    private final Array<InputProcessor> screenProcessors = new Array<>();

    public MetaInput() {
        Gdx.input.setInputProcessor(this);
        Controllers.addListener(new MetaControllerListener(this));
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
        keyListener.setRequiredLengthMillis(milisRequired);
        globalKeyListeners.get(keycode).add(keyListener);
    }

    public void registerScreenKeyListener(int keycode, KeyListener keyListener) {
        registerScreenKeyListener(keycode, 0, keyListener);
    }

    public void registerScreenKeyListener(int keycode, long milisRequired, KeyListener keyListener) {
        if (!screenKeyListeners.containsKey(keycode)) {
            screenKeyListeners.put(keycode, new Array<>());
        }
        keyListener.setRequiredLengthMillis(milisRequired);
        screenKeyListeners.get(keycode).add(keyListener);
    }

    @Override
    public boolean keyTyped(char character) {
        if (exclusiveProcessor != null) {
            exclusiveProcessor.keyTyped(character);
            return false;
        }
        for (InputProcessor processor : globalProcessors) {
            processor.keyTyped(character);
        }
        for (InputProcessor processor : screenProcessors) {
            processor.keyTyped(character);
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (exclusiveProcessor != null) {
            exclusiveProcessor.keyDown(keycode);
            return false;
        }
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
        for (InputProcessor processor : globalProcessors) {
            processor.keyDown(keycode);
        }
        for (InputProcessor processor : screenProcessors) {
            processor.keyDown(keycode);
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (exclusiveProcessor != null) {
            exclusiveProcessor.keyUp(keycode);
            return false;
        }
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
        for (InputProcessor processor : globalProcessors) {
            processor.keyUp(keycode);
        }
        for (InputProcessor processor : screenProcessors) {
            processor.keyUp(keycode);
        }
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        if (exclusiveProcessor != null) {
            exclusiveProcessor.scrolled(amount);
            return true;
        }
        for (InputProcessor processor : globalProcessors) {
            if (processor.scrolled(amount))
                return true;
        }
        for (InputProcessor processor : screenProcessors) {
            if (processor.scrolled(amount))
                return true;
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (exclusiveProcessor != null) {
            exclusiveProcessor.touchDown(screenX, screenY, pointer, button);
            return true;
        }
        for (InputProcessor processor : globalProcessors) {
            if (processor.touchDown(screenX, screenY, pointer, button))
                return true;
        }
        for (InputProcessor processor : screenProcessors) {
            if (processor.touchDown(screenX, screenY, pointer, button))
                return true;
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (exclusiveProcessor != null) {
            exclusiveProcessor.touchUp(screenX, screenY, pointer, button);
            return false;
        }
        for (InputProcessor processor : globalProcessors) {
            if (processor.touchUp(screenX, screenY, pointer, button))
                return true;
        }
        for (InputProcessor processor : screenProcessors) {
            if (processor.touchUp(screenX, screenY, pointer, button))
                return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (exclusiveProcessor != null) {
            exclusiveProcessor.touchDragged(screenX, screenY, pointer);
            return false;
        }
        for (InputProcessor processor : globalProcessors) {
            if (processor.touchDragged(screenX, screenY, pointer))
                return true;
        }
        for (InputProcessor processor : screenProcessors) {
            if (processor.touchDragged(screenX, screenY, pointer))
                return true;
        }
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (exclusiveProcessor != null) {
            exclusiveProcessor.mouseMoved(screenX, screenY);
            return false;
        }
        for (InputProcessor processor : globalProcessors) {
            if (processor.mouseMoved(screenX, screenY))
                return true;
        }
        for (InputProcessor processor : screenProcessors) {
            if (processor.mouseMoved(screenX, screenY))
                return true;
        }
        return false;
    }

    public void registerClick(String simpleName, MetaClickListener metaClickListener) {
    }

    public void setExclusiveProcessor(InputProcessor exclusiveProcessor) {
        this.exclusiveProcessor = exclusiveProcessor;
    }

    public void addGlobalAdapter(InputProcessor processor) {
        globalProcessors.add(processor);
    }

    public void removeAdapterFromScreen(ArcCamControl camControl) {
        screenProcessors.removeValue(camControl, true);
    }
}
