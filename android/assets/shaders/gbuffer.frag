#version 300 es
precision mediump float;

// Textures
uniform sampler2D s_diffuseTex;
uniform sampler2D s_normalTex;
// Material Properties
uniform vec3 u_diffuseColor;

in vec4 v_pos;
in vec4 v_color;
in vec3 v_normal;
in vec3 v_tangent;
in vec3 v_binormal;
in vec2 v_texCoord0;

layout(location = 0) out vec4 o_albedo;
layout(location = 1) out vec3 o_normals;

void main() {
	vec3 albedo = texture(s_diffuseTex, v_texCoord0).rgb * u_diffuseColor * v_color.rgb;
	// Albedo (color-tinted diffuse)
	o_albedo = vec4(albedo,1.0);
	// Normals
	o_normals = (v_normal + 1.0) * 0.5;
}