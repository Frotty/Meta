#ifdef GL_ES
precision mediump float;
#endif

uniform float time;
uniform vec2 resolution;

void main( void ) {
	vec2 p = (2.40*gl_FragCoord.xy-1.45*resolution.xy)/resolution.y *0.618;
    float tau = 3.1415926535*2.0;
    float a = atan(p.x,p.y);
    float r = length(p)*0.75;
    vec2 uv = vec2(a/tau,r);
    float t = time * 0.1 * 0.236;
    //get the color
    float xCol = (uv.x - (t / 3.0)) * 3.0;
    xCol = mod(xCol, 3.0);
    vec3 horColour = vec3(0.25, 0.25, 0.25);

    if (xCol < 1.0) {

        horColour.r += 1.0 - xCol;
        horColour.g += xCol;
    }
    else if (xCol < 2.0) {

        xCol -= 1.0;
        horColour.g += 1.0 - xCol;
        horColour.b += xCol;
    }
    else {

        xCol -= 2.0;
        horColour.b += 1.0 - xCol;
        horColour.r += xCol;
    }

    // draw color beam
    uv = (3.0 * uv) - 1.0;
    float beamWidth = (1.0+0.5*tan(tau*0.15)) * abs(1.0 / (100.0 * uv.y));
    vec3 horBeam = vec3(beamWidth* 1.15) ;
    float t2 = min(time, 1.0);
    gl_FragColor = vec4((( horBeam) * reflect(fract(horColour), horBeam)) * vec3(t2 +0.25,t2 +0.25,t2 +0.25), 1.0);
}