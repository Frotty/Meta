package de.fatox.meta.api.model

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

data class MetaEntityData(
	var pos: Vector3 = Vector3.Zero,
	var transform: Matrix4 = Matrix4(),
)
