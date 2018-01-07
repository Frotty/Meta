package de.fatox.meta.api.dao

import com.badlogic.gdx.utils.Array

/**
 * Holds the data of an entire shader composition.
 * A shader composition has multiple Buffers which each have their own shaders.
 * The order and execution of the buffer defines their composition.
 */
data class MetaShaderCompData(var name: String) {
    val renderBuffers: Array<RenderBufferData> = Array()

    constructor() : this("")
}



