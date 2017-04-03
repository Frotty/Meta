#version 300 es
precision highp float;

in vec2 v_texCoord0;

uniform sampler2D s_albedoTex;
uniform sampler2D s_lightTex;

out vec4 outColor;

void main() {
    vec3 albedo = texture(s_albedoTex, v_texCoord0.xy).rgb;
    vec3 light = texture(s_lightTex, v_texCoord0.xy).rgb;
	outColor = vec4(albedo * 0.05 + albedo * light, 1.0);
}