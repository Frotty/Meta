package de.fatox.meta.api.extensions

import com.badlogic.gdx.graphics.glutils.ShaderProgram

inline fun ShaderProgram.bind(init: ShaderProgram.() -> Unit): Unit = bind().apply { init() }