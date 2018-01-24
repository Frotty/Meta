package de.fatox.meta.api.dao

/**
 * Holds the data of a single Buffer inside a shader composition
 */
data class RenderBufferData(var metaShaderPath: String = "") {
    /** The input data type. GEOMETRY refers to all entities, FULLSCREEN to a fullscreen Quad (for post effects) */
    var inType = IN.GEOMETRY
    /** Whether or not this Buffer has a depth buffer */
    var hasDepth = true

    enum class IN {
        GEOMETRY,
        FULLSCREEN
    }

}
