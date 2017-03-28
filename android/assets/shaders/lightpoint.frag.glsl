#version 300 es
precision mediump float;

in vec4 v_pos;

uniform sampler2D s_depth;
uniform sampler2D s_normal;
uniform mat4 u_invProjTrans;
uniform vec3 u_lightPosition;
uniform vec3 u_lightColor;
uniform float u_lightRadius;
uniform vec2 u_inverseScreenSize;
uniform vec3 u_camPos;

layout(location = 0) out vec4 outColor;

void main() {
    vec2 texCoord = gl_FragCoord.xy * u_inverseScreenSize.xy;

    float depth = texture(s_depth, texCoord.st).x * 2.0 - 1.0;

    vec4 ndc = vec4(texCoord * 2.0 - 1.0, depth, 1.0);

    vec4 position = (u_invProjTrans * ndc);
    position /= position.w;

    vec3 camPos = u_camPos.rgb;
    vec3 normal = (texture(s_normal, texCoord.st).rgb  * 2.0) - 1.0;

    vec3 ul = u_lightPosition - position.rgb;
    float distFromLight = length(ul);
    vec3 lightDir = normalize(ul);
    vec3 viewDir = normalize(camPos - position.xyz);

    float fallof = max(0.0, 1.0f / (distFromLight / (u_lightRadius - distFromLight)));
    float lambert = clamp(dot(normal, lightDir), 0.0, 1.0);
    float diff = fallof * lambert;
    outColor = vec4(diff,diff,diff,1.0);
}