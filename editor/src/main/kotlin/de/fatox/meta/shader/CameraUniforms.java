package de.fatox.meta.shader;

/**
 * Created by Frotty on 17.07.2016.
 */
public enum CameraUniforms {
    WORLD_TRANS("mat4", "u_worldTrans"), NORMAL_TRANS("mat3", "u_normalTrans"), MVP_TRANS("mat4", "u_mvpTrans");

    private final String type;
    private final String name;

    CameraUniforms(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
