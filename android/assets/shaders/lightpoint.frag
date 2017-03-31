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

const float PI = 3.14159265359f;
float smooths(float x) {
	return x * x * ( 3.0f - 2.0f * x);
}
float sqr(float x) {return x*x;}
float GGX(float NdotH, float alphaG) {
    return alphaG*alphaG / (PI * sqr(NdotH*NdotH*(alphaG*alphaG-1.0f) + 1.0f));
}
float smith_GGX(float Ndotv, float alphaG) {
    return 2.0f/(1.0f + sqrt(1.0f + alphaG*alphaG * (1.0f-Ndotv*Ndotv)/(Ndotv*Ndotv)));
}

vec3 EnvBRDFApprox( vec3 SpecularColor, float Roughness, float NoV ) {
	const vec4 c0 = vec4( -1, -0.0275, -0.572, 0.022 );
	const vec4 c1 = vec4( 1, 0.0425, 1.04, -0.04 );
	vec4 r = Roughness * c0 + c1;
	float a004 = min( r.x * r.x, exp2( -9.28 * NoV ) ) * r.x + r.y;
	vec2 AB = vec2( -1.04, 1.04 ) * a004 + r.zw;
	return SpecularColor * AB.x + AB.y;
}

vec3 BRDF(vec3 ul, vec3 L, vec3 V, vec3 N, float roughness, float fresnel) {
    float NdotL = max(dot(N, L),0.0000001f);
    float NdotV = max(dot(N, V),0.0000001f);

    vec3 H = normalize(L+V);
    float NdotH = max(dot(N, H),0.0000001f);
    float VdotH = max(dot(V, H),0.0000001f);
    float D = GGX(NdotH, roughness);

    float G = smith_GGX(NdotL, roughness) * smith_GGX(NdotV, roughness);
    G = NdotL*NdotV/sqr(NdotH);
    float val = G * D;
    vec3 R = 2.0f * NdotV * N - V;
    float RoL = dot(R,L);
    return vec3(EnvBRDFApprox(vec3(fresnel), roughness, NdotV));
}

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

    float fallof = max(0.0, 1.0f / (distFromLight / ((u_lightRadius*2.0f) - distFromLight)));
    float term = 1.0f - pow(smooths(clamp(distFromLight / (u_lightRadius*2.0f), 0.0f, 1.0f)),4.0f);
    float lambert = clamp(dot(normal, lightDir), 0.0, 1.0);
    vec3 spec = BRDF(ul, lightDir, viewDir, normal, 0.51, 0.51);
    float diff = fallof*lambert;
    outColor = vec4(diff*u_lightColor*spec, 1.0);
}