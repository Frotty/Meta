package de.fatox.meta.camera

import com.badlogic.gdx.Input
import com.badlogic.gdx.Input.Buttons
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.entity.EntityManager
import de.fatox.meta.entity.Meta3DEntity
import de.fatox.meta.injection.Inject

/**
 * Better camera control because I dislike LibGDX's
 * Very similar to World Editor.
 *
 * @author Frotty
 */
class ArcCamControl : InputProcessor {
    /** The button for moving the target.  */
    var moveCameraButton = Buttons.RIGHT
    var resetCameraButton = Buttons.MIDDLE

    /** The units to translate the camera when moved the full width or height of the screen.  */
    var translateUnits = 0.2f // FIXME auto calculate this based on the target

    /** The key which must be pressed to enter rotation mode.  */
    var rotateMode = Input.Keys.CONTROL_LEFT
    protected var rotateModeOn = false
    protected var fastZoomMode = false

    /** The camera.  */
    @Inject
    var camera: PerspectiveCamera? = null

    @Inject
    var entityManager: EntityManager<Meta3DEntity>? = null

    @Inject
    var modelBuilder: ModelBuilder? = null

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
        update()
    }

    private var startX = 0
    private var startY = 0
    fun update() {
        target.lerp(temp, 0.5f)
        camera!!.position.x = ppX(target.x, distance, rotationAngle, angleOfAttack)
        camera!!.position.y = ppY(0f, distance, angleOfAttack)
        camera!!.position.z = ppZ(target.z, distance, rotationAngle, angleOfAttack)
        camera!!.direction.x = target.x - camera!!.position.x
        camera!!.direction.y = target.y - camera!!.position.y
        camera!!.direction.z = target.z - camera!!.position.z
        camera!!.up.x = -sin(rotationAngle) * cos(angleOfAttack)
        camera!!.up.y = sin(angleOfAttack)
        camera!!.up.z = -cos(rotationAngle) * cos(angleOfAttack)
        camera!!.update()
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
        update()
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == moveCameraButton) {
            startX = screenX
            startY = screenY
            moveModeOn = false
        }
        update()
        return false
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
                angleOfAttack += (deltaY * .25).toFloat()
                rotationAngle += (deltaX * .25).toFloat()
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
            update()
        }
        return false
    }

    fun scrolled(amount: Int): Boolean {
        return if (fastZoomMode) {
            zoom(amount * translateUnits * 10)
        } else {
            zoom(amount * translateUnits)
        }
    }

    fun zoom(amount: Float): Boolean {
        distance += amount
        update()
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.CONTROL_LEFT) {
            rotateModeOn = true
        } else if (keycode == Input.Keys.SHIFT_LEFT) {
            fastZoomMode = true
        } else {
            yes = !yes
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
        @kotlin.jvm.JvmField
        var yes = true
        private fun cos(aoa: Float): Float {
            return Math.cos((aoa * DEGTORAD).toDouble()).toFloat()
        }

        private fun sin(ang: Float): Float {
            return Math.sin((ang * DEGTORAD).toDouble()).toFloat()
        }

        /** Polar Projection from Wurst  */
        private fun ppX(x: Float, dist: Float, ang: Float, aoa: Float): Float {
            return (x + dist * Math.sin((ang * DEGTORAD).toDouble()) * Math.sin((aoa * DEGTORAD).toDouble())).toFloat()
        }

        private fun ppY(y: Float, dist: Float, ang: Float): Float {
            return (y + dist * Math.cos((ang * DEGTORAD).toDouble())).toFloat()
        }

        private fun ppZ(z: Float, dist: Float, ang: Float, aoa: Float): Float {
            return (z + dist * Math.cos((ang * DEGTORAD).toDouble()) * Math.sin((aoa * DEGTORAD).toDouble())).toFloat()
        }

        const val DEGTORAD = 0.017453293f
    }

    init {
        inject(this)
        update()
    }
}