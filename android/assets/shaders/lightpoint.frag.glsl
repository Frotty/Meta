#version 300 es
precision mediump float;

in vec4 v_pos;

uniform sampler2D s_depth;
uniform sampler2D s_normal;
uniform mat4 u_projTrans;
uniform mat4 u_worldTrans;
uniform mat4 u_viewTrans;
uniform mat4 u_projViewTrans;
uniform mat4 u_invProjTrans;
uniform mat4 u_mvpTrans;
uniform mat3 u_normalTrans;
uniform vec3 u_lightPosition;
uniform vec3 u_lightColor;
uniform float u_lightRadius;
uniform vec2 u_inverseScreenSize;
uniform vec3 u_camPos;
uniform float u_nearDistance;
uniform float u_farDistance;

layout(location = 0) out vec4 outColor;

float smooths(float x) {
	return x * x * ( 3.0f - 2.0f * x);
}

void main() {
	vec2 texCoord = gl_FragCoord.xy * u_inverseScreenSize.xy;

    // Sample the depth and convert to linear view space Z (assume it gets sampled as
    // a floating point value of the range [0,1])
    float depth = texture(s_depth, texCoord.st).x  * 2.0 - 1.0;

    vec4 ndc = vec4(texCoord * 2.0 - 1.0, depth, 1.0);

    vec4 position = (u_invProjTrans * ndc);
    position /= position.w;

  	vec3 camPos = ((vec4(u_camPos,1.0f))).rgb;
	vec3 normal = (vec4(texture(s_normal, texCoord.st).rgb, 0.0f)).rgb;

	vec3 ul = u_lightPosition - position.rgb;
	float distFromLight = length(ul);

	float term = 1.0f - pow(smooths(clamp(distFromLight / (u_lightRadius), 0.0f, 1.0f)),4.0f);
	outColor = vec4(term,term,term,1.0);
}