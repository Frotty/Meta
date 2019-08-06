#version 300 es
precision mediump float;

// Textures
uniform sampler2D s_diffuseTex;
uniform sampler2D s_normalTex;
// Material Properties
uniform vec3 u_diffuseColor;
uniform vec3 u_camPos;

in vec4 v_pos;
in vec4 v_color;
in vec3 v_normal;
in vec2 v_texCoord0;

layout(location = 0) out vec4 o_albedo;
layout(location = 1) out vec3 o_normals;

mat3 cotangentFrame(vec3 N, vec3 p, vec2 uv) {
  // get edge vectors of the pixel triangle
  vec3 dp1 = dFdx(p);
  vec3 dp2 = dFdy(p);
  vec2 duv1 = dFdx(uv);
  vec2 duv2 = dFdy(uv);

  // solve the linear system
  vec3 dp2perp = cross(dp2, N);
  vec3 dp1perp = cross(N, dp1);
  vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
  vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;

  // construct a scale-invariant frame
  float invmax = 1.0 / sqrt(max(dot(T,T), dot(B,B)));
  return mat3(T * invmax, B * invmax, N);
}

vec3 perturb_normal( vec3 N, vec3 V, vec2 texcoord ) {
    vec3 map = texture(s_normalTex, texcoord).xyz;
    map = map * 255./127. - 128./127.;
    mat3 TBN = cotangentFrame(N, -V, texcoord);
    return normalize(TBN * map * 0.75);
}

void main() {
	vec3 albedo = texture(s_diffuseTex, v_texCoord0).rgb * u_diffuseColor * v_color.rgb;
	o_albedo = vec4(albedo,1.0);

	vec3 viewVec = normalize(u_camPos - v_pos.xyz);
	o_normals = (perturb_normal(normalize(v_normal), viewVec, v_texCoord0)  + 1.0) * 0.5;
}