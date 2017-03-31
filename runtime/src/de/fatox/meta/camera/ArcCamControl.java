package de.fatox.meta.camera;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import de.fatox.meta.Meta;
import de.fatox.meta.api.entity.EntityManager;
import de.fatox.meta.entity.Meta3DEntity;
import de.fatox.meta.injection.Inject;

/**
 * Better camera control because I dislike LibGDX's ones
 * Very similar to World Editor.
 *
 * @author Frotty
 */
public class ArcCamControl implements InputProcessor {
    private static Vector3 temp = new Vector3();
    private final Meta3DEntity targetDebug;
    /**
     * The button for moving the target.
     */
    public int moveCameraButton = Buttons.RIGHT;
    public int resetCameraButton = Buttons.MIDDLE;
    /**
     * The units to translate the camera when moved the full width or height of the screen.
     */
    public float translateUnits = 2f; // FIXME auto calculate this based on the target
    /**
     * The key which must be pressed to enter rotation mode.
     */
    public int rotateMode = Keys.CONTROL_LEFT;
    protected boolean rotateModeOn = false;
    protected boolean fastZoomMode = false;
    /**
     * The camera.
     */
    @Inject
    public PerspectiveCamera camera;
    @Inject
    public EntityManager<Meta3DEntity> entityManager;
    @Inject
    public ModelBuilder modelBuilder;
    /**
     * Are we in moveMode?
     */
    private boolean moveModeOn = false;
    /**
     * The target of the arcball
     */
    private Vector3 target = Vector3.Zero;
    /**
     * The planar (X/Y) rotation of the camera
     */
    private float rotationAngle = 0;
    /**
     * The angle in which the camera looks onto the target
     */
    private float angleOfAttack = 56;

    /**
     * Distance from target
     */
    private float distance = 50;

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
        update();
    }

    private int startX, startY;

    public ArcCamControl() {
        Meta.inject(this);

        final Material material = new Material(ColorAttribute.createDiffuse(Color.SLATE));
        final long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.ColorUnpacked;
        Model box = modelBuilder.createSphere(4, 4, 4, 42,42, material, attributes);
        targetDebug = new Meta3DEntity(target, box);
        update();
        entityManager.addEntity(targetDebug);
    }

    public void update() {
        target.lerp(temp, 0.5f);
        targetDebug.setPosition(target);

        camera.position.x = ppX(target.x, distance, rotationAngle, angleOfAttack);
        camera.position.y = ppY(0, distance, angleOfAttack);
        camera.position.z = ppZ(target.z, distance, rotationAngle, angleOfAttack);

        camera.direction.x = target.x - camera.position.x;
        camera.direction.y = target.y - camera.position.y;
        camera.direction.z = target.z - camera.position.z;

        camera.up.x = -sin(rotationAngle) * cos(angleOfAttack);
        camera.up.y = sin(angleOfAttack);
        camera.up.z = -cos(rotationAngle) * cos(angleOfAttack);
        camera.update();
    }

    private static float cos(float aoa) {
        return (float) Math.cos(aoa * DEGTORAD);
    }

    private static float sin(float ang) {
        return (float) Math.sin(ang * DEGTORAD);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == moveCameraButton) {
            startX = screenX;
            startY = screenY;
            moveModeOn = true;
        }
        if (button == resetCameraButton) {
            rotationAngle = 0;
            angleOfAttack = 56;
            distance = 50;
        }
        update();
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == moveCameraButton) {
            startX = screenX;
            startY = screenY;
            moveModeOn = false;
        }
        update();
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        // Touch drag equals clicking a mouseButton and then moving the mouse
        // In case the right mouse button is clicked, we are in MoveMode
        if (moveModeOn) {
            // Calculate the middle of old and new mousePosition
            float deltaX = (screenX - startX);
            float deltaY = (startY - screenY);
            startX = screenX;
            startY = screenY;
            // If CTRL is active, we only rotate
            if (rotateModeOn) {
                angleOfAttack += deltaY * .25;
                rotationAngle += deltaX * .25;
            } else {
                // Otherwise we simple move the target
                if (distance < 150) {
                    deltaX *= distance / 100;
                    deltaY *= distance / 100;
                }
                temp.set(target).add(sin(rotationAngle) * deltaY + cos(rotationAngle) * -deltaX, 0, cos(rotationAngle) * deltaY + sin(rotationAngle) * deltaX);
            }
            update();
        }
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        if (fastZoomMode) {
            return zoom(amount * translateUnits * 10);
        } else {
            return zoom(amount * translateUnits);
        }

    }

    public boolean zoom(float amount) {
        distance += amount;
        update();
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Keys.CONTROL_LEFT) {
            rotateModeOn = true;
        } else if (keycode == Keys.SHIFT_LEFT) {
            fastZoomMode = true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Keys.CONTROL_LEFT) {
            rotateModeOn = false;
        } else if (keycode == Keys.SHIFT_LEFT) {
            fastZoomMode = false;
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    /**
     * Polar Projection from Wurst
     */
    private static float ppX(float x, float dist, float ang, float aoa) {
        return (float) (x + dist * Math.sin(ang * DEGTORAD) * Math.sin(aoa * DEGTORAD));
    }

    private static float ppY(float y, float dist, float ang) {
        return (float) (y + dist * Math.cos(ang * DEGTORAD));
    }

    private static float ppZ(float z, float dist, float ang, float aoa) {
        return (float) (z + dist * Math.cos(ang * DEGTORAD) * Math.sin(aoa * DEGTORAD));
    }

    public static final float DEGTORAD = 0.017453293f;
}
