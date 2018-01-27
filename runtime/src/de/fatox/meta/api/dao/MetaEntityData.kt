package de.fatox.meta.api.dao

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

/**
 * Created by Frotty on 15.06.2016.
 */
data class MetaEntityData(var pos: Vector3 = Vector3.Zero,
                          var transform: Matrix4 = Matrix4())
