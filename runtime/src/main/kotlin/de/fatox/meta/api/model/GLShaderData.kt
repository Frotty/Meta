package de.fatox.meta.api.model

/**
 * Holds the data of on OpenGL shader.
 */
data class GLShaderData(var name: String = "Unnamed GLShader",
                        var vertexFilePath: String = "",
                        var fragmentFilePath: String = "")
