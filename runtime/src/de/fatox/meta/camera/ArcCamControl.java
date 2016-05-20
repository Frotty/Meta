package de.fatox.meta.camera;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;

/**
 * Better camera control because I dislike LibGDX's ones
 * Very similar to World Editor.
 * @author Frotty
 *
 */
public class ArcCamControl implements InputProcessor {
	/** The button for moving the target. */
	public int moveCameraButton = Buttons.RIGHT;
	/** The units to translate the camera when moved the full width or height of the screen. */
	public float translateUnits = 2f; // FIXME auto calculate this based on the target
	/** The key which must be pressed to enter rotation mode. */
	public int rotateMode = Keys.CONTROL_LEFT;
	protected boolean rotateModeOn = false;
	protected boolean fastZoomMode = false;
	/** The camera. */
	@Inject
	public PerspectiveCamera camera;
	/** Are we in moveMode? */
	private boolean moveModeOn = false;
	/** The target of the arcball */
	private Vector3 target = Vector3.Zero;
	/** The planar (X/Y) rotation of the camera*/
	private float rotationAngle = -90;
	/** The angle in which the camera looks onto the target */
	private float angleOfAttack = 56;
	/** Distance from target */
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
		update();
	}

	/**
	 * Peq's work
	 */
	public void update () {
		if (angleOfAttack <-180) {
			angleOfAttack += 360;
		} else if (angleOfAttack>180) {
			angleOfAttack -= 360;
		}
		camera.position.x = ppX(target.x, distance, rotationAngle, angleOfAttack);
		camera.position.y = ppY(target.y, distance, rotationAngle, angleOfAttack);
		camera.position.z = ppZ(0, distance, angleOfAttack);

		camera.direction.x = target.x - camera.position.x;
		camera.direction.y = target.y - camera.position.y;
		camera.direction.z = target.z - camera.position.z;
		camera.direction.nor();

		camera.up.x=-cos(rotationAngle)*sin(angleOfAttack);
		camera.up.y=-sin(rotationAngle)*sin(angleOfAttack);
		camera.up.z=cos(angleOfAttack);
		camera.update();
	}

	private static float cos(float aoa) {
		return (float) Math.cos(aoa*DEGTORAD);
	}

	private static float sin(float ang) {
		return (float) Math.sin(ang*DEGTORAD);
	}

	@Override
	public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		if(button == moveCameraButton) {
			startX = screenX;
			startY = screenY;
			moveModeOn = true;
			update();
		}
		return false;
	}

	@Override
	public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		if(button == moveCameraButton) {
			startX = screenX;
			startY = screenY;
			moveModeOn = false;
		}
		return false;
	}

	@Override
	public boolean touchDragged (int screenX, int screenY, int pointer) {
		// Touch drag equals clicking a mouseButton and then moving the mouse
		// In case the right mouse button is clicked, we are in MoveMode
		if(moveModeOn) {
			// Calculate the middle of old and new mousePosition
			float deltaX = (screenX - startX);
			float deltaY = (startY - screenY);
			startX = screenX;
			startY = screenY;
			// If CTRL is active, we only rotate
			if(rotateModeOn) {
		        angleOfAttack+=deltaY*.25;
		        rotationAngle+=deltaX*.25;
			}else{
				// Otherwise we simple move the target
				if(distance < 150) {
					deltaX *= distance/100;
					deltaY *= distance/100;
				}
				target.add(cos(rotationAngle)*deltaY + sin(rotationAngle)*deltaX, sin(rotationAngle)*deltaY + cos(rotationAngle)*-deltaX, 0);
			}
			update();
		}
		return false;
	}

	@Override
	public boolean scrolled (int amount) {
		if(fastZoomMode) {
			return zoom(amount * translateUnits);
		} else {
			return zoom(amount * translateUnits);
		}

	}

	public boolean zoom (float amount) {
		distance+=amount;
		update();
		return true;
	}

	@Override
	public boolean keyDown (int keycode) {
		if(keycode == Keys.CONTROL_LEFT) {
			rotateModeOn = true;
		}else if(keycode == Keys.SHIFT_LEFT) {
			fastZoomMode = true;
		}
		return false;
	}

	@Override
	public boolean keyUp (int keycode) {
		if(keycode == Keys.CONTROL_LEFT) {
			rotateModeOn = false;
		}else if(keycode == Keys.SHIFT_LEFT) {
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

	/** Polar Projection from Wurst */
	private static float ppX( float x, float dist, float ang, float aoa ){
		return (float) (x + dist * Math.cos(ang*DEGTORAD) * Math.cos(aoa*DEGTORAD));
	}

	private static float ppY( float y, float dist, float ang, float aoa  ) {
		return (float) (y + dist * Math.sin(ang*DEGTORAD) * Math.cos(aoa*DEGTORAD));
	}

	private static float ppZ( float z, float dist, float ang ) {
		return (float) (z + dist * Math.sin(ang*DEGTORAD));
	}
	public static final float DEGTORAD  	=  0.017453293f;
}
