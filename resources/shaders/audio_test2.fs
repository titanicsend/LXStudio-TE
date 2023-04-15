
// TE Audio Test Shader
//
// NOTE: If we flip y axis direction in the shader framework, as we should,
// remember to take the "uv.y = 1-uv.y" out of this shader

#define TEXTURE_SIZE 512.0
#define CHANNEL_COUNT 16.0
#define pixPerBin (TEXTURE_SIZE / CHANNEL_COUNT)
#define halfBin (pixPerBin / 2.0)

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

//  rotate a point around the origin by <angle> radians
vec2 rotate(vec2 point, float angle) {
  mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
  return rotationMatrix * point;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    // normalize coordinates
	vec2 uv = fragCoord.xy / iResolution.xy;

    // translate to roughly vehicle origin, then scale and rotate according
    // to common control settings.
    uv -= vec2(0.5,0.5);
    uv = rotate(uv,iRotationAngle);
    uv += vec2(0.5,0.5);
    uv *= 1.0/iScale;


    // sound texture size is 512x2
    // we're going to quantize that into a 32 channel EQ

    float index = mod(uv.x * 512.0,512.0);
    float p = floor(index / pixPerBin);
    float tx = halfBin+pixPerBin * p;
    float dist = abs(halfBin - mod(index,pixPerBin)) / halfBin;

    // since we're using dist to calculate desatuation for a specular
    // reflection effect, we'll modulate it with beat, to change
    // apparent shininess.
    dist = (dist * dist * dist) - iWow1 * beat;

	// first row is frequency data
	float freq  = texelFetch( iChannel0, ivec2(tx,0), 0 ).x;

	// convert frequency data to shifting colors, just because we can!
	vec3 hsvLayer1 = vec3(0.5 + 0.5 * sin(iTime+p*0.618),dist,iWow2);
    vec3 hsvLayer2 =
    vec3(hsvLayer1.x, max(hsvLayer1.y,1.0-dist)- 0.1,hsvLayer1.z + (1.0-dist));

	vec3 col = hsv2rgb(hsvLayer2);

	// make a simple spectrum analyzer display
	col *= ((freq > uv.y) ? 1.0 : 0.);


    // second row is normalized sound waveform data
	// just draw this over the spectrum analyzer.  Sound data
	// coming from LX is in the range -1 to 1, so we scale it and move it down
	// a bit so we can see it better.
    float wave = (0.5 * (1.0+texelFetch( iChannel0, ivec2(index,1), 0 ).x)) - 0.25;
	col += (1.0-smoothstep( 0.0, 0.04, abs(wave - uv.y)));

	// return pixel color
	fragColor = vec4(col,max(col.r,max(col.g,col.b)));
}