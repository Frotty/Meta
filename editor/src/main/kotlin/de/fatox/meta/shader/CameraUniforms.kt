package de.fatox.meta.shader

/**
 * Created by Frotty on 17.07.2016.
 */
enum class CameraUniforms(private val type: String, private val transName: String) {
    WORLD_TRANS("mat4", "u_worldTrans"),
	NORMAL_TRANS("mat3", "u_normalTrans"),
	MVP_TRANS("mat4", "u_mvpTrans");
}