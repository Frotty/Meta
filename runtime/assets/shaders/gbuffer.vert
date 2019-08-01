#version 300 es
precision mediump float;
// Vertex attributes
in vec3 a_position;
in vec3 a_normal;
in vec4 a_color;
in vec2 a_texCoord0;

// Matrices
uniform mat3 u_normalTrans;
uniform mat4 u_worldTrans;
uniform mat4 u_mvpTrans;

// Output
out vec3 v_normal;
out vec2 v_texCoord0;
out vec4 v_color;
out vec4 v_pos;

void main() {
	v_normal = normalize(u_normalTrans * a_normal);
	v_pos = u_worldTrans * vec4(a_position, 1.0);
	v_texCoord0	= a_texCoord0.st;
	v_color = a_color;
	gl_Position = u_mvpTrans * vec4(a_position, 1.0);
}