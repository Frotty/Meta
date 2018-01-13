package de.fatox.meta.api.dao

/**
 * Holds the data of on OpenGL shader.
 */
data class GLShaderData(var name: String, var vertexFilePath: String, var fragmentFilePath: String) {
    constructor() : this("", "", "")
}
