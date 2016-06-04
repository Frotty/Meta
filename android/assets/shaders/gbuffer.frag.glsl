#version 300 es
precision mediump float;
// Textures
uniform sampler2D s_diffuseTex;
uniform sampler2D s_normalTex;

// Material Properties
uniform mat3 u_normalTrans;
uniform vec3 u_diffuseColor;
uniform mat4 u_projTrans;
uniform mat4 u_mvTrans;
uniform vec3 u_mat;
uniform vec3 u_camPos;

in vec4 v_pos;
in vec3 v_normal;
in vec3 v_tangent;
in vec3 v_binormal;
in vec2 v_texCoord0;

layout(location = 0) out vec4 o_albedo;
layout(location = 1) out vec4 o_normalsDepth;
layout(location = 2) out vec4 o_aux;

mat3 cotangent_frame( vec3 N, vec3 p, vec2 uv )
{
    // get edge vectors of the pixel triangle
    vec3 dp1 = dFdx( p );
    vec3 dp2 = dFdy( p );
    vec2 duv1 = dFdx( uv );
    vec2 duv2 = dFdy( uv );
 
    // solve the linear system
    vec3 dp2perp = cross( dp2, N );
    vec3 dp1perp = cross( N, dp1 );
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
 
    // construct a scale-invariant frame 
    float invmax = inversesqrt( max( dot(T,T), dot(B,B) ) );
    return mat3( T * invmax, B * invmax, N );
}

vec3 perturb_normal( vec3 N, vec3 V, vec2 texcoord )
{
    // assume N, the interpolated vertex normal and 
    // V, the view vector (vertex to eye)
    vec3 map = texture( s_normalTex, texcoord ).xyz;
    map = map * 255./127. - 128./127.;
   //map.z = sqrt( 1. - dot( map.xy, map.xy ) );
    //map.y = -map.y;
    mat3 TBN = cotangent_frame( N, -V, texcoord );
    return normalize( TBN * map );
}

void main() {
	vec3 albedo = texture(s_diffuseTex, v_texCoord0).rgb * u_diffuseColor;
	// Albedo (color-tinted diffuse)
	o_albedo = vec4(albedo,1.0);//vec4(albedo, 1.0);
	vec3 viewVec = normalize(u_camPos - v_pos.xyz);
	// Normals & Depth
	o_normalsDepth = vec4(perturb_normal(v_normal, viewVec, v_texCoord0),1);
	// Material Properties	
	o_aux = vec4(u_mat, 1.0);
}