#version 300 es
precision mediump float;
in vec2 v_texCoord0;
in vec4 v_position;

uniform sampler2D s_inputTex;
uniform sampler2D s_albedoTex;
out vec4 fragColor;

void main() {
	vec2 texelSize = 1.0 / vec2(textureSize(s_inputTex, 0));
	int uBlurSize = 7;
	fragColor = vec4(0.0);
	vec2 hlim = vec2(float(-uBlurSize) * 0.5 + 0.5);
	for (int x = 0; x < uBlurSize; ++x) {
		for (int y = 0; y < uBlurSize; ++y) {
			vec2 offset = vec2(float(x), float(y));
			offset += hlim;
			offset *= texelSize;

			fragColor += texture(s_inputTex, v_texCoord0 + offset);
		}
	}

	fragColor = fragColor / float(uBlurSize * uBlurSize);
	fragColor = texture(s_albedoTex, v_texCoord0) * fragColor * 0.125;
}