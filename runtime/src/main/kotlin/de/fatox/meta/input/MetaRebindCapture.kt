package de.fatox.meta.input

import com.badlogic.gdx.controllers.Controller

/**
 * One captured control, as a rebinding screen needs to record it.
 *
 * Two shapes rather than one integer, because a keycode and a controller button code are not the same kind of
 * number: `Input.Keys.A` and button 0 are both small integers and mean nothing to each other. A profile that stores
 * them in one field has to remember which device each entry came from, and the moment it forgets, a keyboard binding
 * silently reads as a button.
 *
 * The [Controller] travels with a button capture because button codes are per-device: the same physical face button
 * is a different code on different pads, and `MetaControllerButton.code(controller)` needs the device to resolve a
 * semantic binding back to a raw one.
 */
sealed interface MetaRebindCapture {
    /** A keyboard key, as a `com.badlogic.gdx.Input.Keys` code. */
    class Key(val keycode: Int) : MetaRebindCapture

    /** A controller button, as a raw code on [controller]. */
    class Button(val controller: Controller, val buttonCode: Int) : MetaRebindCapture
}
