package de.fatox.meta.api.model

import com.badlogic.gdx.scenes.scene2d.ui.Window
import de.fatox.meta.ui.windows.MetaDialog

/**
 * Created by Frotty on 28.06.2016.
 */
data class MetaWindowData (var name: String? = null,
                           var x: Float = 0f,
                           var y: Float = 0f,
                           var width: Float = 0f,
                           var height: Float = 0f,
                           var displayed: Boolean = false,
                           var dialog: Boolean = false) {


    constructor(metaWindow: Window) : this() {
        setFrom(metaWindow)
    }

    fun setFrom(metaWindow: Window) {
        this.x = metaWindow.x
        this.y = metaWindow.y
        this.width = metaWindow.width
        this.height = metaWindow.height
        this.dialog = MetaDialog::class.java.isInstance(metaWindow)
        if (!dialog) {
            displayed = true
        }
    }

    fun set(metaWindow: Window) {
        metaWindow.setPosition(x, y)
        metaWindow.setSize(width, height)
    }
}
