#ifdef GL_ES
#define LOWP lowp
    precision mediump float;
#else
    #define LOWP
#endif
// Vertex attributes
attribute vec4 a_position;
attribute vec2 a_texCoord0;

// Vertex-Data
varying vec2 fragCoord;

void main() {
    fragCoord = a_texCoord0;
	
    gl_Position = a_position;
}