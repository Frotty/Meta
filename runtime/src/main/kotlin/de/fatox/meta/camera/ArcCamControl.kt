package de.fatox.meta.camera

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.api.entity.EntityManager
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.sound.dlerp

/**
 * Better camera control because I dislike LibGDX's
 * Very similar to World Editor.
 *
 * @author Frotty
 */
class ArcCamControl : InputProcessor {
	/** The button for moving the target.  */
	var moveCameraButton: Int = Buttons.RIGHT
	var resetCameraButton: Int = Buttons.MIDDLE

	/** The units to translate the camera when moved the full width or height of the screen.  */
	var translateUnits: Float = 0.2f // FIXME auto calculate this based on the target

	/** The key which must be pressed to enter rotation mode.  */
	var rotateMode: Int = Input.Keys.CONTROL_LEFT
	private var rotateModeOn: Boolean = false
	private var fastZoomMode: Boolean = false

	/** The camera.  */
	val camera: PerspectiveCamera by lazyInject()
	val entityManager: EntityManager<Meta3DEntity> by lazyInject()
	val modelBuilder: ModelBuilder by lazyInject()

	/** Are we in moveMode?  */
	private var moveModeOn = false

	/** The target of the arcball  */
	private val target = Vector3.Zero

	/** The planar (X/Y) rotation of the camera  */
	private var rotationAngle = 0f

	/** The angle in which the camera looks onto the target  */
	private var angleOfAttack = 56f

	/** Distance from target  */
	private var distance = 10f
	fun getDistance(): Float {
		return distance
	}

	fun setDistance(distance: Float) {
		this.distance = distance
		update(Gdx.graphics.deltaTime)
	}

	private var startX = 0
	private var startY = 0
	fun update(delta: Float) {
		target.dlerp(temp, 0.5f, delta)
		camera.position.x = ppX(target.x, distance, rotationAngle, angleOfAttack)
		camera.position.y = ppY(0f, distance, angleOfAttack)
		camera.position.z = ppZ(target.z, distance, rotationAngle, angleOfAttack)
		camera.direction.x = target.x - camera.position.x
		camera.direction.y = target.y - camera.position.y
		camera.direction.z = target.z - camera.position.z
		camera.up.x = -sin(rotationAngle) * cos(angleOfAttack)
		camera.up.y = sin(angleOfAttack)
		camera.up.z = -cos(rotationAngle) * cos(angleOfAttack)
		camera.update()
	}

	override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		if (button == moveCameraButton) {
			startX = screenX
			startY = screenY
			moveModeOn = true
		}
		if (button == resetCameraButton) {
			rotationAngle = 0f
			angleOfAttack = 56f
			distance = 10f
		}
		update(Gdx.graphics.deltaTime)
		return false
	}

	override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		if (button == moveCameraButton) {
			startX = screenX
			startY = screenY
			moveModeOn = false
		}
		update(Gdx.graphics.deltaTime)
		return false
	}

	override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
		TODO("Not yet implemented")
	}

	override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
		// Touch drag equals clicking a mouseButton and then moving the mouse
		// In case the right mouse button is clicked, we are in MoveMode
		if (moveModeOn) {
			// Calculate the middle of old and new mousePosition
			var deltaX = (screenX - startX).toFloat()
			var deltaY = (startY - screenY).toFloat()
			startX = screenX
			startY = screenY
			// If CTRL is active, we only rotate
			if (rotateModeOn) {
				angleOfAttack += deltaY * .25.toFloat()
				rotationAngle += deltaX * .25.toFloat()
			} else {
				// Otherwise we simple move the target
				if (distance < 150) {
					deltaX *= distance / 100
					deltaY *= distance / 100
				}
				temp.set(target).add(
					sin(rotationAngle) * deltaY + cos(rotationAngle) * -deltaX,
					0f,
					cos(rotationAngle) * deltaY + sin(rotationAngle) * deltaX
				)
			}
			update(Gdx.graphics.deltaTime)
		}
		return false
	}

	override fun scrolled(amountX: Float, amountY: Float): Boolean {
		return if (fastZoomMode) {
			zoom(amountY * translateUnits * 10)
		} else {
			zoom(amountY * translateUnits)
		}
	}

	fun zoom(amount: Float): Boolean {
		distance += amount
		update(Gdx.graphics.deltaTime)
		return true
	}

	override fun keyDown(keycode: Int): Boolean {
		when (keycode) {
			Input.Keys.CONTROL_LEFT -> rotateModeOn = true
			Input.Keys.SHIFT_LEFT -> fastZoomMode = true
			else -> yes = !yes
		}
		return false
	}

	override fun keyUp(keycode: Int): Boolean {
		if (keycode == Input.Keys.CONTROL_LEFT) {
			rotateModeOn = false
		} else if (keycode == Input.Keys.SHIFT_LEFT) {
			fastZoomMode = false
		}
		return false
	}

	override fun keyTyped(character: Char): Boolean {
		return false
	}

	override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
		return false
	}

	companion object {
		private val temp = Vector3()

		@JvmField
		var yes: Boolean = true
		private fun cos(aoa: Float): Float {
			return kotlin.math.cos(aoa * DEG_TO_RAD.toDouble()).toFloat()
		}

		private fun sin(ang: Float): Float {
			return kotlin.math.sin(ang * DEG_TO_RAD.toDouble()).toFloat()
		}

		/** Polar Projection from Wurst  */
		private fun ppX(x: Float, dist: Float, ang: Float, aoa: Float): Float {
			return (x + dist * kotlin.math.sin(ang * DEG_TO_RAD.toDouble()) * kotlin.math.sin(aoa * DEG_TO_RAD.toDouble())).toFloat()
		}

		private fun ppY(y: Float, dist: Float, ang: Float): Float {
			return (y + dist * kotlin.math.cos(ang * DEG_TO_RAD.toDouble())).toFloat()
		}

		private fun ppZ(z: Float, dist: Float, ang: Float, aoa: Float): Float {
			return (z + dist * kotlin.math.cos(ang * DEG_TO_RAD.toDouble()) * kotlin.math.sin(aoa * DEG_TO_RAD.toDouble())).toFloat()
		}

		private const val DEG_TO_RAD = 0.017453293f
	}

	init {
		update(Gdx.graphics.deltaTime)
	}
}