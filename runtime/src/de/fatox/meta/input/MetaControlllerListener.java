package de.fatox.meta.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;
import de.fatox.meta.Meta;
import de.fatox.meta.api.Logger;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Log;

public class MetaControlllerListener implements ControllerListener {
    @Inject
    @Log
    private Logger log;
    private MetaInput metaInput;
    private int currentDownKey = -1;

    private float deadzone = 0.375f;

    public MetaControlllerListener(MetaInput metaInput) {
        this.metaInput = metaInput;
        Meta.inject(this);
    }

    @Override
    public void connected(Controller controller) {
        log.debug("MetaControllerListener", "Controller connected");
    }

    @Override
    public void disconnected(Controller controller) {
        log.debug("MetaControllerListener", "Controller disconnected");
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
//        if (value > deadzone) {
//            log.debug("axis", "through deadzone");
//            if (axisCode == 0) {
//                log.debug("axis", "right");
//                if (pressedKeys.contains(Input.Keys.LEFT)) {
//                    pressedKeys.removeValue(Input.Keys.LEFT);
//                    metaInput.keyUp(Input.Keys.LEFT);
//                }
//                if (!pressedKeys.contains(Input.Keys.RIGHT)) {
//                    pressedKeys.add(Input.Keys.RIGHT);
//                    metaInput.keyDown(Input.Keys.RIGHT);
//                }
//            } else if (axisCode == 1) {
//                if (pressedKeys.contains(Input.Keys.DOWN)) {
//                    pressedKeys.removeValue(Input.Keys.DOWN);
//                    metaInput.keyUp(Input.Keys.DOWN);
//                }
//                if (!pressedKeys.contains(Input.Keys.UP)) {
//                    pressedKeys.add(Input.Keys.UP);
//                    metaInput.keyDown(Input.Keys.UP);
//                }
//            }
//        } else if (value < -deadzone) {
//            log.debug("axis", "through deadzone");
//            if (axisCode == 0) {
//                if (pressedKeys.contains(Input.Keys.RIGHT)) {
//                    pressedKeys.removeValue(Input.Keys.RIGHT);
//                    metaInput.keyUp(Input.Keys.RIGHT);
//                }
//                if (!pressedKeys.contains(Input.Keys.LEFT)) {
//                    pressedKeys.add(Input.Keys.LEFT);
//                    metaInput.keyDown(Input.Keys.LEFT);
//                }
//            } else if (axisCode == 1) {
//                if (pressedKeys.contains(Input.Keys.UP)) {
//                    pressedKeys.removeValue(Input.Keys.UP);
//                    metaInput.keyUp(Input.Keys.UP);
//                }
//                if (!pressedKeys.contains(Input.Keys.DOWN)) {
//                    pressedKeys.add(Input.Keys.DOWN);
//                    metaInput.keyDown(Input.Keys.DOWN);
//                }
//            }
//        }
        checkVert(controller);
        checkHor(controller);


        return false;
    }

    private boolean checkVert(Controller controller) {
        if (currentDownKey != Input.Keys.DOWN && (controller.getAxis(1) < -deadzone)) {
            metaInput.keyUp(currentDownKey);
            currentDownKey = Input.Keys.DOWN;
            metaInput.keyDown(currentDownKey);
            return true;
        } else if (currentDownKey == Input.Keys.DOWN && (controller.getAxis(1) > -deadzone)) {
            metaInput.keyUp(currentDownKey);
            currentDownKey = -1;
            return true;
        }

        if (currentDownKey != Input.Keys.UP && (controller.getAxis(1) > deadzone)) {
            metaInput.keyUp(currentDownKey);
            currentDownKey = Input.Keys.UP;
            metaInput.keyDown(currentDownKey);
            return true;
        } else if (currentDownKey == Input.Keys.UP && (controller.getAxis(1) < deadzone)) {
            metaInput.keyUp(currentDownKey);
            currentDownKey = -1;
            return true;
        }
        return false;
    }

    private boolean checkHor(Controller controller) {
        if (currentDownKey != Input.Keys.LEFT && (controller.getAxis(0) < -deadzone)) {
            metaInput.keyUp(currentDownKey);
            currentDownKey = Input.Keys.LEFT;
            metaInput.keyDown(currentDownKey);
            return true;
        } else if (currentDownKey == Input.Keys.LEFT && (controller.getAxis(0) > -deadzone)) {
            metaInput.keyUp(currentDownKey);
            currentDownKey = -1;
            return true;
        }

        if (currentDownKey != Input.Keys.RIGHT && (controller.getAxis(0) > deadzone)) {
            metaInput.keyUp(currentDownKey);
            currentDownKey = Input.Keys.RIGHT;
            metaInput.keyDown(currentDownKey);
            return true;
        } else if (currentDownKey == Input.Keys.RIGHT && (controller.getAxis(0) < deadzone)) {
            metaInput.keyUp(currentDownKey);
            currentDownKey = -1;
            return true;
        }
        return false;
    }

    @Override
    public boolean povMoved(Controller controller, int povCode, PovDirection value) {
        return false;
    }

    @Override
    public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
        return false;
    }
}