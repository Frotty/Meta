#version 300 es
precision mediump float;
// Vertex attributes
in vec4 a_position;
in vec2 a_texCoord0;

// Vertex-Data
out vec2 v_texCoord0;

void main() {
    v_texCoord0 = a_texCoord0.st;
    gl_Position = a_position;
}