#version 300 es
precision mediump float;

#define PI 3.14159
#define TWO_PI 6.28318
#define PI_OVER_TWO 1.570796
#define ONE_OVER_PI 0.318310

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

float smooths(float x) {
	return x * x * ( 3.0f - 2.0f * x);
}
float sqr(float x) {return x*x;}
float GGX(float NdotH, float alphaG) {
    return alphaG*alphaG / (PI * sqr(NdotH*NdotH*(alphaG*alphaG-1.0f) + 1.0f));
}
float G1V ( float NDotV, float K ) {
	return NDotV / (NDotV*(1.0 - K) + K);
}
float D_GGX( float NDotH, float Alpha ) {
    float AlphaSqr = Alpha*Alpha;
	float OneOverDenominator = 1. / ( (NDotH * NDotH) *(AlphaSqr - 1.0) + 1.0 );
	float Result = AlphaSqr * OneOverDenominator * OneOverDenominator * ONE_OVER_PI;
    return(Result);
}
float G_Schlick( float NDotL, float NDotV, float Alpha ) {
    // Disney remapping Roughness only if we are using area lights
    //float Roughness = (roughness +1.)/2.;
    //float Alpha = Roughness * Roughness;
    float K = Alpha / 2.0;
	float Result = G1V(NDotL, K) * G1V(NDotV, K);
    return(Result);
}
float F_Schlick( float VDotH, float F0 ) {
    float Exponent = ( -5.55473*VDotH -6.98316) * VDotH;
    float Result = F0 + (1.0 - F0) * pow(2., Exponent);
    return(Result);
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
float specular(vec3 L, vec3 V, vec3 N, float roughness, float fresnel) {
    float NdotL = max(dot(N, L),0.0000001f);
    float NdotV = max(dot(N, V),0.0000001f);

    vec3 H = normalize(L+V);
    float NdotH = max(dot(N, H),0.0000001f);
    float VdotH = max(dot(V, H),0.0000001f);
    float D = D_GGX(NdotH, roughness);

    float G = G_Schlick(NdotL, NdotV, roughness);
    float F = F_Schlick(VdotH, fresnel);

    return (D * F * G);
}
vec3 Tonemap_ACES(const vec3 x) {
    // Narkowicz 2015, "ACES Filmic Tone Mapping Curve"
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return (x * (a * x + b)) / (x * (c * x + d) + e);
}
void main() {
    vec2 texCoord = gl_FragCoord.xy * u_inverseScreenSize.xy;

    float depth = texture(s_depth, texCoord.st).x * 2.0 - 1.0;

    vec4 ndc = vec4(texCoord * 2.0 - 1.0, depth, 1.0);

    vec4 position = (u_invProjTrans * ndc);
    position /= position.w;

    vec3 camPos = u_camPos.rgb;
    vec3 normal = normalize((texture(s_normal, texCoord.st).rgb  * 2.0) - 1.0);

    vec3 ul = u_lightPosition - position.rgb;
    float distFromLight = length(ul);
    vec3 lightDir = normalize(ul);
    vec3 viewDir = normalize(camPos - position.xyz);

    float fallof = max(0.0, 1.0f / (distFromLight / ((u_lightRadius) - distFromLight)));
    float term = 1.0f - pow(smooths(clamp(distFromLight / (u_lightRadius*2.0f), 0.0f, 1.0f)),4.0f);
    float lambert = clamp(dot(normal, lightDir), 0.0, 1.0);
    float spec = specular(lightDir, viewDir, normal, 0.55, 0.17);
    float diff = fallof*lambert;
    outColor = vec4(Tonemap_ACES(diff*u_lightColor*spec), 1.0);
}