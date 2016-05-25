#version 300 es
precision mediump float;
// Constants

in vec2 v_texCoord0;

out vec4 outColor;

uniform sampler2D s_albedoTex; 		// Albedo (diffuse without lighting)

void main(void)
{
	// Read Information from GBuffer
	vec3 albedo = texture2D(s_albedoTex, v_texCoord0.st).rgb;
  	outColor = vec4(albedo, 1.0);
}