package de.fatox.meta.api.model

/**
 * Holds the data of a single Buffer inside a shader composition
 */
data class RenderBufferData(var metaShaderPath: String = "") {
	/** The input data type. GEOMETRY refers to all entities, FULLSCREEN to a fullscreen Quad (for post effects) */
	var inType: IN = IN.GEOMETRY

	/** Whether or not this Buffer has a depth buffer */
	var hasDepth: Boolean = false

	enum class IN {
		GEOMETRY,
		FULLSCREEN
	}
}
