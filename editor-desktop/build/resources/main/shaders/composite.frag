#version 300 es
precision highp float;

in vec2 v_texCoord0;

uniform sampler2D s_albedoTex;
uniform sampler2D s_lightTex;
uniform sampler2D s_depthTex;
uniform sampler2D s_normalTex;

uniform float u_cameraNear;
uniform float u_cameraFar;
uniform mat4 u_projTrans;
uniform mat4 u_invProjTrans;


out vec4 outColor;

// Amount of sample points
const int NUM_SAMPLES = 10;
const int NUM_SPIRAL = 7;

const float u_intensity = 0.125;
const float u_bias = 0.01;
const float u_rad = 0.025;
const float PI2 = 6.2831;

highp float rand(vec2 co) {
    highp float a = 12.9898;
    highp float b = 78.233;
    highp float c = 43758.5453;
    highp float dt= dot(co.xy ,vec2(a,b));
    highp float sn= mod(dt,3.14);
    return fract(sin(sn) * c);
}
vec3 getPosition (vec2 uv) {
    float depth = texture(s_depthTex, uv).r * 2.0 - 1.0;

    vec4 ndc = vec4(uv * 2.0 - 1.0, depth, 1.0);

    vec4 position = (u_invProjTrans * ndc);
    position /= position.w;
    return position.xyz;
}
float ld(float depth) {
	float near = u_cameraNear;
	float far = u_cameraFar;
    return (2.0 * near) / (far + near - depth * (far - near));
}
vec2 TapLocation(int sampleNumber, float spinAngle, out float ssR){
    // Radius relative to ssR
    float alpha = (float(sampleNumber) + 0.5) * (1.0 / float(NUM_SAMPLES));
    float angle = alpha * (float(NUM_SPIRAL) * 6.28) + spinAngle;

    ssR = alpha;
    return vec2(cos(angle), sin(angle));
}

vec3 GetOffsetPosition(vec2 ssC, vec2 unitOffset, float ssR) {
    vec2 ssP = clamp(vec2(ssR*unitOffset) + ssC, 0.0, 1.0);

    return getPosition(ssP);
}

float SampleAO(in vec2 ssC, in vec3 C, in vec3 n_C, in float ssDiskRadius, in int tapIndex, in float randomPatternRotationAngle) {
    // Offset on the unit disk, spun for this pixel
    float ssR;
    vec2 unitOffset = TapLocation(tapIndex, randomPatternRotationAngle, ssR);
    ssR *= ssDiskRadius;

    // The occluding point in camera space
    vec3 Q = GetOffsetPosition(ssC, unitOffset, ssR);

    vec3 v = Q - C;

    float vv = dot(v, v);
    float vn = dot(v, n_C);

    const float epsilon = 0.01;
    float f = max(u_rad - vv, 0.0);
    return f * f * f * max((vn - u_bias) / (epsilon + vv), 0.0);
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
float perspectiveDepthToViewZ(float invClipZ, float near, float far) {
  return ( near * far ) / ( ( far - near ) * invClipZ - far );
}
float getViewZ(float depth ) {
    return perspectiveDepthToViewZ( depth, u_cameraNear, u_cameraFar );
}
vec3 getViewPosition( vec2 screenPosition, float depth, float viewZ ) {
    float clipW = u_projTrans[2][3] * viewZ + u_projTrans[3][3];
    vec4 clipPosition = vec4( ( vec3( screenPosition, depth ) - 0.5 ) * 2.0, 1.0 );
    clipPosition *= clipW;
    return ( u_invProjTrans * clipPosition ).xyz;
}
void main() {
    vec3 normal = ((texture(s_normalTex, v_texCoord0.st).rgb  * 2.0) - 1.0);
    float depth = (texture(s_depthTex, v_texCoord0).r  * 2.0) - 1.0;

    float centerViewZ = ld(depth);
    if (centerViewZ >= (1.0 - 0.01)) {
        discard;
    }
    vec3 position = getPosition(v_texCoord0);
    float randomPatternRotationAngle = 1.0;
    vec2 texInt = vec2(v_texCoord0.xy * vec2(960.0, 600.0));
    texInt = vec2(int(texInt.x), int(texInt.y));
    randomPatternRotationAngle = (3.0 * texInt.x * texInt.y + texInt.x * texInt.y) * 10.0;

    float ssDiskRadius = min(u_rad * centerViewZ, 0.2);

    float sum = 0.0;
    for (int l = 0; l < NUM_SAMPLES; ++l) {
        sum += SampleAO(v_texCoord0, position, normal, (ssDiskRadius), l, randomPatternRotationAngle);
    }

    float temp = u_rad * u_rad * u_rad;
    sum /= temp * temp;
    float A = max(0.0, 1.0 - sum * u_intensity * (5.0 / float(NUM_SAMPLES)));
    vec3 albedo = texture(s_albedoTex, v_texCoord0.xy).rgb;
    vec3 light = texture(s_lightTex, v_texCoord0.xy).rgb;
	outColor = vec4(Tonemap_ACES(vec3(A)),1.);// vec4((albedo * 0.05 + albedo * light), 1.0);
}