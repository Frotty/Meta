#version 300 es
precision mediump float;
in vec3 a_position;

uniform mat4 u_worldTrans;
uniform mat4 u_projTrans;
uniform mat4 u_viewTrans;
uniform mat4 u_invProjTrans;
uniform mat4 u_mvpTrans;
uniform mat4 u_mvpShadowTrans;
uniform mat4 u_mvShadowTrans;
uniform mat3 u_normalTrans;

out vec3 positionVS;
out vec4 v_pos;

void main() {
	gl_Position = u_mvpTrans * vec4(a_position, 1.0);
	v_pos = u_invProjTrans * vec4(a_position, 1.0);
}