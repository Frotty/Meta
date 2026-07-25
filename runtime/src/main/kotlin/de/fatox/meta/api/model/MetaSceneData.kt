package de.fatox.meta.api.model

import com.badlogic.gdx.math.Vector3

data class MetaSceneData(
	var name: String = "Unnamed Scene",
	var compositionPath: String = "",
	var cameraPosition: Vector3 = Vector3.Y,
	var showGrid: Boolean = true,
)
