package de.fatox.meta.api.dao

import com.badlogic.gdx.math.Vector3

/**
 * Created by Frotty on 15.06.2016.
 */
data class MetaSceneData(var name: String,
                         var compositionPath: String,
                         var cameraPosition: Vector3,
                         var showGrid: Boolean = true) {


}
