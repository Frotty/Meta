#version 300 es
precision highp float;

in vec2 v_texCoord0;

uniform sampler2D s_depthTex;
uniform float u_nearDistance;
uniform float u_farDistance;

out vec4 outColor;
//user variables
int samples = 16; //ao sample count

float radius = 1.7; //ao radius
float aoclamp = 0.25; //depth clamp - reduces haloing at screen edges
bool noise = true; //use noise instead of pattern for sample dithering
float noiseamount = 0.00012; //dithering amount

float diffarea = 0.25; //self-shadowing reduction
float gdisplace = 0.3; //gauss bell center

float lumInfluence = 0.29; //how much luminance affects occlusion

float getDepth(vec2 offset)
{
    return texture(s_depthTex, v_texCoord0.xy + offset).x;
}
float ld(float depth) {
	float near = u_nearDistance;
	float far = u_farDistance;
    return (2.0 * near) / (far + near - depth * (far - near));
}

vec2 rand(in vec2 coord) //generating noise/pattern texture for dithering
{
	float noiseX = ((fract(1.0-coord.s*(1300.0f/2.0))*0.25)+(fract(coord.t*(670.0f/2.0))*0.75))*2.0-1.0;
	float noiseY = ((fract(1.0-coord.s*(1300.0f/2.0))*0.75)+(fract(coord.t*(670.0f/2.0))*0.25))*2.0-1.0;

	if (noise)
	{
	    noiseX = clamp(fract(sin(dot(coord ,vec2(12.9898,78.233))) * 43758.5453),0.0,1.0)*2.0-1.0;
	    noiseY = clamp(fract(sin(dot(coord ,vec2(12.9898,78.233)*2.0)) * 43758.5453),0.0,1.0)*2.0-1.0;
	}
	return vec2(noiseX,noiseY)*0.001;
}
float compareDepths(in float depth1, in float depth2,inout int far)
{
	float garea = 2.0; //gauss bell width
	float diff = (depth1 - depth2)*100.0; //depth difference (0-100)
	//reduce left bell width to avoid self-shadowing
	if (diff<gdisplace)
	{
	garea = diffarea;
	}else{
	far = 1;
	}
	float gauss = pow(2.7182,-2.0*(diff-gdisplace)*(diff-gdisplace)/(garea*garea));
	return gauss;
}

float calAO(float depth,float dw, float dh)
{
	float dd = (1.0-depth)*radius;

	float temp = 0.0;
	float temp2 = 0.0;
	float coordw = dw*dd;
	float coordh = dh*dd;
	float coordw2 = dw*dd;
	float coordh2 = dh*dd;

	vec2 coord = vec2(coordw , coordh);
	vec2 coord2 = vec2(coordw2, coordh2);

	int far = 0;
	temp = compareDepths(depth, ld(getDepth(vec2(coord))),far);
	//DEPTH EXTRAPOLATION:
	if (far > 0)
	{
		temp2 = compareDepths(ld(getDepth(vec2(coord2))),depth,far);
		temp += (1.0-temp)*temp2;
	}

	return temp;
}
void main()
{
	float depth = ld(getDepth(vec2(0,0)));
	vec2 size = vec2(1300,670);
	vec2 filterRad = 1.0f / vec2(1300.0f,670.0f);

	vec2 noise = rand(v_texCoord0);
	float d;

	float w = (1.0 / 1300.0f)/clamp(depth,aoclamp,1.0)+(noise.x*(1.0-noise.x));
	float h = (1.0 / 670.0f)/clamp(depth,aoclamp,1.0)+(noise.y*(1.0-noise.y));

	float pw;
	float ph;

	float ao = 0.0f;

	float dl = 2.39996;
	float dz = 1.0/float(samples);
	float l = 0.0;
	float z = 1.0 - dz/2.0;

	for (int i = 0; i <= samples; i ++)
	{
		float r = sqrt(1.0-z);

		pw = cos(l)*r;
		ph = sin(l)*r;
		ao += calAO(depth,pw*w,ph*h);
		z = z - dz;
		l = l + dl;
	}

	ao /= float(samples);
	ao = 1.0-ao;
	outColor = vec4(ao); //ambient occlusion only

}
//void main(void)
//{
//	// Read Information from GBuffer
//	vec3 albedo = texture2D(s_albedoTex, v_texCoord0.st).rgb;
//  	outColor = vec4(albedo, 1.0);
//}